package com.st.multinode.logging

interface StopLoggingUseCase {
    suspend fun stop(nodeId: String): Result<Unit>
}