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
import com.st.blue_sdk.BlueManager
import com.st.multinode.data.MultiNodeRepository
import com.st.multinode.logging.StartLoggingUseCase
import com.st.multinode.logging.StopLoggingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class MultiNodeAcquisitionService : Service() {

    @Inject
    lateinit var blueManager: BlueManager

    @Inject
    lateinit var repository: MultiNodeRepository

    @Inject
    lateinit var startLoggingUseCase: StartLoggingUseCase

    @Inject
    lateinit var stopLoggingUseCase: StopLoggingUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val nodeJobs = ConcurrentHashMap<String, Job>()
    private val featureJobs = ConcurrentHashMap<String, Job>()
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
        Log.d(TAG, "onStartCommand action=${intent?.action}")

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

            else -> updateNotification()
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
        Log.d(TAG, "startManagedLogging nodeId=$nodeId")

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

                    Log.d(TAG, "prepareResult for $nodeId: $prepareResult")

                    if (prepareResult.isFailure) {
                        repository.markError(
                            nodeId = nodeId,
                            message = prepareResult.exceptionOrNull()?.message ?: "Connection failed"
                        )
                        return@launch
                    }
                }

                val streamResult = startFeatureStreaming(nodeId)
                Log.d(TAG, "startFeatureStreaming for $nodeId: $streamResult")

                if (streamResult.isFailure) {
                    repository.markLogging(nodeId, false)
                    repository.markError(
                        nodeId = nodeId,
                        message = streamResult.exceptionOrNull()?.message ?: "No features available"
                    )
                    repository.disconnect(nodeId)
                    return@launch
                }

                delay(1000L)

                val startResult = startLoggingUseCase.start(nodeId)
                Log.d(TAG, "startLoggingUseCase.start for $nodeId: $startResult")

                if (startResult.isFailure) {
                    stopFeatureStreaming(nodeId)
                    repository.markLogging(nodeId, false)
                    repository.markError(
                        nodeId = nodeId,
                        message = startResult.exceptionOrNull()?.message ?: "Start failed"
                    )
                    repository.disconnect(nodeId)
                    return@launch
                }

                activeLoggingNodes.add(nodeId)
                repository.markLogging(nodeId, true)
                repository.markError(nodeId, null)
                acquireWakeLockIfNeeded()
                updateNotification()

            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.e(TAG, "Unexpected acquisition error for $nodeId", t)
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
        Log.d(TAG, "nodeFeatures for $nodeId -> count=${features.size}")

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
                // manter vivo
                // isto ativa as features na board e deixa os sensores a produzir dados
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
        }.onFailure {
            Log.e(TAG, "disableFeatures failed for $nodeId", it)
        }
    }

    private suspend fun stopManagedLogging(nodeId: String) {
        Log.d(TAG, "stopManagedLogging nodeId=$nodeId")

        nodeJobs.remove(nodeId)?.cancelAndJoin()

        val stopResult = stopLoggingUseCase.stop(nodeId)
        Log.d(TAG, "stopLoggingUseCase.stop for $nodeId: $stopResult")

        if (stopResult.isFailure) {
            repository.markError(
                nodeId = nodeId,
                message = stopResult.exceptionOrNull()?.message ?: "Stop failed"
            )
        }

        delay(3000L)

        stopFeatureStreaming(nodeId)

        repository.markLogging(nodeId, false)
        activeLoggingNodes.remove(nodeId)

        val disconnectResult = repository.disconnect(nodeId)
        Log.d(TAG, "disconnectResult for $nodeId: $disconnectResult")

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
        val ids = (
                activeLoggingNodes.toList() +
                        nodeJobs.keys().toList() +
                        featureJobs.keys().toList()
                ).distinct()

        ids.forEach { nodeId ->
            stopManagedLogging(nodeId)
        }
    }

    private fun maybeStopSelf() {
        if (activeLoggingNodes.isEmpty() && nodeJobs.isEmpty() && featureJobs.isEmpty()) {
            stopSelf()
        }
    }

    private fun acquireWakeLockIfNeeded() {
        if (wakeLock?.isHeld == true) return

        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$packageName:multi-node-acquisition"
            ).apply {
                setReferenceCounted(false)
                acquire(8 * 60 * 60 * 1000L)
            }
        } catch (security: SecurityException) {
            Log.e(TAG, "WakeLock permission missing", security)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to acquire WakeLock", t)
        }
    }

    private fun releaseWakeLock(force: Boolean) {
        val shouldRelease = force || activeLoggingNodes.isEmpty()
        if (!shouldRelease) return

        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                }
            }
        } catch (security: SecurityException) {
            Log.e(TAG, "WakeLock release permission issue", security)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to release WakeLock", t)
        } finally {
            wakeLock = null
        }
    }

    private fun updateNotification() {
        val preparingCount = nodeJobs.size
        val loggingCount = activeLoggingNodes.size

        val text = when {
            loggingCount > 0 && preparingCount > 0 ->
                "Logging on $loggingCount node(s), preparing $preparingCount"
            loggingCount > 0 ->
                "Logging on $loggingCount node(s)"
            preparingCount > 0 ->
                "Preparing $preparingCount node(s)"
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
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentIntent(mainPendingIntent())
        .build()

    private fun mainPendingIntent(): PendingIntent {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent()

        return PendingIntent.getActivity(
            this,
            1001,
            launchIntent,
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
        private const val TAG = "MultiNodeAcquisition"

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