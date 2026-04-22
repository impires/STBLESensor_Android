package com.st.multinode.logging

import android.util.Log
import javax.inject.Inject

class StartLoggingUseCaseImpl @Inject constructor() : StartLoggingUseCase {

    override suspend fun start(nodeId: String): Result<Unit> {
        val ex = IllegalStateException(
            "StartLoggingUseCaseImpl no longer starts logging directly. " +
                    "Use MultiNodeAcquisitionService + SdFlowStarter and pass EXTRA_FLOW_FILE_NAME."
        )
        Log.e(TAG, "Legacy start path called for nodeId=$nodeId", ex)
        return Result.failure(ex)
    }

    companion object {
        private const val TAG = "StartLoggingUseCase"
    }
}