package com.st.multinode.logging

import android.util.Log
import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.extended.pnpl.PnPL
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCmd
import com.st.blue_sdk.features.extended.pnpl.request.PnPLCommand
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

@Singleton
class SingleNodeFlowUploader @Inject constructor(
    private val blueManager: BlueManager
) {

    companion object {
        private const val TAG = "SingleNodeFlowUploader"

        private const val CHUNK_SIZE = 20
        private const val SEND_FLOW_REQUEST = "SF"
        private const val SEND_FLOW_RESPONSE = "Flow_Req_Received"
        private const val SEND_PARSING_FLOW_RESPONSE = "Parsing_flow"
        private const val FLOW_PARSED_MESSAGE_OK = "Flow_parse_ok"
        private const val FLOW_ERROR_MESSAGE = "Error:"
    }

    suspend fun uploadAndStart(
        nodeId: String,
        flowBytes: ByteArray
    ): Result<Unit> = runCatching {
        require(flowBytes.isNotEmpty()) { "Flow bytes vazios" }

        Log.d(TAG, "[$nodeId] uploadAndStart bytes=${flowBytes.size}")

        var offset = 0
        var parsingStarted = false
        var finished = false

        suspend fun sendMessage(message: ByteArray) {
            val payload = String(message, StandardCharsets.ISO_8859_1)
            blueManager.writeDebugMessage(nodeId = nodeId, msg = payload)
            Log.d(TAG, "[$nodeId] sent debug chunk size=${message.size}")
        }

        fun nextChunk(): ByteArray? {
            if (offset >= flowBytes.size) return null
            val end = minOf(offset + CHUNK_SIZE, flowBytes.size)
            val out = flowBytes.copyOfRange(offset, end)
            offset = end
            return out
        }

        suspend fun waitDebugMessage(timeoutMs: Long = 5000): String {
            return withTimeout(timeoutMs) {
                blueManager.getDebugMessages(nodeId)
                    ?.map { it.payload.replace("\n", "").replace("\r", "") }
                    ?.first()
                    ?: throw IllegalStateException("[$nodeId] Debug message flow null")
            }
        }

        sendMessage(startFlowMessage(flowBytes.size))
        Log.d(TAG, "[$nodeId] SF header enviado")

        while (!finished) {
            val msg = waitDebugMessage()

            Log.d(TAG, "[$nodeId] debug rx=<$msg>")

            when {
                msg.startsWith(SEND_FLOW_RESPONSE) -> {
                    offset = 0
                    val firstChunk = nextChunk()
                    require(firstChunk != null) { "[$nodeId] Primeiro chunk nulo" }
                    sendMessage(firstChunk)
                }

                msg.startsWith(SEND_PARSING_FLOW_RESPONSE) -> {
                    parsingStarted = true
                    Log.d(TAG, "[$nodeId] Flow recebido, parsing a decorrer")
                }

                msg.startsWith(FLOW_PARSED_MESSAGE_OK) -> {
                    finished = true
                    Log.d(TAG, "[$nodeId] Flow parse OK")
                }

                msg.startsWith(FLOW_ERROR_MESSAGE) -> {
                    throw IllegalStateException("[$nodeId] Firmware respondeu erro: $msg")
                }

                else -> {
                    if (!parsingStarted) {
                        val next = nextChunk()
                        if (next != null) {
                            sendMessage(next)
                        } else {
                            Log.d(TAG, "[$nodeId] Sem mais chunks para enviar; a aguardar parse")
                        }
                    }
                }
            }
        }

        val pnpl = blueManager.nodeFeatures(nodeId)
            .filterIsInstance<PnPL>()
            .firstOrNull()
            ?: throw IllegalStateException("[$nodeId] Feature PnPL não encontrada")

        val startLogCmd = PnPLCmd(
            component = "sd_log",
            command = "start_log",
            fields = mapOf("interface" to 2)
        )

        blueManager.writeFeatureCommand(
            nodeId,
            PnPLCommand(pnpl, startLogCmd),
            0L
        )

        Log.d(TAG, "[$nodeId] start_log enviado após Flow_parse_ok")

        val startControllerCmd = PnPLCmd(
            component = "log_controller",
            command = "start"
        )

        blueManager.writeFeatureCommand(
            nodeId,
            PnPLCommand(pnpl, startControllerCmd),
            0L
        )

        Log.d(TAG, "[$nodeId] log_controller.start enviado após start_log")
    }

    private fun startFlowMessage(flowLength: Int): ByteArray {
        val message = ByteArray(10)

        val sf = SEND_FLOW_REQUEST.toByteArray()
        System.arraycopy(sf, 0, message, 0, sf.size)

        message[2] = ((flowLength shr 24) and 0xFF).toByte()
        message[3] = ((flowLength shr 16) and 0xFF).toByte()
        message[4] = ((flowLength shr 8) and 0xFF).toByte()
        message[5] = (flowLength and 0xFF).toByte()

        val now = getSecondsFrom1970().toInt()
        message[6] = ((now shr 24) and 0xFF).toByte()
        message[7] = ((now shr 16) and 0xFF).toByte()
        message[8] = ((now shr 8) and 0xFF).toByte()
        message[9] = (now and 0xFF).toByte()

        return message
    }

    private fun getSecondsFrom1970(): Long {
        val nowMs = System.currentTimeMillis()
        val tz = java.util.Calendar.getInstance()
        val offsetMs = tz.get(java.util.Calendar.ZONE_OFFSET) + tz.get(java.util.Calendar.DST_OFFSET)
        return (nowMs + offsetMs) / 1000L
    }
}