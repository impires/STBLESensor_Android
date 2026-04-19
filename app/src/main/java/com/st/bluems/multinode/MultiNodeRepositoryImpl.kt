package com.st.bluems.multinode

import com.st.blue_sdk.BlueManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MultiNodeRepositoryImpl @Inject constructor(
    private val blueManager: BlueManager
) : MultiNodeRepository {

    private val _discoveredNodes = MutableStateFlow<List<ManagedNode>>(emptyList())

    override fun discoveredNodes(): Flow<List<ManagedNode>> = _discoveredNodes.asStateFlow()

    override fun updateDiscoveredNodes(nodes: List<ManagedNode>) {
        _discoveredNodes.value = nodes
    }

    override suspend fun disconnect(nodeId: String): Result<Unit> {
        return try {
            blueManager.disconnect(nodeId = nodeId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun markReady(nodeId: String): Result<Unit> {
        return try {
            blueManager.getNodeWithFirmwareInfo(nodeId = nodeId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}