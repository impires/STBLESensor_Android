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
import com.st.multinode.logging.StartLoggingUseCase
import com.st.multinode.logging.StopLoggingUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.joinAll

@AndroidEntryPoint
class MultiNodeAcquisitionService : Service() {

    @Inject
    lateinit var repository: MultiNodeRepository

    @Inject
    lateinit var officialSdLogEngine: OfficialSdLogEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val nodeJobs = ConcurrentHashMap<String, Job>()
    private val activeLoggingNodes = ConcurrentHashMap.newKeySet<String>()
    
    // Semáforo de 1 para garantir que a preparação física (SD/BLE) é feita sem colisões
    private val prepareSemaphore = Semaphore(1)

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("ST BLE Sensor", "Ready for multi-device acquisition"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOGGING -> {
                val nodeIds = intent.getStringArrayListExtra(EXTRA_NODE_IDS).orEmpty()
                val enableServer = intent.getBooleanExtra(EXTRA_ENABLE_SERVER, false)
                val maxPayloadSize = intent.getIntExtra(EXTRA_MAX_PAYLOAD_SIZE, 150)
                val maxConnectionRetries = intent.getIntExtra(EXTRA_MAX_CONNECTION_RETRIES, 3)

                startNodesInBatch(nodeIds, enableServer, maxPayloadSize, maxConnectionRetries)
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
        enableServer: Boolean,
        maxPayloadSize: Int,
        maxConnectionRetries: Int
    ) {
        val batchJob = serviceScope.launch {
            Log.d(TAG, "Iniciando batch para ${nodeIds.size} nós")
            
            // 1. Preparar cada nó sequencialmente
            val preparedNodes = mutableListOf<String>()
            if (nodeIds.isNotEmpty()) {
                acquireWakeLockIfNeeded()
            }
            nodeIds.forEach { nodeId ->
                if (activeLoggingNodes.contains(nodeId)) return@forEach
                
                updateNotification()
                val success = prepareSingleNode(nodeId, enableServer, maxPayloadSize, maxConnectionRetries)
                if (success) {
                    preparedNodes.add(nodeId)
                }
            }

            if (preparedNodes.isEmpty()) {
                Log.e(TAG, "Nenhum nó pronto para iniciar logging. Abortando aquisição.")
                updateNotification()
                maybeStopSelf()
                return@launch
            }

            // 2. Disparar todos com um pequeno escalonamento (stagger)
            Log.d(TAG, "Disparando start para ${preparedNodes.size} nós com stagger")
            preparedNodes.forEachIndexed { index, nodeId ->
                launch {
                    delay(index * 2000L) // Aumentado para 2s para dar tempo ao firmware de estabilizar o SD
                    val result = officialSdLogEngine.triggerLogging(nodeId)
                    if (result.isSuccess) {
                        activeLoggingNodes.add(nodeId)
                        repository.markLogging(nodeId, true)
                        acquireWakeLockIfNeeded()
                    } else {
                        repository.markError(nodeId, result.exceptionOrNull()?.message ?: "Trigger failed")
                    }
                    updateNotification()
                }
            }
            
            // Aguarda os triggers terminarem antes de talvez parar o serviço
            // (Note: we don't joinAll here because it's a batchJob that holds the service alive)
            // But we need to ensure the service stays alive if batchJob is still running.
            // batchJob is launched in serviceScope.
            
            //maybeStopSelf()
        }

        // Guardar o Job para permitir cancelamento se o utilizador carregar em "Stop"
        nodeIds.forEach { nodeId ->
            nodeJobs[nodeId] = batchJob
        }
    }

    private suspend fun prepareSingleNode(
        nodeId: String,
        enableServer: Boolean,
        maxPayloadSize: Int,
        maxConnectionRetries: Int
    ): Boolean {
        return prepareSemaphore.withPermit {
            Log.d(TAG, "[$nodeId] Preparando...")
            val connResult = repository.connectAndAwaitReady(nodeId, maxConnectionRetries, maxPayloadSize, enableServer)
            if (connResult.isFailure) {
                repository.markError(nodeId, "Connection failed")
                return@withPermit false
            }

            val sdResult = officialSdLogEngine.prepareForLogging(nodeId)
            if (sdResult.isFailure) {
                Log.e(TAG, "[$nodeId] prepareForLogging falhou", sdResult.exceptionOrNull())
                repository.markError(nodeId, sdResult.exceptionOrNull()?.message ?: "SD Prepare failed")
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
        officialSdLogEngine.stop(nodeId)
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
        // Usamos SCREEN_DIM_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP para tentar manter a tela/sistema ativos
        // conforme solicitado pelo usuário ("disable to lock screen")
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "$packageName:multi-node-active"
        ).apply { acquire(8 * 60 * 60 * 1000L) }
        Log.d(TAG, "WakeLock (SCREEN_DIM) adquirido.")
    }

    private fun releaseWakeLock(force: Boolean) {
        if ((force || activeLoggingNodes.isEmpty()) && wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
        }
    }

    private fun updateNotification() {
        val count = activeLoggingNodes.size
        val text = if (count > 0) "Logging on $count node(s)" else "Waiting for acquisition"
        notificationManager.notify(NOTIFICATION_ID, buildNotification("ST BLE Sensor", text))
    }

    private fun buildNotification(title: String, text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(title).setContentText(text).setOngoing(true).setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentIntent(mainPendingIntent()).build()

    private fun mainPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        return PendingIntent.getActivity(this, 1001, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Acquisition", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "MultiNodeAcquisition"
        const val ACTION_START_LOGGING = "com.st.bluems.multinode.action.START_LOGGING"
        const val ACTION_STOP_LOGGING = "com.st.bluems.multinode.action.STOP_LOGGING"
        const val ACTION_STOP_ALL = "com.st.bluems.multinode.action.STOP_ALL"
        const val EXTRA_NODE_IDS = "extra_node_ids"
        const val EXTRA_ENABLE_SERVER = "extra_enable_server"
        const val EXTRA_MAX_PAYLOAD_SIZE = "extra_max_payload_size"
        const val EXTRA_MAX_CONNECTION_RETRIES = "extra_max_connection_retries"
        private const val CHANNEL_ID = "multi_node_acquisition"
        private const val NOTIFICATION_ID = 4102
    }
}
