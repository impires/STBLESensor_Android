package com.st.multinode

import com.st.blue_sdk.di.LogDirectoryPath
import com.st.blue_sdk.features.Feature
import com.st.blue_sdk.features.FeatureUpdate
import com.st.blue_sdk.logger.Logger
import com.st.blue_sdk.models.Node
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Formatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class MultiNodeCsvFileLogger @Inject constructor(
    @param:LogDirectoryPath private val logDirectoryPath: String,
) : Logger {

    companion object {
        const val TAG = "MultiNodeCsvFileLogger"
        const val FILE_DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val HEADER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    private val formatterMap = ConcurrentHashMap<String, Formatter>()
    private var startLog = Date()

    override var isEnabled: Boolean = false
        set(value) {
            if (value) {
                startLog = Date()
            } else {
                formatterMap.values.forEach { it.close() }
                formatterMap.clear()
            }
            field = value
        }

    override val id: String
        get() = TAG

    override fun clear() {
        val logDir = File(logDirectoryPath)
        if (logDir.exists() && logDir.isDirectory) {
            logDir.deleteRecursively()
        }
    }

    override fun log(
        node: Node,
        feature: Feature<*>,
        update: FeatureUpdate<*>
    ): Boolean {
        if (!isEnabled) return false

        Log.d(TAG, "log() called for node=${safeNodeLabel(node)} feature=${feature.name} value=${update.logValue}")

        val formatter = getOrCreateFormatter(node, feature, update)
        formatter.format("${safeNodeLabel(node)}, ")
        formatter.format("${update.notificationTime.time - startLog.time}, ")
        formatter.format(update.logValue.replace("\n", " "))
        formatter.format("\n")
        formatter.flush()

        return true
    }

    private fun getOrCreateFormatter(
        node: Node,
        feature: Feature<*>,
        update: FeatureUpdate<*>
    ): Formatter {
        val key = formatterKey(node, feature)
        return formatterMap[key] ?: synchronized(this) {
            formatterMap[key] ?: createWithHeader(node, feature, update, key)
        }
    }

    private fun createWithHeader(
        node: Node,
        feature: Feature<*>,
        update: FeatureUpdate<*>,
        key: String
    ): Formatter {
        val sessionPrefix = SimpleDateFormat(FILE_DATE_FORMAT, Locale.getDefault())
            .format(startLog)

        val featureName = feature.name
        val nodeTag = safeNodeFileTag(node)

        val file = createFile(
            path = logDirectoryPath,
            fileName = "${sessionPrefix}_${nodeTag}_$featureName.csv"
        )

        Log.d(TAG, "createWithHeader() file=${file.absolutePath}")

        val formatter = Formatter(file)
        val startLogDate = SimpleDateFormat(HEADER_DATE_FORMAT, Locale.getDefault())
            .format(startLog)

        formatter.format("Log start on, $startLogDate\n")
        formatter.format("Feature, $featureName\n")
        formatter.format("NodeName, HostTimestamp (ms), ${update.logHeader}\n")
        formatter.flush()

        formatterMap[key] = formatter
        return formatter
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
        return File(storageDir, fileName)
    }
}