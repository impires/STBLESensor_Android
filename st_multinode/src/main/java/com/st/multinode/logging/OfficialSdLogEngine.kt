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

    suspend fun prepareForLogging(nodeId: String): Result<Unit> {
        return runCatching {
            ensureDemoStarted(nodeId)
            delay(1000) // Aguardar estabilidade após ativar notificações

            Log.d(TAG, "[$nodeId] Identificando componente de log...")
            // Tentativa segura: usar comandos padrão do SDK para obter status
            sendCommand(nodeId, PnPLCmd.ALL)
            delay(1500) 
            sendCommand(nodeId, PnPLCmd.LOG_CONTROLLER)
            delay(1000)
            
            Log.d(TAG, "[$nodeId] Aguardando montagem do SD...")
            try {
                withTimeout(25000) {
                    _states.filter { 
                        val state = it[nodeId]
                        state?.sdMounted == true 
                    }.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "[$nodeId] TIMEOUT aguardando SD (25s). Estado atual: ${_states.value[nodeId]}")
                throw e
            }

            val compName = nodeComponentNames[nodeId] ?: LOG_CONTROLLER_JSON_KEY
            Log.d(TAG, "[$nodeId] Usando componente: $compName. Sincronizando relógio...")
            
            val datetime = SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.ROOT).format(Date())
            sendCommand(nodeId, PnPLCmd(component = compName, command = "set_time", fields = mapOf("datetime" to datetime)))
            delay(1000)

            Log.d(TAG, "[$nodeId] Configurando nome do ficheiro...")
            sendCommand(nodeId, PnPLCmd(component = compName, command = "set_filename", fields = mapOf("name" to "L")))
            delay(1000)

            Log.i(TAG, "[$nodeId] PREPARAÇÃO CONCLUÍDA.")
        }
    }

    suspend fun triggerLogging(nodeId: String): Result<Unit> {
        return runCatching {
            val compName = nodeComponentNames[nodeId] ?: LOG_CONTROLLER_JSON_KEY
            Log.d(TAG, "[$nodeId] DISPARANDO START (interface=2)...")
            sendCommand(nodeId, PnPLCmd(component = compName, command = "start_log", fields = mapOf("interface" to 2)))

            // Refresh forçado para confirmar estado
            delay(1000)
            sendCommand(nodeId, PnPLCmd(compName, "get_status"))

            try {
                withTimeout(25000) {
                    _states.filter { it[nodeId]?.isLogging == true }.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "[$nodeId] TIMEOUT aguardando LOG (25s). Estado atual: ${_states.value[nodeId]}")
                throw e
            }
            Log.i(TAG, "[$nodeId] GRAVAÇÃO EM CURSO.")
        }
    }

    suspend fun stop(nodeId: String): Result<Unit> {
        return runCatching {
            ensureDemoStarted(nodeId)
            val compName = nodeComponentNames[nodeId] ?: LOG_CONTROLLER_JSON_KEY
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
            if (observeFeatureJobs[nodeId]?.isActive == true && pnplFeatures[nodeId] != null) return@withLock pnplFeatures[nodeId]

            val features = blueManager.nodeFeatures(nodeId = nodeId)
            val feature = features.filterIsInstance<PnPL>().firstOrNull()
            
            if (feature != null) {
                // Hack para habilitar notificações (isDataNotifyFeature = true)
                runCatching {
                    val field = Feature::class.java.getDeclaredField("isDataNotifyFeature")
                    field.isAccessible = true
                    field.set(feature, true)
                    Log.d(TAG, "[$nodeId] isDataNotifyFeature forçado para true via reflexão")
                }.onFailure { Log.e(TAG, "[$nodeId] Falha ao setar isDataNotifyFeature", it) }

                feature.setMaxPayLoadSize(240)
                pnplFeatures[nodeId] = feature
                Log.d(TAG, "[$nodeId] PnPL Feature selecionada: ${feature.name}")
            }
            feature
        } ?: run {
            Log.e(TAG, "[$nodeId] Nenhuma feature PnPL encontrada!")
            return
        }

        if (observeFeatureJobs[nodeId]?.isActive != true) {
            Log.d(TAG, "[$nodeId] Iniciando observação PnPL...")
            observeFeatureJobs[nodeId] = scope.launch {
                blueManager.getFeatureUpdates(nodeId, listOf(pnplFeature), true)
                    .catch { e -> Log.e(TAG, "[$nodeId] Erro no stream PnPL", e) }
                    .collect { update ->
                        Log.d(TAG, "[$nodeId] Atualização recebida: ${update.featureName}")
                        
                        val config = update.data as? PnPLConfig
                        // Se o SDK falhou no parse (ex: devido ao bug do dropLast ou header STL2)
                        if (config?.deviceStatus?.value == null && config?.setCommandResponse?.value == null) {
                            Log.w(TAG, "[$nodeId] SDK falhou no parse. Tentando extração manual...")
                            manualExtractPnPL(nodeId, update.rawData)?.let { handleStatusUpdate(nodeId, it) }
                        } else {
                            handleStatusUpdate(nodeId, config)
                        }
                    }
            }
        }
    }

    private val jsonHandler = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        allowSpecialFloatingPointValues = true
    }

    private fun manualExtractPnPL(nodeId: String, rawData: ByteArray): PnPLConfig? {
        if (rawData.isEmpty()) return null
        val header = rawData[0].toInt() and 0xFF
        
        val buffer = nodeBuffers.getOrPut(nodeId) { ByteArrayOutputStream() }

        // STL2 headers: 0x00/0x20 (Start), 0x40 (Middle), 0x80 (End).
        // 0x10 também é usado para Start com length de 4 bytes em algumas variantes.
        val isStart = (header == 0x00 || header == 0x20 || header == 0x10)
        val isEnd = (header == 0x80 || header == 0x20)

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
        
        // Limpeza agressiva: o JSON deve começar com '{'
        val firstBrace = rawString.indexOf('{')
        if (firstBrace == -1) return null
        
        val jsonString = rawString.substring(firstBrace).trim { it <= ' ' || it == '\u0000' }
        Log.d(TAG, "[$nodeId] Extração manual Completa (brace=$firstBrace, len=${jsonString.length}): $jsonString")

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
        val deviceStatus = data.deviceStatus.value
        if (deviceStatus == null) {
            Log.d(TAG, "[$nodeId] Status update sem DeviceStatus (pode ser resposta de comando)")
            data.setCommandResponse.value?.let { 
                Log.d(TAG, "[$nodeId] Resposta de comando recebida: $it")
            }
            return
        }
        
        val components = deviceStatus.components
        var found = false
        var sdMounted: Boolean? = null
        var isLogging: Boolean? = null

        fun extractFromObject(obj: JsonObject, componentName: String?) {
            // Captura nome do componente a partir de get_status ou de chaves de resposta
            obj["get_status"]?.jsonPrimitive?.contentOrNull?.let {
                if (it != "all") {
                    nodeComponentNames[nodeId] = it
                    Log.d(TAG, "[$nodeId] Componente identificado via 'get_status': $it")
                }
            }

            // SD MOUNTED: Suporta Boolean, Int(1) ou String ("ready", "started", "mounted")
            val sdValPrim = obj[SD_JSON_KEY]?.jsonPrimitive
            if (sdValPrim != null) {
                sdMounted = sdValPrim.booleanOrNull ?: (sdValPrim.intOrNull == 1)
                if (sdMounted == false) {
                    val content = sdValPrim.content.lowercase()
                    if (content.contains("ready") || content.contains("started") || content.contains("mounted")) {
                        sdMounted = true
                    }
                }
                found = true
                Log.d(TAG, "[$nodeId] SD_MOUNTED encontrado (comp=$componentName): $sdMounted (raw=${sdValPrim.content})")
            }
            
            // LOG STATUS: Suporta Boolean, Int(1) ou String ("started", "logging")
            val logValPrim = obj[LOG_STATUS_JSON_KEY]?.jsonPrimitive
                ?: obj["is_logging"]?.jsonPrimitive
                ?: obj["status"]?.jsonPrimitive

            if (logValPrim != null) {
                isLogging = logValPrim.booleanOrNull ?: (logValPrim.intOrNull == 1)
                if (isLogging == false) {
                    val content = logValPrim.content.lowercase()
                    if (content.contains("started") || content.contains("logging")) {
                        isLogging = true
                    }
                    // Em alguns firmwares, "Log ready to start" também indica que o SD está ok
                    if (content.contains("ready") || content.contains("started")) {
                        sdMounted = sdMounted ?: true
                    }
                }
                found = true
                Log.d(TAG, "[$nodeId] LOG_STATUS encontrado (comp=$componentName): $isLogging (raw=${logValPrim.content})")
            }
            
            if (found && componentName != null && componentName != "root") {
                // Prioridade: se o componente contém "log", é o que queremos.
                // Se o atual já contém "log", não sobrescrevemos com um genérico.
                val currentComp = nodeComponentNames[nodeId]
                if (currentComp == null || componentName.contains("log", ignoreCase = true)) {
                    nodeComponentNames[nodeId] = componentName
                    Log.d(TAG, "[$nodeId] Componente atualizado: $componentName")
                }
            }
        }

        components.forEach { compMap ->
            // Tenta extrair diretamente do objeto (caso as chaves não estejam aninhadas)
            extractFromObject(compMap, "root")
            
            // Tenta extrair de sub-objetos (caso aninhado por nome de componente)
            compMap.entries.forEach { (name, element) ->
                (element as? JsonObject)?.let { extractFromObject(it, name) }
            }
        }

        if (found) {
            _states.update { current ->
                val old = current[nodeId]
                val newSd = sdMounted ?: old?.sdMounted ?: false
                val newLog = isLogging ?: old?.isLogging ?: false

                if (old?.sdMounted != newSd || old?.isLogging != newLog) {
                    Log.i(TAG, "[$nodeId] NOVO ESTADO: SD=$newSd, LOG=$newLog (Componente: ${nodeComponentNames[nodeId]})")
                    current + (nodeId to SdLogState(sdMounted = newSd, isLogging = newLog))
                } else {
                    current
                }
            }
        } else {
            Log.d(TAG, "[$nodeId] Nenhuma chave relevante encontrada nos componentes: ${deviceStatus.components}")
        }
    }

    private suspend fun sendCommand(nodeId: String, cmd: PnPLCmd) {
        val pnplFeature = pnplFeatures[nodeId] ?: return
        commandMutex.withLock {
            blueManager.writeFeatureCommand(nodeId, PnPLCommand(pnplFeature, cmd), 0L)
            delay(250) // Intervalo para não saturar o rádio/firmware
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
