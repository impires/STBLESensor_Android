package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.Feature
import com.st.blue_sdk.features.FeatureField
import com.st.blue_sdk.features.extended.pnpl.PnPL
import com.st.blue_sdk.features.extended.pnpl.PnPLConfig
import com.st.blue_sdk.features.extended.pnpl.model.PnPLDevice
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCommand
import java.io.ByteArrayOutputStream
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

@Singleton
class OfficialSdLogEngine @Inject constructor(
    private val blueManager: BlueManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val observeFeatureJobs = ConcurrentHashMap<String, Job>()
    private val pnplFeatures = ConcurrentHashMap<String, PnPL>()
    private val nodeBuffers = ConcurrentHashMap<String, ByteArrayOutputStream>()
    private val nodeComponentNames = ConcurrentHashMap<String, String>()
    private val initialSyncAttempted = Collections.synchronizedSet(mutableSetOf<String>())

    private val nodeControllerNames = ConcurrentHashMap<String, String>()
    private val nodeFileNames = ConcurrentHashMap<String, String>()

    private val pnpLock = Mutex()
    private val commandMutex = Mutex()

    private val _states = MutableStateFlow<Map<String, SdLogState>>(emptyMap())
    val states = _states

    private val jsonHandler = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun sendRawCommand(nodeId: String, cmd: PnPLCmd) {
        sendCommand(nodeId, cmd)
    }

    suspend fun requestStatus(nodeId: String) {
        val compName = getLogComponentName(nodeId)
        Log.d(TAG, "[$nodeId] Solicitando status (comp=$compName)")
        sendCommand(nodeId, PnPLCmd(component = compName, command = "get_status"))
    }

    suspend fun requestStatusFor(nodeId: String, component: String) {
        Log.d(TAG, "[$nodeId] Solicitando status explícito em $component")
        sendCommand(nodeId, PnPLCmd(component = component, command = "get_status"))
    }

    suspend fun stop(nodeId: String): Result<Unit> = runCatching {
        val compName = getLogComponentName(nodeId)

        sendCommand(
            nodeId,
            PnPLCmd(
                component = compName,
                command = "stop_log",
                fields = mapOf("interface" to 2)
            )
        )
    }

    suspend fun release(nodeId: String) {
        observeFeatureJobs.remove(nodeId)?.cancelAndJoin()
        pnplFeatures.remove(nodeId)
        nodeBuffers.remove(nodeId)
        nodeComponentNames.remove(nodeId)
        initialSyncAttempted.remove(nodeId)
        nodeControllerNames.remove(nodeId)
        nodeFileNames.remove(nodeId)

        _states.update { current ->
            current.toMutableMap().apply {
                remove(nodeId)
            }
        }
    }

    fun getLogComponentName(nodeId: String): String {
        return nodeComponentNames[nodeId] ?: SD_LOG_COMPONENT
    }

    fun getControllerComponentName(): String = LOG_CONTROLLER_COMPONENT

    suspend fun ensurePnplReady(nodeId: String) {
        val pnplFeature = pnpLock.withLock {
            pnplFeatures[nodeId]?.let { return@withLock it }

            val features = blueManager.nodeFeatures(nodeId = nodeId)
            Log.d(TAG, "[$nodeId] Features disponíveis: ${features.map { it.javaClass.name }}")

            val feature = features.filterIsInstance<PnPL>().firstOrNull()

            if (feature != null) {
                runCatching {
                    val field = Feature::class.java.getDeclaredField("isDataNotifyFeature")
                    field.isAccessible = true
                    field.set(feature, true)
                }

                feature.setMaxPayLoadSize(240)
                pnplFeatures[nodeId] = feature
                return@withLock feature
            }

            Log.e(TAG, "[$nodeId] Nenhuma feature PnPL encontrada. Features do nó: ${features.map { it.javaClass.name }}")
            null
        } ?: throw IllegalStateException("[$nodeId] Nenhuma feature PnPL encontrada")

        var needsWait = false

        pnpLock.withLock {
            if (observeFeatureJobs[nodeId]?.isActive != true) {
                Log.d(TAG, "[$nodeId] (Re)Iniciando observação PnPL...")

                val updateFlow = blueManager.getFeatureUpdates(nodeId, listOf(pnplFeature), true)

                Log.d(TAG, "[$nodeId] SUBSCRIBED TO PnPL FLOW")

                observeFeatureJobs[nodeId] = scope.launch {
                    updateFlow
                        .catch { e ->
                            Log.e(TAG, "[$nodeId] Erro no stream PnPL", e)
                        }
                        .collect { update ->
                            Log.d(TAG, "[$nodeId] ENTERED COLLECT")

                            val rawData = update.rawData
                            val config = update.data as? PnPLConfig

                            Log.v(
                                TAG,
                                "[$nodeId] PnPL RX Raw (hex): ${rawData.joinToString(" ") { "%02X".format(it) }}"
                            )

                            if (isUsefulPnplConfig(config)) {
                                Log.d(TAG, "[$nodeId] PnPLConfig final útil recebido")
                                handleStatusUpdate(nodeId, config!!)
                                return@collect
                            }

                            Log.v(TAG, "[$nodeId] Fragmento PnPL recebido (raw=${rawData.size} bytes), tentando reassemblagem...")

                            val extracted = manualExtractPnPL(nodeId, rawData)
                            if (extracted != null) {
                                Log.d(TAG, "[$nodeId] Reassemblagem PnPL completa com sucesso")
                                handleStatusUpdate(nodeId, extracted)
                            }
                        }
                }

                needsWait = !_states.value.containsKey(nodeId)
            }
        }

        val shouldWaitNow = needsWait && initialSyncAttempted.add(nodeId)

        if (shouldWaitNow) {
            Log.d(TAG, "[$nodeId] Aguardando sincronização inicial do SDK...")
            try {
                withTimeout(5000) {
                    _states.first { it.containsKey(nodeId) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "[$nodeId] Timeout na sincronização inicial. Nenhum estado recebido ainda.", e)
            }
        }
    }

    private fun isUsefulPnplConfig(config: PnPLConfig?): Boolean {
        val deviceStatus = config?.deviceStatus?.value ?: return false
        return deviceStatus.components.isNotEmpty()
    }

    private fun manualExtractPnPL(
        nodeId: String,
        rawData: ByteArray
    ): PnPLConfig? {
        if (rawData.isEmpty()) return null

        val header = rawData[0].toInt() and 0xFF
        val payloadStartIndex = when (header) {
            0x00, 0x20, 0x80 -> 3
            0x40 -> 1
            else -> 1
        }

        Log.v(TAG, "[$nodeId] manualExtractPnPL header=0x${header.toString(16)} size=${rawData.size}")

        val isStart = header == 0x00 || header == 0x20
        val isMiddle = header == 0x40
        val isEnd = header == 0x80

        val buffer = nodeBuffers.getOrPut(nodeId) { ByteArrayOutputStream() }

        if (isStart) {
            buffer.reset()
        }

        if (rawData.size > payloadStartIndex) {
            buffer.write(rawData, payloadStartIndex, rawData.size - payloadStartIndex)
        }

        if (!isStart && !isMiddle && !isEnd) {
            return null
        }

        if (!isEnd) {
            return null
        }

        val rawString = buffer.toString(Charsets.UTF_8.name())
            .replace("\u0000", "")
            .trim()

        val jsonStart = rawString.indexOf('{')
        if (jsonStart < 0) {
            return null
        }

        val jsonString = rawString.substring(jsonStart)

        return try {
            Log.d(TAG, "[$nodeId] JSON reassemblado (${jsonString.length} chars)")
            Log.d(TAG, "[$nodeId] *** BEGIN REASSEMBLED JSON ***")
            Log.d(TAG, jsonString)
            Log.d(TAG, "[$nodeId] *** END REASSEMBLED JSON ***")

            val jsonElement = jsonHandler.parseToJsonElement(jsonString)
            val rootObj = jsonElement.jsonObject

            val devices = rootObj["devices"]
            val firstDevice = devices
                ?.let { jsonHandler.parseToJsonElement(it.toString()) }
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?: return null

            val boardId = firstDevice["board_id"]?.jsonPrimitive?.intOrNull
            val fwId = firstDevice["fw_id"]?.jsonPrimitive?.intOrNull
            val serial = firstDevice["sn"]?.jsonPrimitive?.contentOrNull
            val componentsJson = firstDevice["components"]
                ?.let { jsonHandler.parseToJsonElement(it.toString()) }
                ?.jsonArray
                ?.mapNotNull { it as? JsonObject }
                ?: emptyList()

            val device = PnPLDevice(
                boardId = boardId,
                fwId = fwId,
                serialNumber = serial,
                pnplBleResponses = null,
                components = componentsJson
            )

            PnPLConfig(
                deviceStatus = FeatureField(
                    name = "DeviceStatus",
                    value = device
                ),
                setCommandResponse = FeatureField(
                    name = "SetCommandResponse",
                    value = null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "[$nodeId] manualExtractPnPL falhou", e)
            null
        } finally {
            buffer.reset()
        }
    }

    private fun handleStatusUpdate(nodeId: String, data: PnPLConfig) {
        Log.i(TAG, "[$nodeId] RAW PnPLConfig: $data")

        val deviceStatus = data.deviceStatus.value
        if (deviceStatus == null) {
            Log.e(TAG, "[$nodeId] PnPLConfig sem deviceStatus")
            data.setCommandResponse.value?.let {
                Log.e(TAG, "[$nodeId] Resposta de comando recebida: $it")
            }
            return
        }

        val components = deviceStatus.components
        var sdMounted: Boolean? = null
        var isLogging: Boolean? = null
        var currentFileName: String? = null
        var bestLogComponent: String? = null
        var bestControllerComponent: String? = null

        fun extractFromObject(obj: JsonObject, componentName: String?) {
            var foundInThis = false

            val sdValPrim = obj[SD_JSON_KEY]?.jsonPrimitive
            if (sdValPrim != null) {
                sdMounted = sdValPrim.booleanOrNull ?: sdValPrim.intOrNull?.let { it == 1 }
                foundInThis = true
                Log.v(TAG, "[$nodeId] SD_MOUNTED em $componentName: $sdMounted (raw=${sdValPrim.content})")
            }

            val fileNamePrim = obj[FILE_NAME_JSON_KEY]?.jsonPrimitive
            if (fileNamePrim != null) {
                currentFileName = fileNamePrim.content
                foundInThis = true
                Log.v(TAG, "[$nodeId] FILE_NAME em $componentName: $currentFileName")
            }

            val logValPrim = obj[LOG_STATUS_JSON_KEY]?.jsonPrimitive
                ?: obj["is_logging"]?.jsonPrimitive
                ?: obj["status"]?.jsonPrimitive

            if (logValPrim != null) {
                val boolVal = logValPrim.booleanOrNull ?: logValPrim.intOrNull?.let { it == 1 }
                val content = logValPrim.content.lowercase(Locale.ROOT)

                isLogging = when {
                    boolVal != null -> boolVal
                    content.contains("started") -> true
                    content.contains("logging") -> true
                    content.contains("recording") -> true
                    content.contains("running") -> true
                    content.contains("acquiring") -> true
                    content.contains("ready to start") -> false
                    content.contains("ready") -> false
                    content.contains("stopped") -> false
                    content.contains("idle") -> false
                    else -> isLogging
                }

                foundInThis = true
                Log.v(TAG, "[$nodeId] LOG_STATUS em $componentName: $isLogging (raw=${logValPrim.content})")
            }

            val runValPrim = obj["is_active"]?.jsonPrimitive
                ?: obj["enabled"]?.jsonPrimitive
                ?: obj["running"]?.jsonPrimitive
                ?: obj["is_running"]?.jsonPrimitive

            if (runValPrim != null) {
                val boolVal = runValPrim.booleanOrNull ?: runValPrim.intOrNull?.let { it == 1 }
                if (boolVal != null) {
                    isLogging = boolVal
                    foundInThis = true
                    Log.v(TAG, "[$nodeId] RUN FLAG em $componentName: $isLogging (raw=${runValPrim.content})")
                }
            }

            if (
                componentName != null &&
                componentName != "root" &&
                (
                        componentName == SD_LOG_COMPONENT ||
                                componentName.contains("sd_log", ignoreCase = true)
                        )
            ) {
                bestLogComponent = componentName
            }

            if (
                componentName != null &&
                componentName != "root" &&
                (
                        componentName == LOG_CONTROLLER_COMPONENT ||
                                componentName.contains("log_controller", ignoreCase = true)
                        )
            ) {
                bestControllerComponent = componentName
            }

            if (foundInThis) {
                Log.v(TAG, "[$nodeId] Estado parcial de $componentName processado")
            }
        }

        components.forEach { compMap ->
            val rootObj = compMap as? JsonObject ?: return@forEach

            extractFromObject(rootObj, "root")

            rootObj.forEach { (componentName, componentValue) ->
                if (componentValue is JsonObject) {
                    extractFromObject(componentValue, componentName)

                    componentValue.forEach { (_, nestedValue) ->
                        if (nestedValue is JsonObject) {
                            extractFromObject(nestedValue, componentName)
                        }
                    }
                }
            }
        }

        bestLogComponent?.let {
            if (nodeComponentNames[nodeId] != it) {
                Log.i(TAG, "[$nodeId] Definindo componente de log prioritário: $it")
                nodeComponentNames[nodeId] = it
            }
        }

        bestControllerComponent?.let {
            if (nodeControllerNames[nodeId] != it) {
                Log.i(TAG, "[$nodeId] Definindo componente de controller prioritário: $it")
                nodeControllerNames[nodeId] = it
            }
        }

        currentFileName?.let {
            if (nodeFileNames[nodeId] != it) {
                Log.i(TAG, "[$nodeId] Flow/ficheiro atual reportado pelo device: $it")
                nodeFileNames[nodeId] = it
            }
        }

        val prevState = _states.value[nodeId]
        val mergedState = SdLogState(
            sdMounted = (sdMounted ?: prevState?.sdMounted) == true,
            isLogging = (isLogging ?: prevState?.isLogging) == true,
            fileName = currentFileName ?: prevState?.fileName,
            logComponent = bestLogComponent ?: prevState?.logComponent ?: nodeComponentNames[nodeId],
            controllerComponent = bestControllerComponent ?: prevState?.controllerComponent ?: nodeControllerNames[nodeId]
        )

        if (prevState != mergedState) {
            _states.update { current ->
                current.toMutableMap().apply {
                    this[nodeId] = mergedState
                }
            }

            Log.i(
                TAG,
                "[$nodeId] NOVO ESTADO: " +
                        "SD=${mergedState.sdMounted}, " +
                        "LOG=${mergedState.isLogging}, " +
                        "FILE=${mergedState.fileName}, " +
                        "LOG_COMP=${mergedState.logComponent}, " +
                        "CTRL_COMP=${mergedState.controllerComponent}"
            )
        } else {
            Log.v(
                TAG,
                "[$nodeId] Estado inalterado: " +
                        "SD=${mergedState.sdMounted}, " +
                        "LOG=${mergedState.isLogging}, " +
                        "FILE=${mergedState.fileName}, " +
                        "LOG_COMP=${mergedState.logComponent}, " +
                        "CTRL_COMP=${mergedState.controllerComponent}"
            )
        }
    }

    private suspend fun sendCommand(nodeId: String, cmd: PnPLCmd) {
        ensurePnplReady(nodeId)

        val pnplFeature = pnplFeatures[nodeId]
            ?: throw IllegalStateException("PnPL feature not initialized for $nodeId")

        commandMutex.withLock {
            Log.v(TAG, "[$nodeId] Escrevendo comando PnPL: ${cmd.component}*${cmd.command ?: "ALL"}")
            blueManager.writeFeatureCommand(
                nodeId,
                PnPLCommand(pnplFeature, cmd),
                0L
            )
        }
    }

    data class SdLogState(
        val sdMounted: Boolean = false,
        val isLogging: Boolean = false,
        val fileName: String? = null,
        val logComponent: String? = null,
        val controllerComponent: String? = null
    )

    companion object {
        private const val TAG = "OfficialSdLogEngine"
        private const val LOG_STATUS_JSON_KEY = "log_status"
        private const val SD_JSON_KEY = "sd_mounted"
        private const val FILE_NAME_JSON_KEY = "file_name"
        private const val SD_LOG_COMPONENT = "sd_log"
        private const val LOG_CONTROLLER_COMPONENT = "log_controller"
    }
}