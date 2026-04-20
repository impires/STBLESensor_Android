package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.Feature
import com.st.blue_sdk.features.extended.pnpl.PnPL
import com.st.blue_sdk.features.extended.pnpl.PnPLConfig
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCommand
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.filter

@Singleton
class OfficialSdLogEngine @Inject constructor(
    private val blueManager: BlueManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val observeFeatureJobs = ConcurrentHashMap<String, Job>()
    private val pnplFeatures = ConcurrentHashMap<String, PnPL>()
    private val enabledFeatures = ConcurrentHashMap<String, List<Feature<*>>>()
    private val shouldInitDemo = ConcurrentHashMap<String, Boolean>()

    private val _states = MutableStateFlow<Map<String, SdLogState>>(emptyMap())

    suspend fun start(nodeId: String): Result<Unit> {
        return runCatching {
            ensureDemoStarted(nodeId)

            // Ensure SD is actually there before we even try
            waitForSdMounted(nodeId)

            Log.d(TAG, "Configuring node for logging...")
            sendSetTimeCommand(nodeId)
            delay(1000) // Hardware needs time to write to RTC/Flash

            sendSetNameCommand(nodeId)
            delay(1000)

            Log.d(TAG, "Sending START_LOG command...")
            sendCommand(
                nodeId = nodeId,
                cmd = PnPLCmd(
                    component = LOG_CONTROLLER_JSON_KEY,
                    command = "start_log",
                    fields = mapOf("interface" to 2) // Explicitly SD Card
                )
            )

            waitForLoggingState(nodeId = nodeId, expected = true)
            Log.i(TAG, "Successfully started SD logging for $nodeId")

            Unit
        }.onFailure {
            Log.e(TAG, "Failed to start SD logging: ${it.message}")
        }
    }

    suspend fun stop(nodeId: String): Result<Unit> {
        return runCatching {
            ensureDemoStarted(nodeId)

            sendCommand(
                nodeId = nodeId,
                cmd = PnPLCmd.STOP_LOG
            )

            shouldInitDemo[nodeId] = true

            delay(500)

            waitForLoggingState(nodeId = nodeId, expected = false)

            delay(3000)

            Log.d(TAG, "SD log stopped for nodeId=$nodeId")
            Unit
        }.onFailure {
            Log.e(TAG, "SD log stop failed for nodeId=$nodeId", it)
        }
    }

    suspend fun release(nodeId: String) {
        observeFeatureJobs.remove(nodeId)?.cancelAndJoin()

        enabledFeatures.remove(nodeId)?.let { features ->
            runCatching {
                if (features.isNotEmpty()) {
                    blueManager.disableFeatures(nodeId = nodeId, features = features)
                }
            }.onFailure {
                Log.w(TAG, "disableFeatures failed for nodeId=$nodeId", it)
            }
        }

        pnplFeatures.remove(nodeId)
        shouldInitDemo.remove(nodeId)

        _states.update { current ->
            current - nodeId
        }
    }

    private suspend fun ensureDemoStarted(nodeId: String) {
        if (observeFeatureJobs[nodeId]?.isActive == true && pnplFeatures[nodeId] != null) {
            return
        }

        val pnplFeature = blueManager.nodeFeatures(nodeId = nodeId)
            .filter { it.name == PnPL.NAME }
            .filterIsInstance<PnPL>()
            .firstOrNull()
            ?: throw IllegalStateException("PnPL feature not found for nodeId=$nodeId")

        val features = listOf<Feature<*>>(pnplFeature)

        pnplFeatures[nodeId] = pnplFeature
        enabledFeatures[nodeId] = features
        shouldInitDemo[nodeId] = true

        observeFeatureJobs[nodeId] = blueManager.getFeatureUpdates(
            nodeId = nodeId,
            features = features,
            onFeaturesEnabled = { onFeaturesEnabled(nodeId = nodeId) }
        ).flowOn(Dispatchers.IO)
            .map { it.data }
            .onEach { data ->
                if (data is PnPLConfig) {
                    handleStatusUpdate(nodeId = nodeId, data = data)
                    initDemo(nodeId = nodeId)
                }
            }
            .launchIn(scope)

        delay(1500)

        sendGetLogControllerCommand(nodeId)
        delay(400)
        sendGetAllCommand(nodeId)
        delay(400)
    }

    private fun onFeaturesEnabled(nodeId: String) {
        scope.launch {
            val pnplFeature = pnplFeatures[nodeId] ?: return@launch

            val node = blueManager.getNodeWithFirmwareInfo(nodeId = nodeId)

            var maxWriteLength =
                node?.catalogInfo?.characteristics?.firstOrNull { it.name == PnPL.NAME }?.maxWriteLength
                    ?: 20

            node?.let {
                if (maxWriteLength > node.maxPayloadSize) {
                    maxWriteLength = node.maxPayloadSize
                }
            }

            pnplFeature.setMaxPayLoadSize(maxWriteLength)

            sendGetLogControllerCommand(nodeId)
        }
    }

    private suspend fun initDemo(nodeId: String) {
        val mustInit = shouldInitDemo[nodeId] ?: true
        val currentState = _states.value[nodeId] ?: SdLogState()

        if (!mustInit) return
        if (currentState.isLogging) return

        shouldInitDemo[nodeId] = false

        sendSetNameCommand(nodeId)
        delay(500)
        sendGetAllCommand(nodeId)
    }

    private fun handleStatusUpdate(
        nodeId: String,
        data: PnPLConfig
    ) {
        data.deviceStatus.value?.components?.let { json ->
            json.find { it.containsKey(LOG_CONTROLLER_JSON_KEY) }
                ?.get(LOG_CONTROLLER_JSON_KEY)
                ?.jsonObject
                ?.let { logControllerJson ->

                    val sdMounted = parseJsonBoolean(logControllerJson, SD_JSON_KEY)
                    val isLogging = parseJsonBoolean(logControllerJson, LOG_STATUS_JSON_KEY)

                    _states.update { current ->
                        current + (
                                nodeId to SdLogState(
                                    sdMounted = sdMounted,
                                    isLogging = isLogging
                                )
                                )
                    }

                    Log.d(
                        TAG,
                        "nodeId=$nodeId sdMounted=$sdMounted isLogging=$isLogging"
                    )
                }
        }
    }

    private fun parseJsonBoolean(
        json: kotlinx.serialization.json.JsonObject,
        key: String
    ): Boolean {
        val primitive = json[key]?.jsonPrimitive ?: return false
        val raw = primitive.content.trim()

        return when {
            raw.equals("true", ignoreCase = true) -> true
            raw.equals("false", ignoreCase = true) -> false
            raw == "1" -> true
            raw == "0" -> false
            else -> false
        }
    }

    private suspend fun sendCommand(
        nodeId: String,
        cmd: PnPLCmd
    ) {
        val pnplFeature = pnplFeatures[nodeId]
            ?: throw IllegalStateException("PnPL feature not initialized for nodeId=$nodeId")

        blueManager.writeFeatureCommand(
            responseTimeout = 0,
            nodeId = nodeId,
            featureCommand = PnPLCommand(
                feature = pnplFeature,
                cmd = cmd
            )
        )
    }

    private suspend fun sendGetAllCommand(nodeId: String) {
        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd.ALL
        )
    }

    private suspend fun sendGetLogControllerCommand(nodeId: String) {
        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd.LOG_CONTROLLER
        )
    }

    private suspend fun sendSetTimeCommand(nodeId: String) {
        val calendar = Calendar.getInstance()
        val timeInMillis = calendar.timeInMillis
        val sdf = SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.ROOT)
        val datetime = sdf.format(Date(timeInMillis))

        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd(
                component = LOG_CONTROLLER_JSON_KEY,
                command = "set_time",
                fields = mapOf("datetime" to datetime)
            )
        )
    }

    private suspend fun sendSetNameCommand(nodeId: String) {
        val calendar = Calendar.getInstance()
        val timeInMillis = calendar.timeInMillis
        val sdf = SimpleDateFormat(DEFAULT_DATALOG_NAME_FORMAT, Locale.UK)
        val datetime = sdf.format(Date(timeInMillis))

        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd(
                component = "",
                command = ACQUISITION_INFO_JSON_KEY,
                fields = mapOf(
                    NAME_JSON_KEY to datetime,
                    DESC_JSON_KEY to "Empty"
                )
            )
        )
    }

    private suspend fun waitForSdMounted(nodeId: String) {
        withTimeout(8000) {
            while (true) {
                // Polling the hardware
                sendGetLogControllerCommand(nodeId)

                // Check current state flow
                val mounted = _states.value[nodeId]?.sdMounted == true

                if (mounted) {
                    Log.d(TAG, "SD mounted confirmed for nodeId=$nodeId")
                    return@withTimeout
                }
                // Spacing out requests to prevent BLE congestion
                delay(1000)
            }
        }
    }

    private suspend fun waitForLoggingState(
        nodeId: String,
        expected: Boolean
    ) {
        withTimeout(15000) { // Increased to 15s for stability
            _states
                .map { it[nodeId]?.isLogging }
                .filter { it == expected }
                .onEach { Log.d(TAG, "Success: log_status=$it confirmed") }
                .first() // This suspends until the condition is met
        }
    }

    private data class SdLogState(
        val sdMounted: Boolean = false,
        val isLogging: Boolean = false
    )

    companion object {
        private const val TAG = "OfficialSdLogEngine"

        private const val LOG_STATUS_JSON_KEY = "log_status"
        private const val SD_JSON_KEY = "sd_mounted"
        private const val LOG_CONTROLLER_JSON_KEY = "log_controller"

        private const val ACQUISITION_INFO_JSON_KEY = "acquisition_info"
        private const val DESC_JSON_KEY = "description"
        private const val NAME_JSON_KEY = "name"

        private const val DEFAULT_DATALOG_NAME_FORMAT = "EEE MMM d yyyy HH:mm:ss"
    }
}