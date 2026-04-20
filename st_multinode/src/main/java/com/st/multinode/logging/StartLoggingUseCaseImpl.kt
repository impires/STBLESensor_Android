package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
import com.st.multinode.MultiNodeCsvFileLogger
import javax.inject.Inject

class StartLoggingUseCaseImpl @Inject constructor(
    private val blueManager: BlueManager
) : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        Log.d("StartLoggingUseCase", "start() called for nodeId=$nodeId")
        return try {
            blueManager.enableAllLoggers(
                nodeId = nodeId,
                loggerTags = listOf(MultiNodeCsvFileLogger.TAG)
            )
            Log.d("StartLoggingUseCase", "enableAllLoggers success for nodeId=$nodeId")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e("StartLoggingUseCase", "enableAllLoggers failed for nodeId=$nodeId", t)
            Result.failure(t)
        }
    }
}