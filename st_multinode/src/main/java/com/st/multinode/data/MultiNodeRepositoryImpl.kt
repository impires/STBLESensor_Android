package com.st.multinode.data

import com.st.multinode.ManagedNode
import com.st.multinode.NodeSessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class MultiNodeRepositoryImpl @Inject constructor(
    private val nodeSessionManager: NodeSessionManager
) : MultiNodeRepository {

    private val _managedNodes = MutableStateFlow<List<ManagedNode>>(emptyList())

    override fun managedNodes(): StateFlow<List<ManagedNode>> = _managedNodes.asStateFlow()

    override fun updateDiscoveredNodes(nodes: List<ManagedNode>) {
        _managedNodes.update { current ->
            nodes.map { incoming ->
                val existing = current.firstOrNull { it.id == incoming.id }

                if (existing != null) {
                    existing.copy(
                        name = incoming.name,
                        mac = incoming.mac
                    )
                } else {
                    incoming
                }
            }
        }
    }

    override fun toggleSelection(nodeId: String, maxSelected: Int) {
        _managedNodes.update { current ->
            val selectedCount = current.count { it.isSelected }

            current.map { node ->
                if (node.id != nodeId) {
                    node
                } else {
                    val canSelect = node.isSelected || selectedCount < maxSelected
                    if (canSelect) {
                        node.copy(isSelected = !node.isSelected)
                    } else {
                        node
                    }
                }
            }
        }
    }

    override fun clearSelection() {
        _managedNodes.update { current ->
            current.map { it.copy(isSelected = false) }
        }
    }

    override fun markLogging(nodeId: String, isLogging: Boolean) {
        _managedNodes.update { current ->
            current.map { node ->
                if (node.id == nodeId) {
                    node.copy(
                        isLogging = isLogging,
                        isReady = !isLogging,
                        error = null
                    )
                } else {
                    node
                }
            }
        }

        nodeSessionManager.markLogging(nodeId, isLogging)
    }

    override fun markError(nodeId: String, message: String?) {
        _managedNodes.update { current ->
            current.map { node ->
                if (node.id == nodeId) {
                    node.copy(error = message)
                } else {
                    node
                }
            }
        }
    }

    override suspend fun connectAndAwaitReady(
        nodeId: String,
        maxConnectionRetries: Int,
        maxPayloadSize: Int,
        enableServer: Boolean
    ): Result<Unit> {
        val result = nodeSessionManager.connectAndAwaitReady(
            nodeId = nodeId,
            maxConnectionRetries = maxConnectionRetries,
            maxPayloadSize = maxPayloadSize,
            enableServer = enableServer
        )

        _managedNodes.update { current ->
            current.map { node ->
                if (node.id == nodeId) {
                    if (result.isSuccess) {
                        node.copy(
                            isConnected = true,
                            isReady = true,
                            error = null
                        )
                    } else {
                        node.copy(
                            isConnected = false,
                            isReady = false,
                            error = result.exceptionOrNull()?.message
                        )
                    }
                } else {
                    node
                }
            }
        }

        return result
    }

    override suspend fun disconnect(nodeId: String): Result<Unit> {
        val result = nodeSessionManager.disconnect(nodeId)

        _managedNodes.update { current ->
            current.map { node ->
                if (node.id == nodeId) {
                    node.copy(
                        isConnected = false,
                        isReady = false,
                        isLogging = false,
                        error = if (result.isFailure) {
                            result.exceptionOrNull()?.message
                        } else {
                            null
                        }
                    )
                } else {
                    node
                }
            }
        }

        return result
    }
}