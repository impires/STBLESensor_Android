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
                "PnPL json must be like {\"component\":{\"field\":value}}"
            )

        val pnplCmd = buildPnplCmd(
            componentName = componentName,
            componentObject = componentObject
        )

        Log.d(TAG, "Writing PnPL nodeId=$nodeId cmd=$pnplCmd")

        blueManager.writeFeatureCommand(
            responseTimeout = 0,
            nodeId = nodeId,
            featureCommand = PnPLCommand(
                feature = feature,
                cmd = pnplCmd
            )
        )
    }

    private fun buildPnplCmd(
        componentName: String,
        componentObject: JSONObject
    ): PnPLCmd {
        return if (componentName == "log_controller") {
            buildLogControllerCmd(componentName, componentObject)
        } else {
            buildPropertySetCmd(componentName, componentObject)
        }
    }

    private fun buildPropertySetCmd(
        componentName: String,
        componentObject: JSONObject
    ): PnPLCmd {
        val fields = extractFields(componentObject)

        Log.d(
            TAG,
            "Property SET component=$componentName fields=$fields"
        )

        return PnPLCmd(
            command = componentName,
            fields = fields
        )
    }

    private fun buildLogControllerCmd(
        componentName: String,
        componentObject: JSONObject
    ): PnPLCmd {
        val keys = mutableListOf<String>()
        val it = componentObject.keys()
        while (it.hasNext()) {
            keys += it.next()
        }

        if (keys.isEmpty()) {
            throw IllegalArgumentException("log_controller command is empty")
        }

        val entryName = keys.first()
        val rawValue = componentObject.get(entryName)

        return when {
            entryName == "start_log" && rawValue is Boolean && rawValue -> {
                Log.d(TAG, "COMMAND component=log_controller command=start_log")
                PnPLCmd(
                    component = componentName,
                    command = "start_log"
                )
            }

            entryName == "start_log" && rawValue is Boolean && !rawValue -> {
                Log.d(TAG, "COMMAND component=log_controller command=stop_log")
                PnPLCmd(
                    component = componentName,
                    command = "stop_log"
                )
            }

            entryName == "stop_log" && rawValue is Boolean && rawValue -> {
                Log.d(TAG, "COMMAND component=log_controller command=stop_log")
                PnPLCmd(
                    component = componentName,
                    command = "stop_log"
                )
            }

            rawValue is JSONObject -> {
                val commandFields = extractFields(rawValue)

                Log.d(
                    TAG,
                    "COMMAND component=$componentName command=$entryName fields=$commandFields"
                )

                PnPLCmd(
                    component = componentName,
                    command = entryName,
                    fields = commandFields
                )
            }

            else -> {
                Log.d(TAG, "COMMAND component=$componentName command=$entryName")
                PnPLCmd(
                    component = componentName,
                    command = entryName
                )
            }
        }
    }

    private fun extractFields(obj: JSONObject): Map<String, Any> {
        val fields = linkedMapOf<String, Any>()
        val fieldIterator = obj.keys()

        while (fieldIterator.hasNext()) {
            val fieldName = fieldIterator.next()
            val rawValue = obj.get(fieldName)
            fields[fieldName] = jsonValueToKotlin(rawValue)
        }

        return fields
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