package com.st.bluems

interface StopLoggingUseCase {
    suspend fun stop(nodeId: String): Result<Unit>
}