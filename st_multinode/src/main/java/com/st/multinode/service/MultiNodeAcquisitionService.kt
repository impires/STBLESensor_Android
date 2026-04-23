package com.st.multinode.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.st.multinode.data.MultiNodeRepository
import com.st.multinode.logging.OfficialSdLogEngine
import com.st.multinode.logging.SdFlowStarter
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@AndroidEntryPoint
class MultiNodeAcquisitionService : Service() {

    @Inject
    lateinit var repository: MultiNodeRepository

    @Inject
    lateinit var officialSdLogEngine: OfficialSdLogEngine

    @Inject
    lateinit var sdFlowStarter: SdFlowStarter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val nodeJobs = ConcurrentHashMap<String, Job>()
    private val activeLoggingNodes = ConcurrentHashMap.newKeySet<String>()
    private val prepareSemaphore = Semaphore(1)

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Ready for multi-device acquisition")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOGGING -> {
                val nodeIds = intent.getStringArrayListExtra(EXTRA_NODE_IDS).orEmpty()
                val flowLocation = intent.getStringExtra(EXTRA_FLOW_FILE_NAME).orEmpty().trim()

                if (flowLocation.isBlank()) {
                    Log.e(TAG, "Nenhum flow file/path/uri foi fornecido")
                    nodeIds.forEach { nodeId ->
                        repository.markError(nodeId, "Nenhum ficheiro de flow foi fornecido")
                        repository.markLogging(nodeId, false)
                    }
                    updateNotification()
                    maybeStopSelf()
                    return START_NOT_STICKY
                }

                val flowJson = runCatching {
                    readFlowText(flowLocation)
                }.onFailure { e ->
                    Log.e(TAG, "Falha a ler flow: $flowLocation", e)
                    nodeIds.forEach { nodeId ->
                        repository.markError(
                            nodeId,
                            "Falha a ler flow file: $flowLocation - ${e.message}"
                        )
                        repository.markLogging(nodeId, false)
                    }
                    updateNotification()
                    maybeStopSelf()
                }.getOrNull() ?: return START_NOT_STICKY

                val enableServer = intent.getBooleanExtra(EXTRA_ENABLE_SERVER, false)
                val maxPayloadSize = intent.getIntExtra(EXTRA_MAX_PAYLOAD_SIZE, 150)
                val maxConnectionRetries = intent.getIntExtra(EXTRA_MAX_CONNECTION_RETRIES, 3)

                Log.d(TAG, "Flow recebido de: $flowLocation")
                Log.d(TAG, "Flow json size=${flowJson.length}")

                startNodesInBatch(
                    nodeIds = nodeIds,
                    flowJson = flowJson,
                    enableServer = enableServer,
                    maxPayloadSize = maxPayloadSize,
                    maxConnectionRetries = maxConnectionRetries
                )
            }

            ACTION_STOP_LOGGING -> {
                val nodeIds = intent.getStringArrayListExtra(EXTRA_NODE_IDS).orEmpty()
                serviceScope.launch {
                    nodeIds.forEach { stopManagedLogging(it) }
                }
            }

            ACTION_STOP_ALL -> {
                serviceScope.launch {
                    stopAllManagedLogging()
                }
            }
        }

        return START_STICKY
    }

    private fun readFlowText(location: String): String {
        val trimmed = location.trim()

        if (trimmed.startsWith("content://")) {
            return contentResolver.openInputStream(Uri.parse(trimmed))
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Nao foi possivel abrir URI: $trimmed")
        }

        val candidates = mutableListOf<File>()

        if (trimmed.startsWith("/")) {
            candidates += File(trimmed)
        } else {
            candidates += File(trimmed)
            candidates += File("/sdcard/$trimmed")
            candidates += File("/storage/emulated/0/$trimmed")
            candidates += File("/storage/self/primary/$trimmed")
        }

        val existing = candidates.firstOrNull { it.exists() && it.isFile }
        if (existing != null) {
            Log.d(TAG, "Flow encontrado no storage: ${existing.absolutePath}")
            return existing.bufferedReader().use { it.readText() }
        }

        return runCatching {
            applicationContext.assets.open(trimmed)
                .bufferedReader()
                .use { it.readText() }
        }.getOrElse {
            error(
                "Flow nao encontrado. location=$trimmed, tentados=${
                    candidates.joinToString { f -> f.absolutePath }
                }, assets=$trimmed"
            )
        }
    }

    private fun startNodesInBatch(
        nodeIds: List<String>,
        flowJson: String,
        enableServer: Boolean,
        maxPayloadSize: Int,
        maxConnectionRetries: Int
    ) {
        if (nodeIds.isNotEmpty()) {
            acquireWakeLockIfNeeded()
        }

        Log.d(TAG, "Iniciando batch para ${nodeIds.size} nos")

        nodeIds.forEachIndexed { index, nodeId ->
            if (activeLoggingNodes.contains(nodeId)) {
                Log.d(TAG, "[$nodeId] Ja esta em logging ativo, a saltar")
                return@forEachIndexed
            }

            val nodeJob = serviceScope.launch {
                try {
                    updateNotification()

                    val prepared = prepareSingleNode(
                        nodeId = nodeId,
                        flowJson = flowJson,
                        enableServer = enableServer,
                        maxPayloadSize = maxPayloadSize,
                        maxConnectionRetries = maxConnectionRetries
                    )

                    if (!prepared) {
                        Log.e(TAG, "[$nodeId] Preparacao falhou")
                        return@launch
                    }

                    delay(index * 2000L)

                    Log.d(TAG, "[$nodeId] A iniciar startFlow")
                    val result = sdFlowStarter.startFlow(nodeId, flowJson)

                    if (result.isSuccess) {
                        delay(8000)

                        activeLoggingNodes.add(nodeId)
                        repository.markLogging(nodeId, true)
                        acquireWakeLockIfNeeded()

                        Log.d(TAG, "[$nodeId] Sequencia de start concluida; logging assumido ativo")
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e(TAG, "[$nodeId] Falha no startFlow", error)
                        repository.markError(
                            nodeId,
                            error?.message ?: "Falha no startFlow"
                        )
                        repository.markLogging(nodeId, false)
                    }

                    updateNotification()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "[$nodeId] Job cancelado durante start/acquisition", e)
                    repository.markLogging(nodeId, false)
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "[$nodeId] Erro inesperado no fluxo de aquisicao", e)
                    repository.markError(nodeId, e.message ?: "Erro inesperado")
                    repository.markLogging(nodeId, false)
                } finally {
                    nodeJobs.remove(nodeId)
                    updateNotification()
                    maybeStopSelf()
                }
            }

            nodeJobs[nodeId] = nodeJob
        }
    }

    private suspend fun prepareSingleNode(
        nodeId: String,
        flowJson: String,
        enableServer: Boolean,
        maxPayloadSize: Int,
        maxConnectionRetries: Int
    ): Boolean {
        return prepareSemaphore.withPermit {
            Log.d(TAG, "[$nodeId] Preparando...")

            val connResult = repository.connectAndAwaitReady(
                nodeId,
                maxConnectionRetries,
                maxPayloadSize,
                enableServer
            )

            if (connResult.isFailure) {
                repository.markError(nodeId, "Connection failed")
                repository.markLogging(nodeId, false)
                return@withPermit false
            }

            delay(2500)

            val prepResult = sdFlowStarter.prepareFlow(nodeId, flowJson)
            if (prepResult.isFailure) {
                Log.e(TAG, "[$nodeId] prepareFlow falhou", prepResult.exceptionOrNull())
                repository.markError(
                    nodeId,
                    prepResult.exceptionOrNull()?.message ?: "Flow prepare failed"
                )
                repository.markLogging(nodeId, false)
                return@withPermit false
            }

            true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock(force = true)
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun stopManagedLogging(nodeId: String) {
        runCatching {
            nodeJobs.remove(nodeId)?.cancelAndJoin()
        }.onFailure {
            Log.w(TAG, "[$nodeId] Falha ao cancelar job", it)
        }

        runCatching { sdFlowStarter.stopFlow(nodeId) }
            .onFailure { Log.w(TAG, "[$nodeId] stopFlow falhou", it) }

        runCatching { officialSdLogEngine.stop(nodeId) }
            .onFailure { Log.w(TAG, "[$nodeId] officialSdLogEngine.stop falhou", it) }

        repository.markLogging(nodeId, false)
        activeLoggingNodes.remove(nodeId)

        runCatching { officialSdLogEngine.release(nodeId) }
            .onFailure { Log.w(TAG, "[$nodeId] officialSdLogEngine.release falhou", it) }

        runCatching { repository.disconnect(nodeId) }
            .onFailure { Log.w(TAG, "[$nodeId] disconnect falhou", it) }

        releaseWakeLock(force = false)
        updateNotification()
        maybeStopSelf()
    }

    private suspend fun stopAllManagedLogging() {
        val nodes = activeLoggingNodes.toList() + nodeJobs.keys.toList()
        nodes.distinct().forEach { stopManagedLogging(it) }
    }

    private fun maybeStopSelf() {
        if (activeLoggingNodes.isEmpty() && nodeJobs.isEmpty()) {
            stopSelf()
        }
    }

    private fun acquireWakeLockIfNeeded() {
        if (wakeLock?.isHeld == true) return

        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:MultiNodeAcquisition"
        ).apply {
            setReferenceCounted(false)
            acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock(force: Boolean) {
        if (force || activeLoggingNodes.isEmpty()) {
            runCatching {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
            wakeLock = null
        }
    }

    private fun updateNotification() {
        val content = when {
            activeLoggingNodes.isNotEmpty() -> "Logging em ${activeLoggingNodes.size} no(s)"
            else -> "A preparar/parado"
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification(content)
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Erro ao enviar notificacao", e)
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ST BLE Sensor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    packageManager.getLaunchIntentForPackage(packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createNotificationChannel() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "MultiNode Acquisition",
            NotificationManager.IMPORTANCE_LOW
        )
        mgr.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "MultiNodeAcquisition"
        private const val NOTIFICATION_CHANNEL_ID = "multi_node_acquisition"
        private const val NOTIFICATION_ID = 42001

        const val ACTION_START_LOGGING = "com.st.bluems.multinode.action.START_LOGGING"
        const val ACTION_STOP_LOGGING = "com.st.bluems.multinode.action.STOP_LOGGING"
        const val ACTION_STOP_ALL = "com.st.bluems.multinode.action.STOP_ALL"

        const val EXTRA_NODE_IDS = "extra_node_ids"
        const val EXTRA_ENABLE_SERVER = "extra_enable_server"
        const val EXTRA_MAX_PAYLOAD_SIZE = "extra_max_payload_size"
        const val EXTRA_MAX_CONNECTION_RETRIES = "extra_max_connection_retries"
        const val EXTRA_FLOW_FILE_NAME = "extra_flow_file_name"
    }
}