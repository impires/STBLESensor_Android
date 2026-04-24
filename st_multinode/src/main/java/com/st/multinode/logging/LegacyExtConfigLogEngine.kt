package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.extended.ext_configuration.ExtConfiguration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class LegacyExtConfigLogEngine @Inject constructor(
    private val blueManager: BlueManager
) : NodeLogEngine {

    override suspend fun prepareFlow(nodeId: String, flowJson: String) {
        val extConfig = blueManager.nodeFeatures(nodeId)
            .filterIsInstance<ExtConfiguration>()
            .firstOrNull()
            ?: throw IllegalStateException("[$nodeId] ExtConfiguration não encontrada")

        Log.d(TAG, "[$nodeId] ExtConfiguration disponível; preparação legacy OK")

        setTime(nodeId)
        delay(100)
        setDate(nodeId)
        delay(100)
    }

    override suspend fun startFlow(nodeId: String, flowJson: String) {
        Log.d(TAG, "[$nodeId] A iniciar flow em modo legacy/ExtConfiguration")

        throw IllegalStateException(
            "[$nodeId] SensorTile legacy reconhecido, mas o comando real de START ainda não está implementado"
        )
    }

    override suspend fun stopFlow(nodeId: String) {
        Log.w(TAG, "[$nodeId] stopFlow legacy chamado, mas não implementado")
    }

    override suspend fun requestStatus(nodeId: String) {
        Log.d(TAG, "[$nodeId] requestStatus legacy via ReadBanksFwId")
    }

    override suspend fun release(nodeId: String) {
        Log.d(TAG, "[$nodeId] release legacy")
    }

    override fun getLogComponentName(nodeId: String): String {
        return SD_LOG_COMPONENT
    }

    override fun getControllerComponentName(nodeId: String): String {
        return LOG_CONTROLLER_COMPONENT
    }

    private suspend fun setTime(nodeId: String) {
        val now = java.util.Calendar.getInstance()
        val hh = now.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val mm = now.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        val ss = now.get(java.util.Calendar.SECOND).toString().padStart(2, '0')

        val timeString = "$hh:$mm:$ss"

        Log.d(TAG, "[$nodeId] SetTime=$timeString")
    }

    private suspend fun setDate(nodeId: String) {
        val now = java.util.Calendar.getInstance()
        val dd = now.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val mm = (now.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val yy = (now.get(java.util.Calendar.YEAR) % 100).toString().padStart(2, '0')

        val dateString = "$dd/$mm/$yy"

        Log.d(TAG, "[$nodeId] SetDate=$dateString")
    }

    companion object {
        private const val TAG = "LegacyExtConfigLogEngine"
        private const val SD_LOG_COMPONENT = "sd_log"
        private const val LOG_CONTROLLER_COMPONENT = "log_controller"
    }
}