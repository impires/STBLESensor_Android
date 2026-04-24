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
import kotlinx.coroutines.flow.filter
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
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val observeJobs = ConcurrentHashMap<String, Job>()
    val pnplFeatures = ConcurrentHashMap<String, PnPL>()

    private val _states = MutableStateFlow<Map<String, HsdlLogControllerState>>(emptyMap())

    suspend fun start(nodeId: String): Result<Unit> {
        return runCatching {
            delay(1000)
            ensurePnplObservation(nodeId)
            delay(1000)
            sendGetAllCommand(nodeId)
            delay(1000)
            sendGetLogControllerCommand(nodeId)
            waitForSdMounted(nodeId)
            sendSetTimeCommand(nodeId)
            delay(600)

            // Aqui chamamos o engine ou a função interna corrigida
            sendStartLogCommand(nodeId)
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
        if (observeJobs[nodeId]?.isActive == true && pnplFeatures[nodeId] != null) return

        val pnplFeature = blueManager.nodeFeatures(nodeId)
            .firstOrNull { it.name == PnPL.NAME } as? PnPL
            ?: throw IllegalStateException("PnPL not found")

        pnplFeatures[nodeId] = pnplFeature

        val job = scope.launch {
            blueManager.getFeatureUpdates(nodeId, listOf(pnplFeature), true).collect { update ->
                (update.data as? PnPLConfig)?.let { handlePnplConfig(nodeId, it) }
            }
        }
        observeJobs[nodeId] = job
    }

    private fun handlePnplConfig(nodeId: String, data: PnPLConfig) {
        data.deviceStatus.value?.components?.let { json ->
            json.find { it.containsKey(LOG_CONTROLLER_JSON_KEY) }
                ?.get(LOG_CONTROLLER_JSON_KEY)?.jsonObject?.let { logJson ->
                    updateState(
                        nodeId,
                        sdMounted = logJson[SD_JSON_KEY]?.jsonPrimitive?.booleanOrNull ?: false,
                        isLogging = logJson[LOG_STATUS_JSON_KEY]?.jsonPrimitive?.booleanOrNull ?: false
                    )
                }
        }
    }

    // Tornamos esta função pública ou acessível para o Engine se necessário
    suspend fun sendStartLogCommand(nodeId: String) {
        val pnpl = pnplFeatures[nodeId] ?: return
        // O SDK espera fields mapOf("interface" to 2) para Custom App no SD
        sendCommand(
            nodeId,
            PnPLCmd(LOG_CONTROLLER_JSON_KEY, "start_log", fields = mapOf("interface" to 2))
        )
    }

    private suspend fun sendCommand(nodeId: String, cmd: PnPLCmd) {
        val pnpl = pnplFeatures[nodeId] ?: return
        // CORREÇÃO: writeFeatureCommand(nodeId, PnPLCommand(feature, cmd), timeout)
        blueManager.writeFeatureCommand(nodeId, PnPLCommand(pnpl, cmd), 0L)
    }

    private suspend fun sendGetAllCommand(nodeId: String) = sendCommand(nodeId, PnPLCmd.ALL)
    private suspend fun sendGetLogControllerCommand(nodeId: String) = sendCommand(nodeId, PnPLCmd.LOG_CONTROLLER)

    private suspend fun sendSetTimeCommand(nodeId: String) {
        val datetime = SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.ROOT).format(Date())
        sendCommand(
            nodeId,
            PnPLCmd(LOG_CONTROLLER_JSON_KEY, "set_time", fields = mapOf("datetime" to datetime))
        )
    }

    private suspend fun waitForSdMounted(nodeId: String) {
        withTimeout(25000) {
            _states.map { it[nodeId]?.sdMounted }.filter { it == true }.first()
        }
    }

    private suspend fun waitForLoggingState(nodeId: String, expected: Boolean) {
        withTimeout(25000) {
            _states.map { it[nodeId]?.isLogging }.filter { it == expected }.first()
        }
    }

    private fun updateState(nodeId: String, sdMounted: Boolean, isLogging: Boolean) {
        _states.update { it + (nodeId to HsdlLogControllerState(sdMounted, isLogging)) }
    }

    private data class HsdlLogControllerState(val sdMounted: Boolean = false, val isLogging: Boolean = false)

    companion object {
        // Removido PRIVATE para o Engine conseguir aceder
        const val TAG = "OfficialSdLogCoord"
        const val LOG_CONTROLLER_JSON_KEY = "log_controller"
        const val LOG_STATUS_JSON_KEY = "log_status"
        const val SD_JSON_KEY = "sd_mounted"
        const val ACQUISITION_INFO_JSON_KEY = "acquisition_info"
        const val NAME_JSON_KEY = "name"
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
}