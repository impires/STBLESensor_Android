package com.st.bluems.multinode

import kotlinx.coroutines.flow.StateFlow

interface MultiNodeRepository {

    fun managedNodes(): StateFlow<List<ManagedNode>>

    fun updateDiscoveredNodes(nodes: List<ManagedNode>)

    fun upsertDiscoveredNode(node: ManagedNode)

    fun toggleSelection(nodeId: String, maxSelected: Int = 4)

    fun clearSelection()

    fun markLogging(nodeId: String, isLogging: Boolean)

    fun markError(nodeId: String, message: String?)

    suspend fun connectAndAwaitReady(
        nodeId: String,
        maxConnectionRetries: Int = 3,
        maxPayloadSize: Int = 248,
        enableServer: Boolean = false
    ): Result<Unit>

    suspend fun disconnect(nodeId: String): Result<Unit>
}