package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.extended.pnpl.PnPL
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCommand
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class BoardSdLoggingTransportImpl @Inject constructor(
    private val blueManager: BlueManager
) : BoardSdLoggingTransport {

    override suspend fun sendPnplCommand(
        nodeId: String,
        json: String
    ) {
        Log.d(TAG, "sendPnplCommand nodeId=$nodeId json=$json")

        val feature = blueManager.nodeFeatures(nodeId)
            .find { it.name == PnPL.NAME } as? PnPL
            ?: throw IllegalStateException("PnPL feature not found for nodeId=$nodeId")

        val root = JSONObject(json)
        if (root.length() == 0) {
            throw IllegalArgumentException("Empty PnPL json")
        }

        val componentName = root.keys().next()
        val componentObject = root.optJSONObject(componentName)
            ?: throw IllegalArgumentException(
                "PnPL json must be like {'component':{'field':value}}"
            )

        val fields = mutableMapOf<String, Any>()

        val fieldIterator = componentObject.keys()
        while (fieldIterator.hasNext()) {
            val fieldName = fieldIterator.next()
            val rawValue = componentObject.get(fieldName)
            fields[fieldName] = jsonValueToKotlin(rawValue)
        }

        Log.d(
            TAG,
            "Writing PnPL change nodeId=$nodeId component=$componentName fields=$fields"
        )

        blueManager.writeFeatureCommand(
            responseTimeout = 0,
            nodeId = nodeId,
            featureCommand = PnPLCommand(
                feature = feature,
                cmd = PnPLCmd(
                    command = componentName,
                    fields = fields
                )
            )
        )
    }

    private fun jsonValueToKotlin(value: Any): Any {
        return when (value) {
            is Boolean -> value
            is Int -> value
            is Long -> value
            is Double -> value
            is String -> value
            is Number -> value.toDouble()
            else -> value.toString()
        }
    }

    companion object {
        private const val TAG = "BoardSdLoggingTransport"
    }
}