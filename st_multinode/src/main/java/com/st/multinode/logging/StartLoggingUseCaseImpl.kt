package com.st.multinode.logging

import android.util.Log
import javax.inject.Inject

class StartLoggingUseCaseImpl @Inject constructor(
    private val officialSdLogEngine: OfficialSdLogEngine
) : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        val result = officialSdLogEngine.start(nodeId)

        if (result.isFailure) {
            Log.e(TAG, "Start failed for nodeId=$nodeId", result.exceptionOrNull())
        } else {
            Log.d(TAG, "Start succeeded for nodeId=$nodeId")
        }

        return result
    }

    companion object {
        private const val TAG = "StartLoggingUseCase"
    }
}