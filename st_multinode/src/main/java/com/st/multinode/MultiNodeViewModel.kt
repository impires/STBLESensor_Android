package com.st.multinode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.st.multinode.data.MultiNodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class MultiNodeViewModel @Inject constructor(
    private val repository: MultiNodeRepository,
    private val acquisitionServiceController: AcquisitionServiceController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiNodeUiState())
    val uiState: StateFlow<MultiNodeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.managedNodes().collect { nodes ->
                _uiState.update { current ->
                    current.copy(discovered = nodes)
                }
            }
        }
    }

    fun setDiscoveredNodes(nodes: List<ManagedNode>) {
        repository.updateDiscoveredNodes(nodes)
    }

    fun toggleNodeSelection(nodeId: String) {
        repository.toggleSelection(nodeId = nodeId, maxSelected = 4)
    }

    fun clearAllSelections() {
        repository.clearSelection()
    }

    fun prepareSelected(
        enableServer: Boolean = false,
        maxPayloadSize: Int = 150, // Reduzido de 248
        maxConnectionRetries: Int = 3
    ) {
        val selectedNodeIds = _uiState.value.discovered
            .filter { it.isSelected }
            .map { it.id }

        if (selectedNodeIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true) }

            // Mudar para 1 garante que uma board termina de conectar/negociar MTU
            // antes da próxima começar. Isso evita ruído no rádio.
            val semaphore = Semaphore(1)

            selectedNodeIds.forEach { nodeId -> // Usa forEach em vez de launch paralelo
                semaphore.withPermit {
                    val result = repository.connectAndAwaitReady(
                        nodeId = nodeId,
                        maxConnectionRetries = maxConnectionRetries,
                        maxPayloadSize = maxPayloadSize,
                        enableServer = enableServer
                    )
                    if (result.isFailure) {
                        repository.markError(
                            nodeId = nodeId,
                            message = result.exceptionOrNull()?.message ?: "Node not ready"
                        )
                    }
                }
            }

            _uiState.update { it.copy(isConnecting = false) }
        }
    }

    fun disconnectSelected() {
        val selectedNodes = _uiState.value.discovered.filter { it.isSelected }
        if (selectedNodes.isEmpty()) return

        val serviceOwned = selectedNodes
            .filter { it.isLogging }
            .map { it.id }

        val plainDisconnect = selectedNodes
            .filter { !it.isLogging && (it.isConnected || it.isReady) }
            .map { it.id }

        viewModelScope.launch {
            if (serviceOwned.isNotEmpty()) {
                acquisitionServiceController.stopLogging(serviceOwned)
            }

            plainDisconnect.forEach { nodeId ->
                val result = repository.disconnect(nodeId)
                if (result.isFailure) {
                    repository.markError(
                        nodeId = nodeId,
                        message = result.exceptionOrNull()?.message ?: "Disconnect failed"
                    )
                }
            }
        }
    }

    fun startAll(
        enableServer: Boolean = false,
        maxPayloadSize: Int = 150, // MUDAR DE 248 PARA 150 (Igual ao prepareSelected)
        maxConnectionRetries: Int = 3
    ) {
        val nodesToStart = _uiState.value.discovered
            .filter { it.isSelected && !it.isLogging }

        if (nodesToStart.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingAll = true) }

            // Aqui é onde o Service recebe a instrução fatal.
            // Se passar 248 aqui, a board bloqueia no momento do log.
            acquisitionServiceController.startLogging(
                nodeIds = nodesToStart.map { it.id },
                enableServer = enableServer,
                maxPayloadSize = maxPayloadSize, // Agora vai usar 150
                maxConnectionRetries = maxConnectionRetries
            )

            _uiState.update { it.copy(isStartingAll = false) }
        }
    }

    fun stopAll() {
        val nodesToStop = _uiState.value.discovered
            .filter { it.isSelected && (it.isLogging || it.isConnected || it.isReady) }

        if (nodesToStop.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isStoppingAll = true) }

            acquisitionServiceController.stopLogging(
                nodeIds = nodesToStop.map { it.id }
            )

            _uiState.update { it.copy(isStoppingAll = false) }
        }
    }
}