package com.st.core.multinode

data class MultiNodeUiState(
    val discovered: List<ManagedNode> = emptyList(),
    val isConnecting: Boolean = false,
    val isStartingAll: Boolean = false,
    val isStoppingAll: Boolean = false
) {
    val selected: List<ManagedNode>
        get() = discovered.filter { it.isSelected }

    val selectedCount: Int
        get() = selected.size
}