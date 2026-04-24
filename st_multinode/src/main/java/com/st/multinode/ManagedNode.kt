package com.st.multinode

data class ManagedNode(
    val id: String,
    val name: String? = null,
    val mac: String? = null,
    val isSelected: Boolean = false,
    val isConnected: Boolean = false,
    val isReady: Boolean = false,
    val isLogging: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null
)