package com.st.multinode.logging

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class StartLoggingUseCaseImpl @Inject constructor(
    private val boardSdLoggingTransport: BoardSdLoggingTransport
) : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting board SD logging for nodeId=$nodeId")

            val sensorSetupCommands = listOf(
                """{"stts22h_temp":{"enable":true,"odr":1}}""",
                """{"lps22df_press":{"enable":true,"odr":1}}""",
                """{"lsm6dsv16x_acc":{"enable":true,"odr":30}}""",
                """{"lsm6dsv16x_gyro":{"enable":true,"odr":30}}""",
                """{"lis2mdl_mag":{"enable":true,"odr":20}}"""
            )

            var enabledCount = 0

            sensorSetupCommands.forEach { json ->
                runCatching {
                    boardSdLoggingTransport.sendPnplCommand(
                        nodeId = nodeId,
                        json = json
                    )
                    enabledCount += 1
                    Log.d(TAG, "Sensor setup accepted: $json")
                }.onFailure { t ->
                    Log.w(TAG, "Sensor setup rejected: $json", t)
                }

                delay(150)
            }

            if (enabledCount == 0) {
                return Result.failure(
                    IllegalStateException(
                        "No MKBOXPRO sensor component accepted the enable command"
                    )
                )
            }

            delay(500)

            boardSdLoggingTransport.sendPnplCommand(
                nodeId = nodeId,
                json = """{"log_controller":{"start_log":true}}"""
            )

            delay(500)

            Log.d(
                TAG,
                "Board SD logging started for nodeId=$nodeId with $enabledCount enabled component(s)"
            )

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