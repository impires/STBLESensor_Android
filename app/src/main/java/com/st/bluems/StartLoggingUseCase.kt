package com.st.bluems

interface StartLoggingUseCase {
    suspend fun start(nodeId: String): Result<Unit>
}