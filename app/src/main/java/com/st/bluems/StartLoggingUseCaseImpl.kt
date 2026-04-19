package com.st.bluems

import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.logger.CsvFileLogger
import javax.inject.Inject

class StartLoggingUseCaseImpl @Inject constructor(
    private val blueManager: BlueManager
) : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        return try {
            blueManager.enableAllLoggers(
                nodeId = nodeId,
                loggerTags = listOf(CsvFileLogger.TAG)
            )
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}