package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class SdFlowStarter @Inject constructor(
    private val officialSdLogEngine: OfficialSdLogEngine
) {

    suspend fun prepareFlow(nodeId: String, flowFileName: String): Result<Unit> = runCatching {
        require(flowFileName.isNotBlank()) {
            "Flow file name vazio"
        }

        val compName = officialSdLogEngine.getLogComponentName(nodeId)
        val datetimeShort = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())

        Log.d(TAG, "[$nodeId] Flow recebido: $flowFileName (usando componente: $compName)")

        Log.d(TAG, "[$nodeId] set_time")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = compName,
                command = "set_time",
                fields = mapOf("datetime" to datetimeShort)
            )
        )
        delay(1000)

        Log.d(TAG, "[$nodeId] acquisition_info")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = compName,
                command = "acquisition_info",
                fields = mapOf(
                    "name" to "M_$datetimeShort",
                    "description" to "MN",
                    "interface" to 2
                )
            )
        )
        delay(1500)

        Log.d(TAG, "[$nodeId] load_file")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = compName,
                command = "load_file",
                fields = mapOf("value" to flowFileName)
            )
        )
        delay(2000)

        Log.d(TAG, "[$nodeId] get_status após load_file")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = compName, command = "get_status")
        )
        delay(1500)

        Log.i(TAG, "[$nodeId] Flow preparado")
    }

    suspend fun startFlow(nodeId: String, flowFileName: String): Result<Unit> = runCatching {
        require(flowFileName.isNotBlank()) { "Flow file name vazio" }

        val logComp = officialSdLogEngine.getLogComponentName(nodeId)
        val controllerComp = "log_controller"

        Log.d(TAG, "[$nodeId] Flow recebido: $flowFileName (logComp=$logComp, controllerComp=$controllerComp)")

        Log.d(TAG, "[$nodeId] get_status inicial em $logComp")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = logComp, command = "get_status")
        )
        delay(1000)

        Log.d(TAG, "[$nodeId] run")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = logComp,
                command = "run"
            )
        )
        delay(3000)

        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = logComp, command = "get_status")
        )
        delay(2000)

        Log.d(TAG, "[$nodeId] start_log")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = logComp,
                command = "start_log",
                fields = mapOf("interface" to 2)
            )
        )
        delay(5000)

        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = logComp, command = "get_status")
        )
        delay(2000)

        Log.i(TAG, "[$nodeId] Comandos de start do flow enviados")
    }

    suspend fun stopFlow(nodeId: String): Result<Unit> = runCatching {
        val compName = officialSdLogEngine.getLogComponentName(nodeId)

        Log.d(TAG, "[$nodeId] stop_log (usando componente: $compName)")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = compName,
                command = "stop_log",
                fields = mapOf("interface" to 2)
            )
        )

        delay(1500)

        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = compName, command = "get_status")
        )

        Log.i(TAG, "[$nodeId] Comandos de stop do flow enviados")
    }

    companion object {
        private const val TAG = "SdFlowStarter"
    }
}