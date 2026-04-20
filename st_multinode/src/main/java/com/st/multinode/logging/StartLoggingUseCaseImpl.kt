package com.st.multinode.logging

import android.util.Log
import kotlinx.coroutines.delay
import javax.inject.Inject

class StartLoggingUseCaseImpl @Inject constructor(
    private val boardSdLoggingTransport: BoardSdLoggingTransport
) : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        return try {
            val enableResult = boardSdLoggingTransport.setProperty(
                nodeId = nodeId,
                component = "stts22h_temp",
                fields = mapOf(
                    "enable" to true,
                    "odr" to 1
                )
            )

            if (enableResult.isFailure) {
                return enableResult
            }

            delay(500)

            val startResult = boardSdLoggingTransport.startSdLogging(nodeId)
            if (startResult.isFailure) {
                return startResult
            }

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