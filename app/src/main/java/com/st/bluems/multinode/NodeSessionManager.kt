package com.st.bluems.multinode

import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.models.NodeState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.Result.Companion.failure

@Singleton
class NodeSessionManager @Inject constructor(
    private val blueManager: BlueManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val connectJobs = ConcurrentHashMap<String, Job>()

    private val _states = MutableStateFlow<Map<String, NodeSessionState>>(emptyMap())
    val states: StateFlow<Map<String, NodeSessionState>> = _states.asStateFlow()

    suspend fun connectAndAwaitReady(
        nodeId: String,
        maxConnectionRetries: Int = 3,
        maxPayloadSize: Int = 248,
        enableServer: Boolean = false,
        readyTimeoutMillis: Long = 15_000L
    ): Result<Unit> {
        val current = _states.value[nodeId]
        if (current?.phase == NodeSessionPhase.Ready || current?.phase == NodeSessionPhase.Logging) {
            return Result.success(Unit)
        }

        startConnectJob(
            nodeId = nodeId,
            maxConnectionRetries = maxConnectionRetries,
            maxPayloadSize = maxPayloadSize,
            enableServer = enableServer
        )

        val finalState = withTimeoutOrNull(readyTimeoutMillis) {
            states
                .map { it[nodeId] }
                .filterNotNull()
                .first {
                    it.phase == NodeSessionPhase.Ready ||
                            it.phase == NodeSessionPhase.Logging ||
                            it.phase == NodeSessionPhase.Error ||
                            it.phase == NodeSessionPhase.Disconnected
                }
        } ?: run {
            updateState(nodeId) {
                it.copy(
                    phase = NodeSessionPhase.Error,
                    error = "Timeout waiting for node $nodeId to become ready"
                )
            }
            return failure(
                IllegalStateException("Timeout waiting for node $nodeId to become ready")
            )
        }

        return when (finalState.phase) {
            NodeSessionPhase.Ready,
            NodeSessionPhase.Logging -> Result.success(Unit)

            else -> failure(
                IllegalStateException(finalState.error ?: "Node $nodeId is not ready")
            )
        }
    }

    suspend fun disconnect(nodeId: String): Result<Unit> {
        return try {
            updateState(nodeId) {
                it.copy(
                    phase = NodeSessionPhase.Disconnecting,
                    error = null
                )
            }

            connectJobs.remove(nodeId)?.cancelAndJoin()
            blueManager.disconnect(nodeId = nodeId)

            updateState(nodeId) {
                it.copy(
                    phase = NodeSessionPhase.Disconnected,
                    error = null
                )
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            updateState(nodeId) {
                it.copy(
                    phase = NodeSessionPhase.Error,
                    error = t.message ?: "Disconnect failed"
                )
            }
            failure(t)
        }
    }

    fun markLogging(nodeId: String, isLogging: Boolean) {
        updateState(nodeId) {
            it.copy(
                phase = if (isLogging) NodeSessionPhase.Logging else NodeSessionPhase.Ready,
                error = null
            )
        }
    }

    fun clear(nodeId: String) {
        connectJobs.remove(nodeId)?.cancel()
        _states.update { current -> current - nodeId }
    }

    private fun startConnectJob(
        nodeId: String,
        maxConnectionRetries: Int,
        maxPayloadSize: Int,
        enableServer: Boolean
    ) {
        val existingJob = connectJobs[nodeId]
        if (existingJob?.isActive == true) return

        val job = scope.launch {
            var retryCount = 0

            updateState(nodeId) {
                it.copy(
                    phase = NodeSessionPhase.Connecting,
                    retryCount = 0,
                    error = null
                )
            }

            while (isActive) {
                try {
                    blueManager.connectToNode(
                        nodeId = nodeId,
                        maxPayloadSize = maxPayloadSize,
                        enableServer = enableServer
                    ).collect { node ->

                        val previousNodeState = node.connectionStatus.prev
                        val currentNodeState = node.connectionStatus.current

                        when (currentNodeState) {
                            NodeState.Connecting -> {
                                updateState(nodeId) {
                                    it.copy(
                                        phase = NodeSessionPhase.Connecting,
                                        retryCount = retryCount,
                                        error = null
                                    )
                                }
                            }

                            NodeState.Ready -> {
                                updateState(nodeId) {
                                    it.copy(
                                        phase = NodeSessionPhase.Ready,
                                        retryCount = retryCount,
                                        error = null
                                    )
                                }
                            }

                            NodeState.Disconnected -> {
                                val shouldRetry =
                                    retryCount < maxConnectionRetries &&
                                            (previousNodeState == NodeState.Connecting ||
                                                    previousNodeState == NodeState.Ready)

                                if (shouldRetry) {
                                    retryCount += 1

                                    updateState(nodeId) {
                                        it.copy(
                                            phase = NodeSessionPhase.Connecting,
                                            retryCount = retryCount,
                                            error = "Retrying $retryCount/$maxConnectionRetries"
                                        )
                                    }

                                    throw RetryConnectException()
                                } else {
                                    updateState(nodeId) {
                                        it.copy(
                                            phase = NodeSessionPhase.Disconnected,
                                            retryCount = retryCount,
                                            error = if (previousNodeState == NodeState.Ready) {
                                                "Connection lost"
                                            } else {
                                                "Disconnected"
                                            }
                                        )
                                    }

                                    throw StopConnectJobException()
                                }
                            }

                            else -> {
                                updateState(nodeId) {
                                    it.copy(
                                        phase = NodeSessionPhase.Connected,
                                        retryCount = retryCount,
                                        error = null
                                    )
                                }
                            }
                        }
                    }

                    return@launch
                } catch (stop: StopConnectJobException) {
                    return@launch
                } catch (retry: RetryConnectException) {
                    delay(1_000L)
                } catch (t: Throwable) {
                    updateState(nodeId) {
                        it.copy(
                            phase = NodeSessionPhase.Error,
                            retryCount = retryCount,
                            error = t.message ?: "Connection failed"
                        )
                    }
                    return@launch
                }
            }
        }

        job.invokeOnCompletion {
            connectJobs.remove(nodeId)
        }

        connectJobs[nodeId] = job
    }

    private fun updateState(
        nodeId: String,
        transform: (NodeSessionState) -> NodeSessionState
    ) {
        _states.update { current ->
            val existing = current[nodeId] ?: NodeSessionState(nodeId = nodeId)
            current + (nodeId to transform(existing))
        }
    }

    private class RetryConnectException : RuntimeException()

    private class StopConnectJobException : RuntimeException()
}