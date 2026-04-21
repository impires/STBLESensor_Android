package com.st.multinode.logging

import android.util.Log
import javax.inject.Inject

class StartLoggingUseCaseImpl @Inject constructor(
    private val officialSdLogEngine: OfficialSdLogEngine
) : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        Log.d(TAG, "Starting log preparation flow for nodeId=$nodeId")
        
        // Primeiro prepara (montagem SD, set time, etc)
        val prepResult = officialSdLogEngine.prepareForLogging(nodeId)
        if (prepResult.isFailure) {
            Log.e(TAG, "Preparation failed for nodeId=$nodeId", prepResult.exceptionOrNull())
            return prepResult
        }

        // Depois dispara o comando de start efetivo
        val result = officialSdLogEngine.triggerLogging(nodeId)

        if (result.isFailure) {
            Log.e(TAG, "Start trigger failed for nodeId=$nodeId", result.exceptionOrNull())
        } else {
            Log.d(TAG, "Start succeeded for nodeId=$nodeId")
        }

        return result
    }

    companion object {
        private const val TAG = "StartLoggingUseCase"
    }
}