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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Singleton
class OfficialSdLogEngine @Inject constructor(
    private val blueManager: BlueManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val observeFeatureJobs = ConcurrentHashMap<String, Job>()
    private val pnplFeatures = ConcurrentHashMap<String, PnPL>()

    private val pnpLock = Mutex()

    private val _states = MutableStateFlow<Map<String, SdLogState>>(emptyMap())

    suspend fun start(nodeId: String): Result<Unit> {
        return runCatching {
            ensureDemoStarted(nodeId)

            // 1. Polling de montagem do SD
            withTimeout(20000) {
                while (_states.value[nodeId]?.sdMounted != true) {
                    Log.d(TAG, "Checando SD...")
                    sendGetLogControllerCommand(nodeId)
                    delay(3000) // Delay alto é amigo do firmware
                }
            }

            // 2. Configurações pré-log (Sempre com delay entre elas)
            Log.d(TAG, "Configurando Nome...")
            sendSetNameCommand(nodeId)
            delay(1500)

            Log.d(TAG, "Configurando Hora...")
            sendSetTimeCommand(nodeId)
            delay(1500)

            // 3. Comando de Start Real
            Log.d(TAG, "Enviando START_LOG...")
            val startCmd = PnPLCmd(
                component = "log_controller",
                command = "start_log",
                fields = mapOf("interface" to 2) // 2 = SD Card
            )
            sendCommand(nodeId, startCmd)

            // 4. Verificação final
            waitForLoggingState(nodeId, true)
        }
    }

    suspend fun stop(nodeId: String): Result<Unit> {
        return runCatching {
            ensureDemoStarted(nodeId)

            sendCommand(
                nodeId = nodeId,
                cmd = PnPLCmd.STOP_LOG
            )

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

        pnplFeatures.remove(nodeId)?.let { pnpl ->
            runCatching {
                blueManager.disableFeatures(nodeId = nodeId, features = listOf(pnpl))
            }.onFailure {
                Log.w(TAG, "disableFeatures failed for nodeId=$nodeId", it)
            }
        }

        _states.update { current ->
            current - nodeId
        }
    }

    private suspend fun ensureDemoStarted(nodeId: String) {
        pnpLock.withLock {
            if (observeFeatureJobs[nodeId]?.isActive == true && pnplFeatures[nodeId] != null) return

            val pnplFeature = blueManager.nodeFeatures(nodeId = nodeId)
                .filterIsInstance<PnPL>()
                .firstOrNull() ?: return

            pnplFeatures[nodeId] = pnplFeature

            val job = scope.launch {
                blueManager.getFeatureUpdates(
                    nodeId = nodeId,
                    features = listOf(pnplFeature),
                    autoEnable = true
                ).collect { update ->
                    val pnplConfig = update.data as? PnPLConfig ?: return@collect
                    handleStatusUpdate(nodeId, pnplConfig)
                }
            }
            observeFeatureJobs[nodeId] = job

            delay(600)
            sendGetLogControllerCommand(nodeId)
        }
    }

    private fun handleStatusUpdate(nodeId: String, data: PnPLConfig) {
        runCatching {
            val components = data.deviceStatus.value?.components ?: return

            // Procura pelo log_controller ou sdlog
            val logControllerJson = components.find {
                it.containsKey(LOG_CONTROLLER_JSON_KEY) || it.containsKey("sdlog")
            }?.values?.firstOrNull()?.jsonObject ?: return

            val sdMounted = parseJsonBoolean(logControllerJson, SD_JSON_KEY)
            val isLogging = parseJsonBoolean(logControllerJson, LOG_STATUS_JSON_KEY)

            _states.update { current ->
                val old = current[nodeId]
                if (old?.sdMounted != sdMounted || old?.isLogging != isLogging) {
                    Log.i(TAG, ">>> STATUS [$nodeId]: SD=$sdMounted, LOG=$isLogging")
                    current + (nodeId to SdLogState(sdMounted = sdMounted, isLogging = isLogging))
                } else {
                    current
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Erro ao processar status JSON: ${e.message}")
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

    private suspend fun sendGetLogControllerCommand(nodeId: String) {
        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd.LOG_CONTROLLER // Use a constante oficial do SDK
        )
    }

    private suspend fun sendSetNameCommand(nodeId: String) {
        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd(
                component = "log_controller", // O PRO costuma centralizar aqui
                command = "set_filename",     // O comando correto costuma ser set_filename e não acquisition_info
                fields = mapOf("name" to "L")
            )
        )
    }

    private suspend fun sendSetTimeCommand(nodeId: String) {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT) // Sem underline
        val datetime = sdf.format(Date())

        sendCommand(
            nodeId = nodeId,
            cmd = PnPLCmd(
                component = "", // Global
                command = "set_time",
                fields = mapOf("datetime" to datetime)
            )
        )
    }

    private suspend fun waitForLoggingState(
        nodeId: String,
        expected: Boolean
    ) {
        withTimeout(15000) {
            _states
                .map { it[nodeId]?.isLogging }
                .filter { it == expected }
                .first()
            Log.d(TAG, "log_status=$expected confirmed for nodeId=$nodeId")
        }
    }

    data class SdLogState(
        val sdMounted: Boolean = false,
        val isLogging: Boolean = false
    )

    companion object {
        private const val TAG = "OfficialSdLogEngine"

        private const val LOG_STATUS_JSON_KEY = "log_status"
        private const val SD_JSON_KEY = "sd_mounted"
        private const val LOG_CONTROLLER_JSON_KEY = "log_controller"
    }
}