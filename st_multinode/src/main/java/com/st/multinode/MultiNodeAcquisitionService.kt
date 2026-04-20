package com.st.multinode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.st.bluems.MainActivity
import com.st.bluems.R
import com.st.bluems.StartLoggingUseCase
import com.st.bluems.StopLoggingUseCase
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.st.blue_sdk.BlueManager

@AndroidEntryPoint
class MultiNodeAcquisitionService : Service() {

    @Inject
    lateinit var blueManager: BlueManager

    private val featureJobs = ConcurrentHashMap<String, Job>()

    @Inject
    lateinit var repository: MultiNodeRepository

    @Inject
    lateinit var startLoggingUseCase: StartLoggingUseCase

    @Inject
    lateinit var stopLoggingUseCase: StopLoggingUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nodeJobs = ConcurrentHashMap<String, Job>()
    private val activeLoggingNodes = ConcurrentHashMap.newKeySet<String>()
    private val prepareSemaphore = Semaphore(2)

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
            buildNotification(
                title = "ST BLE Sensor",
                text = "Waiting for multi-device acquisition"
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOGGING -> {
                val nodeIds = intent.getStringArrayListExtra(EXTRA_NODE_IDS).orEmpty()
                val enableServer = intent.getBooleanExtra(EXTRA_ENABLE_SERVER, false)
                val maxPayloadSize = intent.getIntExtra(EXTRA_MAX_PAYLOAD_SIZE, 248)
                val maxConnectionRetries = intent.getIntExtra(
                    EXTRA_MAX_CONNECTION_RETRIES,
                    3
                )

                nodeIds.forEach { nodeId ->
                    startManagedLogging(
                        nodeId = nodeId,
                        enableServer = enableServer,
                        maxPayloadSize = maxPayloadSize,
                        maxConnectionRetries = maxConnectionRetries
                    )
                }
            }

            ACTION_STOP_LOGGING -> {
                val nodeIds = intent.getStringArrayListExtra(EXTRA_NODE_IDS).orEmpty()
                serviceScope.launch {
                    nodeIds.forEach { nodeId ->
                        stopManagedLogging(nodeId)
                    }
                }
            }

            ACTION_STOP_ALL -> {
                serviceScope.launch {
                    stopAllManagedLogging()
                }
            }

            else -> {
                updateNotification()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock(force = true)
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startManagedLogging(
        nodeId: String,
        enableServer: Boolean,
        maxPayloadSize: Int,
        maxConnectionRetries: Int
    ) {
        if (activeLoggingNodes.contains(nodeId)) return
        if (nodeJobs[nodeId]?.isActive == true) return

        val job = serviceScope.launch {
            try {
                updateNotification()

                prepareSemaphore.withPermit {
                    val prepareResult = repository.connectAndAwaitReady(
                        nodeId = nodeId,
                        maxConnectionRetries = maxConnectionRetries,
                        maxPayloadSize = maxPayloadSize,
                        enableServer = enableServer
                    )

                    if (prepareResult.isFailure) {
                        repository.markError(
                            nodeId = nodeId,
                            message = prepareResult.exceptionOrNull()?.message ?: "Connection failed"
                        )
                        return@launch
                    }
                }

                val startResult = startLoggingUseCase.start(nodeId)
                if (startResult.isFailure) {
                    repository.markLogging(nodeId, false)
                    repository.markError(
                        nodeId = nodeId,
                        message = startResult.exceptionOrNull()?.message ?: "Start failed"
                    )
                    repository.disconnect(nodeId)
                    return@launch
                }

                val streamResult = startFeatureStreaming(nodeId)
                if (streamResult.isFailure) {
                    stopLoggingUseCase.stop(nodeId)
                    repository.markLogging(nodeId, false)
                    repository.markError(
                        nodeId = nodeId,
                        message = streamResult.exceptionOrNull()?.message ?: "No features available"
                    )
                    repository.disconnect(nodeId)
                    return@launch
                }

                activeLoggingNodes.add(nodeId)
                repository.markLogging(nodeId, true)
                repository.markError(nodeId, null)
                acquireWakeLockIfNeeded()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                stopFeatureStreaming(nodeId)
                repository.markLogging(nodeId, false)
                repository.markError(
                    nodeId = nodeId,
                    message = t.message ?: "Unexpected acquisition error"
                )
                repository.disconnect(nodeId)
            } finally {
                nodeJobs.remove(nodeId)
                updateNotification()
                maybeStopSelf()
            }
        }

        nodeJobs[nodeId] = job
        updateNotification()
    }

    private suspend fun startFeatureStreaming(nodeId: String): Result<Unit> {
        if (featureJobs[nodeId]?.isActive == true) {
            return Result.success(Unit)
        }

        val features = blueManager.nodeFeatures(nodeId)
        if (features.isEmpty()) {
            return Result.failure(
                IllegalStateException("No loggable features found for node $nodeId")
            )
        }

        val featureJob = serviceScope.launch {
            blueManager.getFeatureUpdates(
                nodeId = nodeId,
                features = features,
                autoEnable = true
            ).collect {
                // Intentionally empty.
                // BlueST SDK forwards each FeatureUpdate to enabled loggers internally.
            }
        }

        featureJobs[nodeId] = featureJob
        return Result.success(Unit)
    }

    private suspend fun stopFeatureStreaming(nodeId: String) {
        featureJobs.remove(nodeId)?.cancelAndJoin()

        runCatching {
            val features = blueManager.nodeFeatures(nodeId)
            if (features.isNotEmpty()) {
                blueManager.disableFeatures(nodeId, features)
            }
        }
    }

    private suspend fun stopManagedLogging(nodeId: String) {
        nodeJobs.remove(nodeId)?.cancelAndJoin()

        stopFeatureStreaming(nodeId)

        val stopResult = stopLoggingUseCase.stop(nodeId)
        if (stopResult.isFailure) {
            repository.markError(
                nodeId = nodeId,
                message = stopResult.exceptionOrNull()?.message ?: "Stop failed"
            )
        }

        repository.markLogging(nodeId, false)
        activeLoggingNodes.remove(nodeId)

        val disconnectResult = repository.disconnect(nodeId)
        if (disconnectResult.isFailure) {
            repository.markError(
                nodeId = nodeId,
                message = disconnectResult.exceptionOrNull()?.message ?: "Disconnect failed"
            )
        }

        releaseWakeLock(force = false)
        updateNotification()
        maybeStopSelf()
    }

    private suspend fun stopAllManagedLogging() {
        val ids = (activeLoggingNodes.toList() + nodeJobs.keys().toList() + featureJobs.keys().toList())
            .distinct()

        ids.forEach { nodeId ->
            stopManagedLogging(nodeId)
        }
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
            "$packageName:multi-node-acquisition"
        ).apply {
            setReferenceCounted(false)
            acquire(8 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock(force: Boolean) {
        val shouldRelease = force || activeLoggingNodes.isEmpty()
        if (!shouldRelease) return

        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null
    }

    private fun updateNotification() {
        val connectingCount = nodeJobs.size
        val loggingCount = activeLoggingNodes.size

        val text = when {
            loggingCount > 0 && connectingCount > 0 ->
                "Logging on $loggingCount node(s), preparing $connectingCount"
            loggingCount > 0 ->
                "Logging on $loggingCount node(s)"
            connectingCount > 0 ->
                "Preparing $connectingCount node(s)"
            else ->
                "Waiting for multi-device acquisition"
        }

        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(
                title = "ST BLE Sensor",
                text = text
            )
        )
    }

    private fun buildNotification(
        title: String,
        text: String
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentIntent(mainPendingIntent())
        .build()

    private fun mainPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            Intent.setFlags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Multi-device acquisition",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps BLE acquisition alive during screen lock"
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START_LOGGING =
            "com.st.bluems.multinode.action.START_LOGGING"
        const val ACTION_STOP_LOGGING =
            "com.st.bluems.multinode.action.STOP_LOGGING"
        const val ACTION_STOP_ALL =
            "com.st.bluems.multinode.action.STOP_ALL"

        const val EXTRA_NODE_IDS = "extra_node_ids"
        const val EXTRA_ENABLE_SERVER = "extra_enable_server"
        const val EXTRA_MAX_PAYLOAD_SIZE = "extra_max_payload_size"
        const val EXTRA_MAX_CONNECTION_RETRIES = "extra_max_connection_retries"

        private const val CHANNEL_ID = "multi_node_acquisition"
        private const val NOTIFICATION_ID = 4102
    }
}