package com.st.multinode.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SdFlowStarter @Inject constructor(
    private val nodeLogEngineFactory: NodeLogEngineFactory
) {

    suspend fun prepareFlow(nodeId: String, flowJson: String): Result<Unit> = runCatching {
        nodeLogEngineFactory.get(nodeId).prepareFlow(nodeId, flowJson)
    }

    suspend fun startFlow(nodeId: String, flowJson: String): Result<Unit> = runCatching {
        nodeLogEngineFactory.get(nodeId).startFlow(nodeId, flowJson)
    }

    suspend fun stopFlow(nodeId: String): Result<Unit> = runCatching {
        nodeLogEngineFactory.get(nodeId).stopFlow(nodeId)
    }

    suspend fun requestStatus(nodeId: String): Result<Unit> = runCatching {
        nodeLogEngineFactory.get(nodeId).requestStatus(nodeId)
    }

    suspend fun release(nodeId: String) {
        nodeLogEngineFactory.get(nodeId).release(nodeId)
    }

    fun engineFor(nodeId: String): NodeLogEngine {
        return nodeLogEngineFactory.get(nodeId)
    }

    fun officialEngineFor(nodeId: String): OfficialSdLogEngine? {
        return nodeLogEngineFactory.get(nodeId) as? OfficialSdLogEngine
    }
}