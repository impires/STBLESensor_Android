package com.st.bluems.multinode

import kotlinx.coroutines.flow.Flow

interface MultiNodeRepository {
    fun discoveredNodes(): Flow<List<ManagedNode>>
    suspend fun disconnect(nodeId: String): Result<Unit>
    suspend fun markReady(nodeId: String): Result<Unit>
    fun updateDiscoveredNodes(nodes: List<ManagedNode>)
}