package com.st.bluems.multinode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.st.bluems.StartLoggingUseCase
import com.st.bluems.StopLoggingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MultiNodeViewModel @Inject constructor(
    private val repository: MultiNodeRepository,
    private val startLoggingUseCase: StartLoggingUseCase,
    private val stopLoggingUseCase: StopLoggingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiNodeUiState())
    val uiState: StateFlow<MultiNodeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.discoveredNodes().collect { nodes ->
                _uiState.value = _uiState.value.copy(
                    discovered = mergeNodesKeepingLocalState(nodes)
                )
            }
        }
    }

    fun setDiscoveredNodes(nodes: List<ManagedNode>) {
        repository.updateDiscoveredNodes(nodes)
    }

    fun addOrUpdateDiscoveredNode(node: ManagedNode) {
        val current = _uiState.value.discovered.toMutableList()
        val index = current.indexOfFirst { it.id == node.id || it.mac == node.mac }

        if (index >= 0) {
            val oldNode = current[index]
            current[index] = node.copy(
                isSelected = oldNode.isSelected,
                isConnected = oldNode.isConnected,
                isReady = oldNode.isReady,
                isLogging = oldNode.isLogging,
                error = node.error ?: oldNode.error
            )
        } else {
            current.add(node)
        }

        repository.updateDiscoveredNodes(current)
    }

    fun toggleNodeSelection(nodeId: String) {
        val state = _uiState.value
        val nodes = state.discovered
        val node = nodes.firstOrNull { it.id == nodeId } ?: return

        val selectedCount = nodes.count { it.isSelected }

        if (!node.isSelected && selectedCount >= 4) {
            return
        }

        val updated = nodes.map {
            if (it.id == nodeId) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }

        _uiState.value = state.copy(discovered = updated)
    }

    fun prepareSelected() {
        val nodesToPrepare = _uiState.value.discovered.filter { it.isSelected }
        if (nodesToPrepare.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)

            nodesToPrepare.forEach { node ->
                updateNode(node.id) {
                    it.copy(
                        error = null,
                        isConnected = false,
                        isReady = false
                    )
                }

                val readyResult = repository.markReady(node.id)

                if (readyResult.isSuccess) {
                    updateNode(node.id) {
                        it.copy(
                            isConnected = true,
                            isReady = true,
                            error = null
                        )
                    }
                } else {
                    updateNode(node.id) {
                        it.copy(
                            isConnected = false,
                            isReady = false,
                            error = readyResult.exceptionOrNull()?.message ?: "Node not ready"
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(isConnecting = false)
        }
    }

    fun disconnectSelected() {
        val nodesToDisconnect = _uiState.value.discovered.filter { it.isSelected && it.isConnected }
        if (nodesToDisconnect.isEmpty()) return

        viewModelScope.launch {
            nodesToDisconnect.forEach { node ->
                val result = repository.disconnect(node.id)

                if (result.isSuccess) {
                    updateNode(node.id) {
                        it.copy(
                            isConnected = false,
                            isReady = false,
                            isLogging = false,
                            error = null
                        )
                    }
                } else {
                    updateNode(node.id) {
                        it.copy(
                            error = result.exceptionOrNull()?.message ?: "Disconnect failed"
                        )
                    }
                }
            }
        }
    }

    fun markNodeConnected(nodeId: String, connected: Boolean) {
        updateNode(nodeId) {
            it.copy(
                isConnected = connected,
                isReady = if (!connected) false else it.isReady,
                error = if (connected) null else it.error
            )
        }
    }

    fun markNodeReady(nodeId: String, ready: Boolean) {
        updateNode(nodeId) {
            it.copy(
                isReady = ready,
                error = if (ready) null else it.error
            )
        }
    }

    fun markNodeError(nodeId: String, message: String) {
        updateNode(nodeId) {
            it.copy(error = message)
        }
    }

    fun clearNodeError(nodeId: String) {
        updateNode(nodeId) {
            it.copy(error = null)
        }
    }

    fun startAll() {
        val nodesToStart = _uiState.value.discovered.filter { it.isSelected && it.isReady }
        if (nodesToStart.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStartingAll = true)

            nodesToStart.forEach { node ->
                val result = startLoggingUseCase.start(node.id)
                if (result.isSuccess) {
                    updateNode(node.id) {
                        it.copy(
                            isLogging = true,
                            error = null
                        )
                    }
                } else {
                    updateNode(node.id) {
                        it.copy(
                            isLogging = false,
                            error = result.exceptionOrNull()?.message ?: "Start failed"
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(isStartingAll = false)
        }
    }

    fun stopAll() {
        val nodesToStop = _uiState.value.discovered.filter { it.isSelected && it.isLogging }
        if (nodesToStop.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStoppingAll = true)

            nodesToStop.forEach { node ->
                val result = stopLoggingUseCase.stop(node.id)
                if (result.isSuccess) {
                    updateNode(node.id) {
                        it.copy(
                            isLogging = false,
                            error = null
                        )
                    }
                } else {
                    updateNode(node.id) {
                        it.copy(
                            error = result.exceptionOrNull()?.message ?: "Stop failed"
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(isStoppingAll = false)
        }
    }

    fun clearAllSelections() {
        val updated = _uiState.value.discovered.map { it.copy(isSelected = false) }
        _uiState.value = _uiState.value.copy(discovered = updated)
    }

    private fun updateNode(nodeId: String, transform: (ManagedNode) -> ManagedNode) {
        val updated = _uiState.value.discovered.map { node ->
            if (node.id == nodeId) transform(node) else node
        }
        _uiState.value = _uiState.value.copy(discovered = updated)
    }

    private fun mergeNodesKeepingLocalState(newNodes: List<ManagedNode>): List<ManagedNode> {
        val oldMap = _uiState.value.discovered.associateBy { it.id }

        return newNodes.map { newNode ->
            val oldNode = oldMap[newNode.id]
            if (oldNode == null) {
                newNode
            } else {
                newNode.copy(
                    isSelected = oldNode.isSelected,
                    isConnected = oldNode.isConnected,
                    isReady = oldNode.isReady,
                    isLogging = oldNode.isLogging,
                    error = oldNode.error
                )
            }
        }
    }
}