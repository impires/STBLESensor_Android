package com.st.multinode.logging

import android.util.Log
import javax.inject.Inject

class StopLoggingUseCaseImpl @Inject constructor(
    private val officialSdLogEngine: OfficialSdLogEngine
) : StopLoggingUseCase {

    override suspend fun stop(nodeId: String): Result<Unit> {
        val result = officialSdLogEngine.stop(nodeId)

        if (result.isFailure) {
            Log.e(TAG, "Stop failed for nodeId=$nodeId", result.exceptionOrNull())
        } else {
            Log.d(TAG, "Stop succeeded for nodeId=$nodeId")
        }

        return result
    }

    companion object {
        private const val TAG = "StopLoggingUseCase"
    }
}