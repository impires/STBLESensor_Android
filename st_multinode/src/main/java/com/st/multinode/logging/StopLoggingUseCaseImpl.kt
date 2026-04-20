package com.st.multinode.logging

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.delay

class StopLoggingUseCaseImpl @Inject constructor(
    private val boardSdLoggingTransport: BoardSdLoggingTransport
) : StopLoggingUseCase {

    override suspend fun stop(nodeId: String): Result<Unit> {
        return try {
            boardSdLoggingTransport.sendPnplCommand(
                nodeId = nodeId,
                json = """{"log_controller":{"start_log":false}}"""
            )

            delay(3000)

            Log.d(TAG, "Board SD logging stopped for nodeId=$nodeId")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Board SD logging stop failed for nodeId=$nodeId", t)
            Result.failure(t)
        }
    }

    companion object {
        private const val TAG = "StopLoggingUseCase"
    }
}