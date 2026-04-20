package com.st.multinode

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcquisitionServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun startLogging(
        nodeIds: List<String>,
        enableServer: Boolean = false,
        maxPayloadSize: Int = 248,
        maxConnectionRetries: Int = 3
    ) {
        if (nodeIds.isEmpty()) return

        val intent = Intent(context, MultiNodeAcquisitionService::class.java).apply {
            action = MultiNodeAcquisitionService.ACTION_START_LOGGING
            putStringArrayListExtra(
                MultiNodeAcquisitionService.EXTRA_NODE_IDS,
                ArrayList(nodeIds)
            )
            putExtra(MultiNodeAcquisitionService.EXTRA_ENABLE_SERVER, enableServer)
            putExtra(MultiNodeAcquisitionService.EXTRA_MAX_PAYLOAD_SIZE, maxPayloadSize)
            putExtra(
                MultiNodeAcquisitionService.EXTRA_MAX_CONNECTION_RETRIES,
                maxConnectionRetries
            )
        }

        ContextCompat.startForegroundService(context, intent)
    }

    fun stopLogging(nodeIds: List<String>) {
        if (nodeIds.isEmpty()) return

        val intent = Intent(context, MultiNodeAcquisitionService::class.java).apply {
            action = MultiNodeAcquisitionService.ACTION_STOP_LOGGING
            putStringArrayListExtra(
                MultiNodeAcquisitionService.EXTRA_NODE_IDS,
                ArrayList(nodeIds)
            )
        }

        context.startService(intent)
    }

    fun stopAll() {
        val intent = Intent(context, MultiNodeAcquisitionService::class.java).apply {
            action = MultiNodeAcquisitionService.ACTION_STOP_ALL
        }

        context.startService(intent)
    }
}