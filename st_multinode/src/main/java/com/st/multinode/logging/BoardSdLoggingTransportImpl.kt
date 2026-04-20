package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.extended.pnpl.PnPL
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCommand
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardSdLoggingTransportImpl @Inject constructor(
    private val blueManager: BlueManager
) : BoardSdLoggingTransport {

    override suspend fun startSdLogging(nodeId: String): Result<Unit> {
        return runCatching {
            val feature = requirePnpl(nodeId)

            Log.d(TAG, "Sending START SD logging to $nodeId")

            blueManager.writeFeatureCommand(
                responseTimeout = 0,
                nodeId = nodeId,
                featureCommand = PnPLCommand(
                    feature = feature,
                    cmd = PnPLCmd(
                        component = "log_controller",
                        command = "start_log",
                        fields = mapOf("interface" to 0)
                    )
                )
            )
        }
    }

    override suspend fun stopSdLogging(nodeId: String): Result<Unit> {
        return runCatching {
            val feature = requirePnpl(nodeId)

            Log.d(TAG, "Sending STOP SD logging to $nodeId")

            blueManager.writeFeatureCommand(
                responseTimeout = 0,
                nodeId = nodeId,
                featureCommand = PnPLCommand(
                    feature = feature,
                    cmd = PnPLCmd(
                        component = "log_controller",
                        command = "stop_log",
                        fields = mapOf("interface" to 0)
                    )
                )
            )
        }
    }

    override suspend fun setProperty(
        nodeId: String,
        component: String,
        fields: Map<String, Any>
    ): Result<Unit> {
        return runCatching {
            val feature = requirePnpl(nodeId)

            Log.d(TAG, "Setting PnPL property on $nodeId component=$component fields=$fields")

            blueManager.writeFeatureCommand(
                responseTimeout = 0,
                nodeId = nodeId,
                featureCommand = PnPLCommand(
                    feature = feature,
                    cmd = PnPLCmd(
                        command = component,
                        fields = fields
                    )
                )
            )
        }
    }

    private fun requirePnpl(nodeId: String): PnPL {
        return blueManager.nodeFeatures(nodeId)
            .firstOrNull { it.name == PnPL.NAME } as? PnPL
            ?: throw IllegalStateException("PnPL feature not found for nodeId=$nodeId")
    }

    companion object {
        private const val TAG = "BoardSdLoggingTransport"
    }
}