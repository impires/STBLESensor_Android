package com.st.core.multinode

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Singleton
class MultiNodeRepositoryImpl @Inject constructor(
    private val sessionManager: NodeSessionManager
) : MultiNodeRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _discoveredNodes = MutableStateFlow<List<ManagedNode>>(emptyList())

    private val managedNodesState: StateFlow<List<ManagedNode>> =
        combine(_discoveredNodes, sessionManager.states) { discovered, sessions ->
            discovered.map { node ->
                node.withSession(sessions[node.id])
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override fun managedNodes(): StateFlow<List<ManagedNode>> = managedNodesState

    override fun updateDiscoveredNodes(nodes: List<ManagedNode>) {
        val previous = _discoveredNodes.value
        val currentSessions = sessionManager.states.value

        val merged = LinkedHashMap<String, ManagedNode>()

        nodes.forEach { newNode ->
            val oldNode = previous.firstOrNull { sameNode(it, newNode) }

            merged[keyOf(newNode)] = newNode.copy(
                isSelected = oldNode?.isSelected ?: false,
                isLogging = oldNode?.isLogging ?: false,
                error = oldNode?.error
            )
        }

        previous.forEach { oldNode ->
            val session = currentSessions[oldNode.id]
            val keepNode =
                oldNode.isSelected ||
                        oldNode.isLogging ||
                        session?.phase in activePhases

            if (keepNode) {
                merged.putIfAbsent(keyOf(oldNode), oldNode)
            }
        }

        _discoveredNodes.value = merged.values.toList()
    }

    override fun upsertDiscoveredNode(node: ManagedNode) {
        val previous = _discoveredNodes.value.toMutableList()
        val index = previous.indexOfFirst { sameNode(it, node) }

        if (index >= 0) {
            val oldNode = previous[index]
            previous[index] = node.copy(
                isSelected = oldNode.isSelected,
                isLogging = oldNode.isLogging,
                error = oldNode.error
            )
        } else {
            previous.add(node)
        }

        _discoveredNodes.value = previous
    }

    override fun toggleSelection(nodeId: String, maxSelected: Int) {
        val current = _discoveredNodes.value
        val selectedCount = current.count { it.isSelected }
        val target = current.firstOrNull { it.id == nodeId } ?: return

        if (!target.isSelected && selectedCount >= maxSelected) return

        _discoveredNodes.update { nodes ->
            nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(isSelected = !node.isSelected)
                } else {
                    node
                }
            }
        }
    }

    override fun clearSelection() {
        _discoveredNodes.update { nodes ->
            nodes.map { it.copy(isSelected = false) }
        }
    }

    override fun markLogging(nodeId: String, isLogging: Boolean) {
        if (isLogging) {
            sessionManager.markLogging(nodeId, true)
        }

        _discoveredNodes.update { nodes ->
            nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(
                        isLogging = isLogging,
                        error = null
                    )
                } else {
                    node
                }
            }
        }
    }

    override fun markError(nodeId: String, message: String?) {
        _discoveredNodes.update { nodes ->
            nodes.map { node ->
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
        return sessionManager.connectAndAwaitReady(
            nodeId = nodeId,
            maxConnectionRetries = maxConnectionRetries,
            maxPayloadSize = maxPayloadSize,
            enableServer = enableServer
        )
    }

    override suspend fun disconnect(nodeId: String): Result<Unit> {
        val result = sessionManager.disconnect(nodeId)

        if (result.isSuccess) {
            _discoveredNodes.update { nodes ->
                nodes.map { node ->
                    if (node.id == nodeId) {
                        node.copy(
                            isLogging = false,
                            error = null
                        )
                    } else {
                        node
                    }
                }
            }
        }

        return result
    }

    private fun ManagedNode.withSession(session: NodeSessionState?): ManagedNode {
        if (session == null) return this

        return copy(
            isConnected = session.phase in setOf(
                NodeSessionPhase.Connected,
                NodeSessionPhase.Ready,
                NodeSessionPhase.Logging
            ),
            isReady = session.phase in setOf(
                NodeSessionPhase.Ready,
                NodeSessionPhase.Logging
            ),
            isLogging = when (session.phase) {
                NodeSessionPhase.Logging -> true
                NodeSessionPhase.Disconnected,
                NodeSessionPhase.Disconnecting,
                NodeSessionPhase.Error -> false
                else -> isLogging
            },
            error = when {
                session.error != null -> session.error
                session.phase in setOf(
                    NodeSessionPhase.Connected,
                    NodeSessionPhase.Ready,
                    NodeSessionPhase.Logging
                ) -> null
                else -> error
            }
        )
    }

    private fun sameNode(left: ManagedNode, right: ManagedNode): Boolean {
        return left.id == right.id || left.mac == right.mac
    }

    private fun keyOf(node: ManagedNode): String {
        return if (node.id.isNotBlank()) node.id else node.mac
    }

    private companion object {
        val activePhases = setOf(
            NodeSessionPhase.Connecting,
            NodeSessionPhase.Connected,
            NodeSessionPhase.Ready,
            NodeSessionPhase.Logging,
            NodeSessionPhase.Disconnecting
        )
    }
}