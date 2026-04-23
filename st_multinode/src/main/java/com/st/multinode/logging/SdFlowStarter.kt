package com.st.multinode.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SdFlowStarter @Inject constructor(
    private val singleNodeFlowUploader: SingleNodeFlowUploader,
    private val officialSdLogEngine: OfficialSdLogEngine
) {

    suspend fun prepareFlow(nodeId: String, flowJson: String): Result<Unit> = runCatching {
        require(flowJson.isNotBlank()) { "Flow json vazio" }
        officialSdLogEngine.ensurePnplReady(nodeId)
    }

    suspend fun startFlow(nodeId: String, flowJson: String): Result<Unit> = runCatching {
        require(flowJson.isNotBlank()) { "Flow json vazio" }

        val flowBytes = flowJson.toByteArray(Charsets.UTF_8)

        singleNodeFlowUploader
            .uploadAndStart(nodeId, flowBytes)
            .getOrThrow()
    }

    suspend fun stopFlow(nodeId: String): Result<Unit> = runCatching {
        val logComp = officialSdLogEngine.getLogComponentName(nodeId)
        val controllerComp = officialSdLogEngine.getControllerComponentName(nodeId)

        runCatching {
            officialSdLogEngine.sendRawCommand(
                nodeId,
                com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd(
                    component = logComp,
                    command = "stop_log",
                    fields = mapOf("interface" to 2)
                )
            )
        }

        runCatching {
            officialSdLogEngine.sendRawCommand(
                nodeId,
                com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd(
                    component = controllerComp,
                    command = "stop"
                )
            )
        }
    }
}