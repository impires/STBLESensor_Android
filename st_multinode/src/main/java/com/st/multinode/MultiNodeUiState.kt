package com.st.multinode

data class MultiNodeUiState(
    val discovered: List<ManagedNode> = emptyList(),
    val flowFileName: String = "SENSORTILE.BOX",
    val isConnecting: Boolean = false,
    val isStartingAll: Boolean = false,
    val isStoppingAll: Boolean = false
) {
    val selected: List<ManagedNode>
        get() = discovered.filter { it.isSelected }

    val selectedCount: Int
        get() = selected.size
}