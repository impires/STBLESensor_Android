package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.booleanOrNull

@Singleton
class OfficialSdLogCoordinator @Inject constructor(
    private val blueManager: BlueManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val observeJobs = ConcurrentHashMap<String, Job>()
    private val pnplFeatures = ConcurrentHashMap<String, PnPL>()

    private val _states =
        MutableStateFlow<Map<String, HsdlLogControllerState>>(emptyMap())

    suspend fun start(nodeId: String): Result<Unit> {
        return runCatching {
            ensurePnplObservation(nodeId)

            // 1. Initial Sync
            sendGetAllCommand(nodeId)
            delay(1000) // Give more time for the initial heavy sync

            // 2. Single Refresh instead of repeat(3)
            sendGetLogControllerCommand(nodeId)

            // 3. Wait for SD with longer polling
            waitForSdMounted(nodeId)

            // 4. Time Sync (Critical)
            sendSetTimeCommand(nodeId)
            delay(600) // Increase spacing

            // 5. Start Logging
            sendStartLogCommand(nodeId)

            // 6. Verify
            waitForLoggingState(nodeId = nodeId, expected = true)
        }
    }

    suspend fun stop(nodeId: String): Result<Unit> {
        return runCatching {
            ensurePnplObservation(nodeId)

            Log.d(TAG, "HSDL stop sequence begin nodeId=$nodeId")

            refreshLogController(nodeId)

            sendStopLogCommand(nodeId)
            waitForLoggingState(nodeId = nodeId, expected = false)

            delay(3000)

            Log.d(TAG, "HSDL stop sequence completed nodeId=$nodeId")
        }
    }

    suspend fun release(nodeId: String) {
        observeJobs.remove(nodeId)?.cancelAndJoin()

        pnplFeatures.remove(nodeId)?.let { pnpl ->
            runCatching {
                blueManager.disableFeatures(nodeId = nodeId, features = listOf(pnpl))
            }.onFailure {
                Log.w(TAG, "disableFeatures failed for nodeId=$nodeId", it)
            }
        }

        _states.update { it - nodeId }
    }

    private suspend fun ensurePnplObservation(nodeId: String) {
        if (observeJobs[nodeId]?.isActive == true && pnplFeatures[nodeId] != null) {
            return
        }

        val pnplFeature = blueManager.nodeFeatures(nodeId)
            .firstOrNull { it.name == PnPL.NAME } as? PnPL
            ?: throw IllegalStateException("PnPL feature not found for nodeId=$nodeId")

        pnplFeatures[nodeId] = pnplFeature

        val job = scope.launch {
            blueManager.getFeatureUpdates(
                nodeId = nodeId,
                features = listOf(pnplFeature),
                autoEnable = true
            ).collect { update ->
                val pnplConfig = update.data as? PnPLConfig ?: return@collect
                handlePnplConfig(nodeId = nodeId, data = pnplConfig)
            }
        }

        observeJobs[nodeId] = job
        delay(600)
    }

    private fun handlePnplConfig(
        nodeId: String,
        data: PnPLConfig
    ) {
        data.deviceStatus.value?.components?.let { json ->
            json.find { it.containsKey(LOG_CONTROLLER_JSON_KEY) }
                ?.get(LOG_CONTROLLER_JSON_KEY)
                ?.jsonObject
                ?.let { logControllerJson ->

                    val sdMounted =
                        logControllerJson[SD_JSON_KEY]?.jsonPrimitive?.booleanOrNull ?: false

                    val isLogging =
                        logControllerJson[LOG_STATUS_JSON_KEY]?.jsonPrimitive?.booleanOrNull
                            ?: false

                    updateState(
                        nodeId = nodeId,
                        sdMounted = sdMounted,
                        isLogging = isLogging
                    )

                    Log.d(
                        TAG,
                        "PnPL status nodeId=$nodeId sdMounted=$sdMounted isLogging=$isLogging"
                    )
                }
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
        Log.d(TAG, "sendGetAllCommand nodeId=$nodeId")

        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd.ALL
        )
    }

    private suspend fun sendGetLogControllerCommand(nodeId: String) {
        Log.d(TAG, "sendGetLogControllerCommand nodeId=$nodeId")

        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd.LOG_CONTROLLER
        )
    }

    private suspend fun sendSetTimeCommand(nodeId: String) {
        val datetime = SimpleDateFormat(
            "yyyyMMdd_HH_mm_ss",
            Locale.ROOT
        ).format(Date())

        Log.d(TAG, "sendSetTimeCommand nodeId=$nodeId datetime=$datetime")

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
        val acquisitionName = SimpleDateFormat(
            "EEE_MMM_d_yyyy_HH_mm_ss",
            Locale.UK
        ).format(Date())

        Log.d(TAG, "sendSetNameCommand nodeId=$nodeId acquisitionName=$acquisitionName")

        // Em algumas firmwares isto funciona como set de propriedades em acquisition_info.
        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd(
                command = ACQUISITION_INFO_JSON_KEY,
                fields = mapOf(NAME_JSON_KEY to acquisitionName)
            )
        )
    }

    private suspend fun sendStartLogCommand(nodeId: String) {
        Log.d(TAG, "sendStartLogCommand nodeId=$nodeId")

        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd(
                component = LOG_CONTROLLER_JSON_KEY,
                command = "start_log",
                fields = mapOf("interface" to 2)
            )
        )
    }

    private suspend fun sendStopLogCommand(nodeId: String) {
        Log.d(TAG, "sendStopLogCommand nodeId=$nodeId")

        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd(
                component = LOG_CONTROLLER_JSON_KEY,
                command = "stop_log",
                fields = mapOf("interface" to 2)
            )
        )
    }

    private suspend fun refreshLogController(nodeId: String) {
        repeat(3) {
            sendGetLogControllerCommand(nodeId)
            delay(300)
        }
    }

    private suspend fun waitForSdMounted(nodeId: String) {
        withTimeout(8000) {
            while (true) {
                sendGetLogControllerCommand(nodeId)
                val mounted = _states
                    .map { it[nodeId]?.sdMounted == true }
                    .first()

                if (mounted) {
                    Log.d(TAG, "SD mounted confirmed for nodeId=$nodeId")
                    return@withTimeout
                }

                delay(400)
            }
        }
    }

    private suspend fun waitForLoggingState(
        nodeId: String,
        expected: Boolean
    ) {
        withTimeout(12000) {
            while (true) {
                sendGetLogControllerCommand(nodeId)
                val current = _states
                    .map { it[nodeId]?.isLogging == expected }
                    .first()

                if (current) {
                    Log.d(TAG, "log_status=$expected confirmed for nodeId=$nodeId")
                    return@withTimeout
                }

                delay(500)
            }
        }
    }

    private fun updateState(
        nodeId: String,
        sdMounted: Boolean,
        isLogging: Boolean
    ) {
        _states.update { current ->
            current + (
                    nodeId to HsdlLogControllerState(
                        sdMounted = sdMounted,
                        isLogging = isLogging
                    )
                    )
        }
    }

    private data class HsdlLogControllerState(
        val sdMounted: Boolean = false,
        val isLogging: Boolean = false
    )

    companion object {
        private const val TAG = "OfficialSdLogCoord"

        private const val LOG_CONTROLLER_JSON_KEY = "log_controller"
        private const val LOG_STATUS_JSON_KEY = "log_status"
        private const val SD_JSON_KEY = "sd_mounted"

        private const val ACQUISITION_INFO_JSON_KEY = "acquisition_info"
        private const val NAME_JSON_KEY = "name"
    }
}