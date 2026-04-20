package com.st.multinode.logging

import com.st.blue_sdk.BlueManager
import com.st.multinode.MultiNodeCsvFileLogger
import javax.inject.Inject

class StopLoggingUseCaseImpl @Inject constructor(
    private val blueManager: BlueManager
) : StopLoggingUseCase {

    override suspend fun stop(nodeId: String): Result<Unit> {
        return try {
            blueManager.disableAllLoggers(
                nodeId = nodeId,
                loggerTags = listOf(MultiNodeCsvFileLogger.TAG)
            )
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}