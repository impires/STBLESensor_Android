package com.st.bluems.multinode

data class ManagedNode(
    val id: String,
    val name: String,
    val mac: String,
    val isSelected: Boolean = false,
    val isConnected: Boolean = false,
    val isReady: Boolean = false,
    val isLogging: Boolean = false,
    val error: String? = null
)
