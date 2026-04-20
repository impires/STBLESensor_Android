package com.st.multinode

import android.content.Context
import android.util.Log
import com.st.blue_sdk.features.Feature
import com.st.blue_sdk.features.FeatureUpdate
import com.st.blue_sdk.logger.Logger
import com.st.blue_sdk.models.Node
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiNodeCsvFileLogger @Inject constructor(
    @ApplicationContext private val context: Context
) : Logger {

    companion object {
        const val TAG = "MultiNodeCsvFileLogger"
        const val FILE_DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val HEADER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    private val logDirectoryPath: String
        get() = File(
            context.getExternalFilesDir(null),
            "STMicroelectronics/logs"
        ).absolutePath

    private val fileMap = ConcurrentHashMap<String, File>()
    private var startLog = Date()

    override var isEnabled: Boolean = false
        set(value) {
            if (value && !field) {
                startLog = Date()
                Log.d(TAG, "Logger enabled. startLog=$startLog path=$logDirectoryPath")
            }

            if (!value && field) {
                fileMap.clear()
                Log.d(TAG, "Logger disabled")
            }

            field = value
        }

    override val id: String
        get() = TAG

    override fun clear() {
        val logDir = File(logDirectoryPath)
        if (logDir.exists() && logDir.isDirectory) {
            logDir.deleteRecursively()
            Log.d(TAG, "Logs cleared: $logDirectoryPath")
        }
        fileMap.clear()
    }

    override fun log(
        node: Node,
        feature: Feature<*>,
        update: FeatureUpdate<*>
    ): Boolean {
        if (!isEnabled) {
            Log.d(TAG, "log() skipped because logger is disabled")
            return false
        }

        return try {
            val key = formatterKey(node, feature)
            val file = fileMap[key] ?: synchronized(this) {
                fileMap[key] ?: createFileWithHeader(node, feature, update, key)
            }

            val line = buildString {
                append(safeNodeLabel(node))
                append(", ")
                append(update.notificationTime.time - startLog.time)
                append(", ")
                append(update.logValue.replace("\n", " "))
                append("\n")
            }

            synchronized(file.absolutePath.intern()) {
                file.appendText(line)
            }

            Log.d(
                TAG,
                "APPENDED file=${file.absolutePath} size=${file.length()} line=$line"
            )
            true
        } catch (t: Throwable) {
            Log.e(TAG, "CSV write failed", t)
            false
        }
    }

    private fun createFileWithHeader(
        node: Node,
        feature: Feature<*>,
        update: FeatureUpdate<*>,
        key: String
    ): File {
        val sessionPrefix = SimpleDateFormat(FILE_DATE_FORMAT, Locale.getDefault())
            .format(startLog)

        val nodeTag = safeNodeFileTag(node)
        val featureTag = safeFeatureFileTag(feature)

        val file = createFile(
            path = logDirectoryPath,
            fileName = "${sessionPrefix}_${nodeTag}_${featureTag}.csv"
        )

        synchronized(file.absolutePath.intern()) {
            if (file.length() == 0L) {
                val startLogDate = SimpleDateFormat(HEADER_DATE_FORMAT, Locale.getDefault())
                    .format(startLog)

                file.appendText("Log start on, $startLogDate\n")
                file.appendText("Feature, ${feature.name}\n")
                file.appendText("NodeName, HostTimestamp (ms), ${update.logHeader}\n")
            }
        }

        fileMap[key] = file
        Log.d(TAG, "HEADER WRITTEN file=${file.absolutePath} size=${file.length()}")
        return file
    }

    private fun formatterKey(
        node: Node,
        feature: Feature<*>
    ): String {
        return "${safeNodeIdentity(node)}__${feature.name}"
    }

    private fun safeNodeIdentity(node: Node): String {
        return node.device.address?.ifBlank {
            node.friendlyName.ifBlank { "unknown_node" }
        } ?: node.friendlyName.ifBlank { "unknown_node" }
    }

    private fun safeNodeLabel(node: Node): String {
        return node.friendlyName.ifBlank {
            node.device.address?.ifBlank { "unknown_node" } ?: "unknown_node"
        }
    }

    private fun safeNodeFileTag(node: Node): String {
        return safeNodeLabel(node)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun safeFeatureFileTag(feature: Feature<*>): String {
        return feature.name
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun createFile(path: String, fileName: String): File {
        val storageDir = File(path)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val file = File(storageDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }

        Log.d(TAG, "CSV target file=${file.absolutePath}")
        return file
    }
}