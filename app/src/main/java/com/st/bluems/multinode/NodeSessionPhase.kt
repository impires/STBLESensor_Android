package com.st.bluems.multinode

enum class NodeSessionPhase {
    Idle,
    Connecting,
    Connected,
    Ready,
    Logging,
    Disconnecting,
    Disconnected,
    Error
}

data class NodeSessionState(
    val nodeId: String,
    val phase: NodeSessionPhase = NodeSessionPhase.Idle,
    val retryCount: Int = 0,
    val error: String? = null
)