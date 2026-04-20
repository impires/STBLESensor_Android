package com.st.multinode.logging

interface StartLoggingUseCase {
    suspend fun start(nodeId: String): Result<Unit>
}