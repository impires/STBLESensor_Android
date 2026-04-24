package com.st.multinode.logging

import javax.inject.Inject

class StopLoggingUseCaseImpl @Inject constructor(
    private val nodeLogEngineFactory: NodeLogEngineFactory
) : StopLoggingUseCase {

    override suspend fun stop(nodeId: String): Result<Unit> = runCatching {
        nodeLogEngineFactory.get(nodeId).stopFlow(nodeId)
    }
}