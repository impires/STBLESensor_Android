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
        require(flowFileName.isNotBlank()) { "Flow file name vazio" }

        officialSdLogEngine.ensurePnplReady(nodeId)

        val logComp = officialSdLogEngine.getLogComponentName(nodeId)
        val controllerComp = officialSdLogEngine.getControllerComponentName(nodeId)

        Log.d("SdFlowStarter", "prepareFlow($nodeId)")
        Log.d("SdFlowStarter", "logComp=$logComp controllerComp=$controllerComp flow=$flowFileName")

        // discovery leve apenas
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = logComp, command = "get_status")
        )

        delay(300)

        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = controllerComp, command = "get_status")
        )
    }

    suspend fun startFlow(nodeId: String, flowFileName: String): Result<Unit> = runCatching {
        require(flowFileName.isNotBlank()) { "Flow file name vazio" }

        officialSdLogEngine.ensurePnplReady(nodeId)

        val logComp = officialSdLogEngine.getLogComponentName(nodeId)
        val controllerComp = officialSdLogEngine.getControllerComponentName(nodeId)

        Log.d("SdFlowStarter", "startFlow($nodeId)")
        Log.d("SdFlowStarter", "logComp=$logComp controllerComp=$controllerComp flow=$flowFileName")

        // 1. estado inicial
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = controllerComp, command = "get_status")
        )
        delay(500)

        // 2. carregar o flow apenas no arranque
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = controllerComp,
                command = "load_file",
                fields = mapOf("value" to flowFileName)
            )
        )

        Log.d("SdFlowStarter", "load_file enviado para $controllerComp")
        delay(2000)

        // 3. arrancar o flow
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = controllerComp, command = "run")
        )

        Log.d("SdFlowStarter", "run enviado para $controllerComp")
        delay(3000)

        // 4. iniciar logging SD
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = logComp,
                command = "start_log",
                fields = mapOf("interface" to 2)
            )
        )

        Log.d("SdFlowStarter", "start_log enviado para $logComp")
    }

    suspend fun stopFlow(nodeId: String): Result<Unit> = runCatching {
        val logComp = officialSdLogEngine.getLogComponentName(nodeId)

        Log.d(TAG, "[$nodeId] stop_log (usando componente: $logComp)")
        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(
                component = logComp,
                command = "stop_log",
                fields = mapOf("interface" to 2)
            )
        )

        delay(1500)

        officialSdLogEngine.sendRawCommand(
            nodeId,
            PnPLCmd(component = logComp, command = "get_status")
        )

        Log.i(TAG, "[$nodeId] Comandos de stop do flow enviados")
    }

    companion object {
        private const val TAG = "SdFlowStarter"
    }
}