package com.st.multinode.logging

interface BoardSdLoggingTransport {
    suspend fun startSdLogging(nodeId: String): Result<Unit>
    suspend fun stopSdLogging(nodeId: String): Result<Unit>
    suspend fun setProperty(
        nodeId: String,
        component: String,
        fields: Map<String, Any>
    ): Result<Unit>
}