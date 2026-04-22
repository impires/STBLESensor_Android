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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

@Singleton
class OfficialSdLogEngine @Inject constructor(
    private val blueManager: BlueManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val observeFeatureJobs = ConcurrentHashMap<String, Job>()
    private val pnplFeatures = ConcurrentHashMap<String, PnPL>()
    private val nodeBuffers = ConcurrentHashMap<String, ByteArrayOutputStream>()
    private val nodeComponentNames = ConcurrentHashMap<String, String>()

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
        sendCommand(nodeId, PnPLCmd(component = compName, command = "get_status"))
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

        _states.update { current ->
            current.toMutableMap().apply {
                remove(nodeId)
            }
        }
    }

    private fun getLogComponentName(nodeId: String): String {
        return nodeComponentNames[nodeId] ?: SD_LOG_COMPONENT
    }

    private suspend fun ensurePnplReady(nodeId: String) {
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

                observeFeatureJobs[nodeId] = scope.launch {
                    updateFlow
                        .catch { e ->
                            Log.e(TAG, "[$nodeId] Erro no stream PnPL", e)
                        }
                        .collect { update ->
                            Log.e(TAG, "[$nodeId] UPDATE RAW BYTES: ${update.rawData.joinToString(",")}")
                            Log.e(TAG, "[$nodeId] UPDATE RAW STRING: ${update.rawData.toString(Charsets.UTF_8)}")
                            Log.e(TAG, "[$nodeId] UPDATE DATA CLASS: ${update.data?.javaClass?.name}")

                            val config = update.data as? PnPLConfig

                            if (config?.deviceStatus?.value == null && config?.setCommandResponse?.value == null) {
                                val extracted = manualExtractPnPL(nodeId, update.rawData)

                                if (extracted == null) {
                                    Log.e(TAG, "[$nodeId] manualExtractPnPL FALHOU")
                                } else {
                                    Log.d(TAG, "[$nodeId] manualExtractPnPL OK")
                                    handleStatusUpdate(nodeId, extracted)
                                }
                            } else if (config != null) {
                                Log.e(TAG, "[$nodeId] PnPLConfig recebido")
                                handleStatusUpdate(nodeId, config)
                            }
                        }
                }

                needsWait = _states.value[nodeId] == null
            }
        }

        if (needsWait) {
            Log.d(TAG, "[$nodeId] Aguardando sincronização inicial do SDK...")
            try {
                withTimeout(5000) {
                    _states.first { it.containsKey(nodeId) }
                }
            } catch (_: Exception) {
                Log.w(TAG, "[$nodeId] Timeout na sincronização inicial. Prosseguindo mesmo assim.")
            }
        }
    }

    private fun manualExtractPnPL(nodeId: String, rawData: ByteArray): PnPLConfig? {
        if (rawData.size < 3) return null

        val header = rawData[0].toInt() and 0xFF
        val payloadStartIndex = 3

        Log.d(TAG, "[$nodeId] manualExtractPnPL header=0x${header.toString(16)} size=${rawData.size}")

        // 0x00 = pacote completo
        // 0x20 = pacote completo variante
        // 0x10 = início de pacote multi-frame
        // 0x80 = fim de pacote multi-frame
        val isStart = (header == 0x00 || header == 0x20 || header == 0x10)
        val isEnd = (header == 0x00 || header == 0x20 || header == 0x80)

        val buffer = nodeBuffers.getOrPut(nodeId) { ByteArrayOutputStream() }

        if (isStart) {
            buffer.reset()
        }

        if (rawData.size > payloadStartIndex) {
            buffer.write(rawData, payloadStartIndex, rawData.size - payloadStartIndex)
        }

        if (!isEnd) return null

        val rawString = buffer.toString(Charsets.UTF_8.name()).trim()
        val jsonStart = rawString.indexOf('{')
        if (jsonStart < 0) return null

        val jsonString = rawString.substring(jsonStart)

        return try {
            val jsonElement = jsonHandler.parseToJsonElement(jsonString)
            val device = jsonHandler.decodeFromJsonElement<PnPLDevice>(jsonElement)

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
        } catch (firstError: Exception) {
            try {
                val jsonElement = jsonHandler.parseToJsonElement(jsonString)

                PnPLConfig(
                    deviceStatus = FeatureField(
                        name = "DeviceStatus",
                        value = PnPLDevice(
                            null,
                            null,
                            null,
                            null,
                            listOf(jsonElement.jsonObject)
                        )
                    ),
                    setCommandResponse = FeatureField(
                        name = "SetCommandResponse",
                        value = null
                    )
                )
            } catch (secondError: Exception) {
                Log.e(TAG, "[$nodeId] manualExtractPnPL falhou", secondError)
                null
            }
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
        var bestLogComponent: String? = null

        fun extractFromObject(obj: JsonObject, componentName: String?) {
            var foundInThis = false

            val sdValPrim = obj[SD_JSON_KEY]?.jsonPrimitive
            if (sdValPrim != null) {
                sdMounted = sdValPrim.booleanOrNull ?: sdValPrim.intOrNull?.let { it == 1 }
                foundInThis = true
                Log.v(
                    TAG,
                    "[$nodeId] SD_MOUNTED STRICT em $componentName: $sdMounted (raw=${sdValPrim.content})"
                )
            }

            val logValPrim = obj[LOG_STATUS_JSON_KEY]?.jsonPrimitive
                ?: obj["is_logging"]?.jsonPrimitive
                ?: obj["status"]?.jsonPrimitive

            if (logValPrim != null) {
                val boolVal = logValPrim.booleanOrNull ?: logValPrim.intOrNull?.let { it == 1 }
                val content = logValPrim.content.lowercase()

                isLogging = when {
                    boolVal != null -> boolVal
                    content.contains("started") -> true
                    content.contains("logging") -> true
                    content.contains("recording") -> true
                    content.contains("running") -> true
                    content.contains("acquiring") -> true
                    content.contains("ready") -> false
                    content.contains("stopped") -> false
                    content.contains("idle") -> false
                    else -> isLogging
                }

                foundInThis = true
                Log.v(
                    TAG,
                    "[$nodeId] LOG_STATUS em $componentName: $isLogging (raw=${logValPrim.content})"
                )
            }

            if (foundInThis && componentName != null && componentName != "root") {
                bestLogComponent = componentName
            }

            obj["get_status"]?.jsonPrimitive?.contentOrNull?.let {
                if (it != "all" && it != "root" && nodeComponentNames[nodeId] == null) {
                    nodeComponentNames[nodeId] = it
                    Log.d(TAG, "[$nodeId] Componente inicial via get_status: $it")
                }
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

        val prevState = _states.value[nodeId]
        val mergedState = SdLogState(
            sdMounted = (sdMounted ?: prevState?.sdMounted) == true,
            isLogging = (isLogging ?: prevState?.isLogging) == true
        )

        if (prevState != mergedState) {
            _states.update { current ->
                current.toMutableMap().apply {
                    this[nodeId] = mergedState
                }
            }

            Log.i(
                TAG,
                "[$nodeId] NOVO ESTADO: SD=${mergedState.sdMounted}, LOG=${mergedState.isLogging} " +
                        "(Componente: ${nodeComponentNames[nodeId] ?: "desconhecido"})"
            )
        } else {
            Log.v(
                TAG,
                "[$nodeId] Estado inalterado: SD=${mergedState.sdMounted}, LOG=${mergedState.isLogging}"
            )
        }
    }

    private suspend fun sendCommand(nodeId: String, cmd: PnPLCmd) {
        ensurePnplReady(nodeId)

        val pnplFeature = pnplFeatures[nodeId]
            ?: throw IllegalStateException("PnPL feature not initialized for $nodeId")

        commandMutex.withLock {
            Log.v(TAG, "[$nodeId] Escrevendo comando PnPL: ${cmd.command ?: "ALL"}")
            blueManager.writeFeatureCommand(
                nodeId,
                PnPLCommand(pnplFeature, cmd),
                0L
            )
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
        private const val SD_LOG_COMPONENT = "sd_log"
    }
}