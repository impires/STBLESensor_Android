package com.st.multinode.logging

import android.util.Log
import javax.inject.Inject

class StartLoggingUseCaseImpl @Inject constructor(
    private val boardSdLoggingTransport: BoardSdLoggingTransport
) : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        return try {
            boardSdLoggingTransport.sendPnplCommand(
                nodeId = nodeId,
                json = """{"log_controller":{"start_log":true}}"""
            )

            Log.d(TAG, "Board SD logging started for nodeId=$nodeId")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Board SD logging start failed for nodeId=$nodeId", t)
            Result.failure(t)
        }
    }

    companion object {
        private const val TAG = "StartLoggingUseCase"
    }
}