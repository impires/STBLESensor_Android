package com.st.multinode.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.st.multinode.data.MultiNodeRepository
import com.st.multinode.logging.OfficialSdLogEngine
import com.st.multinode.logging.SdFlowStarter
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

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
            buildNotification("ST BLE Sensor", "Ready for multi-device acquisition")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOGGING -> {
                val nodeIds = intent.getStringArrayListExtra(EXTRA_NODE_IDS).orEmpty()
                val flowFileName = intent.getStringExtra(EXTRA_FLOW_FILE_NAME).orEmpty()
                    .ifBlank { "defaultAppPro.json" }
                val enableServer = intent.getBooleanExtra(EXTRA_ENABLE_SERVER, false)
                val maxPayloadSize = intent.getIntExtra(EXTRA_MAX_PAYLOAD_SIZE, 150)
                val maxConnectionRetries = intent.getIntExtra(EXTRA_MAX_CONNECTION_RETRIES, 3)

                Log.d(TAG, "Flow recebido: $flowFileName")

                startNodesInBatch(
                    nodeIds = nodeIds,
                    flowFileName = flowFileName,
                    enableServer = enableServer,
                    maxPayloadSize = maxPayloadSize,
                    maxConnectionRetries = maxConnectionRetries
                )
            }

            ACTION_STOP_LOGGING -> {
                val nodeIds = intent.getStringArrayListExtra(EXTRA_NODE_IDS).orEmpty()
                serviceScope.launch { nodeIds.forEach { stopManagedLogging(it) } }
            }

            ACTION_STOP_ALL -> {
                serviceScope.launch { stopAllManagedLogging() }
            }
        }
        return START_STICKY
    }

    private fun startNodesInBatch(
        nodeIds: List<String>,
        flowFileName: String,
        enableServer: Boolean,
        maxPayloadSize: Int,
        maxConnectionRetries: Int
    ) {
        val batchJob = serviceScope.launch {
            Log.d(TAG, "Iniciando batch para ${nodeIds.size} nós")

            val preparedNodes = mutableListOf<String>()
            if (nodeIds.isNotEmpty()) {
                acquireWakeLockIfNeeded()
            }

            nodeIds.forEach { nodeId ->
                if (activeLoggingNodes.contains(nodeId)) return@forEach

                updateNotification()
                val success = prepareSingleNode(
                    nodeId = nodeId,
                    flowFileName = flowFileName,
                    enableServer = enableServer,
                    maxPayloadSize = maxPayloadSize,
                    maxConnectionRetries = maxConnectionRetries
                )
                if (success) {
                    preparedNodes.add(nodeId)
                }
            }

            if (preparedNodes.isEmpty()) {
                Log.e(TAG, "Nenhum nó pronto para iniciar flow. Abortando aquisição.")
                updateNotification()
                maybeStopSelf()
                return@launch
            }

            Log.d(TAG, "Disparando start do flow para ${preparedNodes.size} nós com stagger")
            preparedNodes.forEachIndexed { index, nodeId ->
                launch {
                    delay(index * 2000L)

                    val result = sdFlowStarter.startFlow(nodeId, flowFileName)

                    if (result.isSuccess) {
                        val started = waitForLoggingStart(nodeId)

                        if (started) {
                            activeLoggingNodes.add(nodeId)
                            repository.markLogging(nodeId, true)
                            acquireWakeLockIfNeeded()
                        } else {
                            Log.e(TAG, "[$nodeId] Flow não entrou em RUN")
                            repository.markError(nodeId, "Flow não iniciou")
                        }
                    } else {
                        Log.e(TAG, "[$nodeId] Falha no startFlow", result.exceptionOrNull())
                        repository.markError(
                            nodeId,
                            result.exceptionOrNull()?.message ?: "Falha no startFlow"
                        )
                    }

                    updateNotification()
                }
            }
        }

        nodeIds.forEach { nodeId ->
            nodeJobs[nodeId] = batchJob
        }
    }

    private suspend fun waitForLoggingStart(nodeId: String): Boolean {
        return try {
            withTimeout(30000) {
                val pollingJob = launch {
                    while (true) {
                        delay(10000)
                        Log.d(TAG, "[$nodeId] Ainda aguardando logging... solicitando status novamente.")
                        officialSdLogEngine.requestStatus(nodeId)
                    }
                }
                try {
                    officialSdLogEngine.states
                        .filter { it[nodeId]?.isLogging == true }
                        .first()
                    true
                } finally {
                    pollingJob.cancel()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$nodeId] Timeout à espera de logging", e)
            false
        }
    }

    private suspend fun prepareSingleNode(
        nodeId: String,
        flowFileName: String,
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
                return@withPermit false
            }

            delay(1000)

            val prepResult = sdFlowStarter.prepareFlow(nodeId, flowFileName)
            if (prepResult.isFailure) {
                Log.e(TAG, "[$nodeId] prepareFlow falhou", prepResult.exceptionOrNull())
                repository.markError(
                    nodeId,
                    prepResult.exceptionOrNull()?.message ?: "Flow prepare failed"
                )
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
        nodeJobs.remove(nodeId)?.cancelAndJoin()

        runCatching { sdFlowStarter.stopFlow(nodeId) }
        runCatching { officialSdLogEngine.stop(nodeId) }

        repository.markLogging(nodeId, false)
        activeLoggingNodes.remove(nodeId)

        officialSdLogEngine.release(nodeId)
        repository.disconnect(nodeId)

        releaseWakeLock(force = false)
        updateNotification()
        maybeStopSelf()
    }

    private suspend fun stopAllManagedLogging() {
        activeLoggingNodes.toList().forEach { stopManagedLogging(it) }
    }

    private fun maybeStopSelf() {
        if (activeLoggingNodes.isEmpty() && nodeJobs.isEmpty()) stopSelf()
    }

    private fun acquireWakeLockIfNeeded() {
        if (wakeLock?.isHeld == true) return
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:MultiNodeAcquisition"
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock(force: Boolean) {
        if (force || activeLoggingNodes.isEmpty()) {
            runCatching {
                if (wakeLock?.isHeld == true) wakeLock?.release()
            }
            wakeLock = null
        }
    }

    private fun updateNotification() {
        val content = when {
            activeLoggingNodes.isNotEmpty() -> "Logging em ${activeLoggingNodes.size} nó(s)"
            else -> "A preparar/parado"
        }
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification("ST BLE Sensor", content)
        )
    }

    private fun buildNotification(title: String, text: String) =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MultiNode Acquisition",
                NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(channel)
        }
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