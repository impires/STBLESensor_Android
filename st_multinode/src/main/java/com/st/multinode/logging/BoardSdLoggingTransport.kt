package com.st.multinode.logging

interface BoardSdLoggingTransport {
    suspend fun sendPnplCommand(
        nodeId: String,
        json: String
    )
}