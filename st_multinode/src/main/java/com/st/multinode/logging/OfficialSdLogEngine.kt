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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
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
    private val nodeBuffers = ConcurrentHashMap<String, ByteArrayOutputStream>()

    // Mapeia o nome correto do componente para cada nó (log_controller ou sdlog)
    private val nodeComponentNames = ConcurrentHashMap<String, String>()
    
    private val pnpLock = Mutex()
    private val commandMutex = Mutex()

    private val _states = MutableStateFlow<Map<String, SdLogState>>(emptyMap())

    suspend fun prepareForLogging(nodeId: String): Result<Unit> = runCatching {
        delay(500)

        Log.d(TAG, "[$nodeId] Identificando componentes (ALL)...")
        sendCommand(nodeId, PnPLCmd.ALL)

        try {
            withTimeout(15000) {
                while (nodeComponentNames[nodeId] == null) {
                    delay(500)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[$nodeId] Falha ao identificar componente de log via ALL. Usando fallback...")
        }

        val compName = nodeComponentNames[nodeId] ?: "sd_log"

        Log.d(TAG, "[$nodeId] Solicitando estado inicial no componente $compName...")
        sendCommand(nodeId, PnPLCmd(compName, "get_status"))

        try {
            withTimeout(8000) {
                _states.filter { it[nodeId] != null }.first()
            }
        } catch (e: Exception) {
            Log.w(TAG, "[$nodeId] Nenhum estado inicial recebido após get_status em $compName. Prosseguindo mesmo assim.")
        }

        val state = _states.value[nodeId]
        Log.d(TAG, "[$nodeId] Estado inicial disponível: $state")

        delay(1000)

        Log.d(TAG, "[$nodeId] Sincronizando relógio (set_time)...")
        val datetimeShort = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        sendCommand(
            nodeId,
            PnPLCmd(
                component = compName,
                command = "set_time",
                fields = mapOf("datetime" to datetimeShort)
            )
        )
        delay(1500)

        Log.d(TAG, "[$nodeId] Configurando acquisition_info via Property...")
        val infoCmd = PnPLCmd(
            component = compName,
            command = "acquisition_info",
            fields = mapOf(
                "name" to "M_$datetimeShort",
                "description" to "MN",
                "interface" to 2
            )
        )
        sendCommand(nodeId, infoCmd)
        delay(2000)

        Log.i(TAG, "[$nodeId] PREPARAÇÃO CONCLUÍDA. Estado: ${_states.value[nodeId]}")
    }

    private fun getLogComponentName(nodeId: String): String {
        return nodeComponentNames[nodeId] ?: "sd_log"
    }

    suspend fun triggerLogging(nodeId: String): Result<Unit> = runCatching {
        val compName = getLogComponentName(nodeId)
        val state = _states.value[nodeId]

        if (state == null) {
            Log.w(TAG, "[$nodeId] Estado de logging ainda indisponível. Prosseguindo com start_log mesmo assim.")
        }

        Log.v(TAG, "[$nodeId] Escrevendo comando PnPL: start_log")
        sendCommand(
            nodeId,
            PnPLCmd(
                component = compName,
                command = "start_log",
                fields = mapOf("interface" to 2)
            )
        )

        delay(5000)

        Log.v(TAG, "[$nodeId] Escrevendo comando PnPL: get_status")
        sendCommand(nodeId, PnPLCmd(compName, "get_status"))

        try {
            withTimeout(30000) {
                _states.filter { it[nodeId]?.isLogging == true }.first()
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "[$nodeId] start_log enviado, mas logging não entrou em RUN. Estado final: ${_states.value[nodeId]}",
                e
            )
            throw e
        }

        Log.i(TAG, "[$nodeId] Logging iniciado com sucesso. Estado: ${_states.value[nodeId]}")
    }

    suspend fun stop(nodeId: String): Result<Unit> {
        return runCatching {
            ensureDemoStarted(nodeId)
            val compName = getLogComponentName(nodeId)
            Log.d(TAG, "[$nodeId] Parando gravação (interface=2)...")
            sendCommand(nodeId, PnPLCmd(component = compName, command = "stop_log", fields = mapOf("interface" to 2)))
            
            delay(800)
            sendCommand(nodeId, PnPLCmd(compName, "get_status"))
            
            waitForLoggingState(nodeId = nodeId, expected = false)
            Unit
        }.onFailure {
            Log.e(TAG, "Falha ao parar log em $nodeId", it)
        }
    }

    suspend fun release(nodeId: String) {
        observeFeatureJobs.remove(nodeId)?.cancelAndJoin()
        pnplFeatures.remove(nodeId)?.let { pnpl ->
            runCatching { blueManager.disableFeatures(nodeId, listOf(pnpl)) }
        }
        nodeBuffers.remove(nodeId)
        nodeComponentNames.remove(nodeId)
        _states.update { it - nodeId }
    }

    private suspend fun ensureDemoStarted(nodeId: String) {
        val pnplFeature = pnpLock.withLock {
            val currentJob = observeFeatureJobs[nodeId]
            // Se o job existe e está ativo, e temos a feature, apenas retornamos
            if (currentJob?.isActive == true && pnplFeatures[nodeId] != null) {
                return@withLock pnplFeatures[nodeId]
            }

            // Caso contrário, precisamos (re)configurar
            val features = blueManager.nodeFeatures(nodeId = nodeId)
            val feature = features.filterIsInstance<PnPL>().firstOrNull()
            
            if (feature != null) {
                // Hack para habilitar notificações (isDataNotifyFeature = true)
                runCatching {
                    val field = Feature::class.java.getDeclaredField("isDataNotifyFeature")
                    field.isAccessible = true
                    field.set(feature, true)
                }

                feature.setMaxPayLoadSize(240)
                pnplFeatures[nodeId] = feature
            }
            feature
        } ?: run {
            Log.e(TAG, "[$nodeId] Nenhuma feature PnPL encontrada!")
            return
        }

        var needsWait = false
        pnpLock.withLock {
            if (observeFeatureJobs[nodeId]?.isActive != true) {
                Log.d(TAG, "[$nodeId] (Re)Iniciando observação PnPL...")
                val updateFlow = blueManager.getFeatureUpdates(nodeId, listOf(pnplFeature), true)
                
                observeFeatureJobs[nodeId] = scope.launch {
                    updateFlow
                        .catch { e -> Log.e(TAG, "[$nodeId] Erro no stream PnPL", e) }
                        .collect { update ->
                            val config = update.data as? PnPLConfig

                            if (config?.deviceStatus?.value == null && config?.setCommandResponse?.value == null) {
                                Log.e(TAG, "[$nodeId] RAW RX BYTES: ${update.rawData.joinToString(",")}")
                                Log.e(TAG, "[$nodeId] RAW RX STRING: ${update.rawData.toString(Charsets.UTF_8)}")

                                val extracted = manualExtractPnPL(nodeId, update.rawData)

                                if (extracted == null) {
                                    Log.e(TAG, "[$nodeId] manualExtractPnPL FALHOU")
                                } else {
                                    Log.d(TAG, "[$nodeId] manualExtractPnPL OK")
                                    handleStatusUpdate(nodeId, extracted)
                                }
                            } else {
                                handleStatusUpdate(nodeId, config)
                            }
                        }
                }
                needsWait = _states.value[nodeId] == null
            }
        }

        // Aguarda a primeira atualização chegar se ainda não temos estado (FORA DO LOCK)
        if (needsWait) {
            Log.d(TAG, "[$nodeId] Aguardando sincronização inicial do SDK...")
            try {
                withTimeout(10000) {
                    _states.filter { it.containsKey(nodeId) }.first()
                }
            } catch (e: Exception) {
                Log.w(TAG, "[$nodeId] Timeout na sincronização inicial.")
            }
        }
    }

    private val jsonHandler = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        allowSpecialFloatingPointValues = true
    }

    suspend fun sendRawCommand(nodeId: String, cmd: PnPLCmd) {
        sendCommand(nodeId, cmd)
    }

    private fun manualExtractPnPL(nodeId: String, rawData: ByteArray): PnPLConfig? {
        if (rawData.isEmpty()) return null
        val header = rawData[0].toInt() and 0xFF
        
        val buffer = nodeBuffers.getOrPut(nodeId) { ByteArrayOutputStream() }

        // STL2 headers: 0x00/0x20 (Start), 0x40 (Middle), 0x80 (End).
        // 0x10 também é usado para Start com length de 4 bytes em algumas variantes.
        val isStart = (header == 0x00 || header == 0x20 || header == 0x10)
        val isEnd = (header == 0x00 || header == 0x20 || header == 0x80)

        if (isStart) {
            buffer.reset()
        }

        val payload = when (header) {
            0x00, 0x20 -> if (rawData.size > 3) rawData.sliceArray(3 until rawData.size) else null
            0x10 -> if (rawData.size > 5) rawData.sliceArray(5 until rawData.size) else null
            0x40, 0x80 -> if (rawData.size > 1) rawData.sliceArray(1 until rawData.size) else null
            else -> rawData
        }

        payload?.let { buffer.write(it) }

        if (!isEnd) return null

        val fullPayload = buffer.toByteArray()
        val rawString = fullPayload.toString(Charsets.UTF_8)
        
        // Limpeza agressiva: o JSON deve começar com '{' e terminar com '}'
        val firstBrace = rawString.indexOf('{')
        val lastBrace = rawString.lastIndexOf('}')
        if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) return null
        
        val jsonString = rawString.substring(firstBrace, lastBrace + 1).trim()
        Log.d(TAG, "[$nodeId] Extração manual Completa (brace=$firstBrace, last=$lastBrace, len=${jsonString.length}): $jsonString")

        return try {
            val jsonElement = jsonHandler.parseToJsonElement(jsonString)
            if (jsonElement is JsonObject && jsonElement.containsKey("components")) {
                val device = jsonHandler.decodeFromJsonElement<PnPLDevice>(jsonElement)
                PnPLConfig(
                    deviceStatus = FeatureField(name = "DeviceStatus", value = device),
                    setCommandResponse = FeatureField(name = "SetCommandResponse", value = null)
                )
            } else if (jsonElement is JsonObject) {
                // Se for um update de componente ou resposta de comando formatada
                PnPLConfig(
                    deviceStatus = FeatureField(name = "DeviceStatus", value = PnPLDevice(null, null, null, null, listOf(jsonElement))),
                    setCommandResponse = FeatureField(name = "SetCommandResponse", value = null)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$nodeId] Erro ao parsear JSON reassemblado: ${e.message}")
            null
        }
    }

    private fun handleStatusUpdate(nodeId: String, data: PnPLConfig) {
        Log.i(TAG, "[$nodeId] RAW PnPLConfig: $data")

        val deviceStatus = data.deviceStatus.value
        if (deviceStatus == null) {
            Log.d(TAG, "[$nodeId] Status update sem DeviceStatus")
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

            // SD MOUNTED: confiar apenas no campo real sd_mounted
            val sdValPrim = obj[SD_JSON_KEY]?.jsonPrimitive
            if (sdValPrim != null) {
                sdMounted = sdValPrim.booleanOrNull ?: sdValPrim.intOrNull?.let { it == 1 }
                foundInThis = true
                Log.v(
                    TAG,
                    "[$nodeId] SD_MOUNTED STRICT em $componentName: $sdMounted (raw=${sdValPrim.content})"
                )
            }

            // LOG STATUS: aceitar campos dedicados e fallback em "status" apenas para isLogging
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

            // identificação explícita via resposta
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

        if (bestLogComponent != null) {
            val previous = nodeComponentNames[nodeId]
            if (previous == null || previous != bestLogComponent) {
                nodeComponentNames[nodeId] = bestLogComponent!!
                Log.i(TAG, "[$nodeId] Definindo componente de log prioritário: $bestLogComponent")
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
        val pnplFeature = pnplFeatures[nodeId] ?: return
        commandMutex.withLock {
            Log.v(TAG, "[$nodeId] Escrevendo comando PnPL: ${cmd.command}")
            blueManager.writeFeatureCommand(nodeId, PnPLCommand(pnplFeature, cmd), 0L)
            // Delay de 800ms é o "sweet spot" para evitar que o rádio ou o firmware engasguem 
            // durante operações pesadas como montagem de SD ou início de gravação.
            delay(800)
        }
    }

    private suspend fun waitForLoggingState(nodeId: String, expected: Boolean) {
        withTimeout(25000) {
            _states.map { it[nodeId]?.isLogging }.filter { it == expected }.first()
        }
    }

    data class SdLogState(val sdMounted: Boolean = false, val isLogging: Boolean = false)

    companion object {
        private const val TAG = "OfficialSdLogEngine"
        private const val LOG_STATUS_JSON_KEY = "log_status"
        private const val SD_JSON_KEY = "sd_mounted"
        private const val LOG_CONTROLLER_JSON_KEY = "log_controller"
    }
}
