package com.st.multinode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MultiNodeScreen(
    viewModel: MultiNodeViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Multi-device acquisition",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selecionados: ${uiState.selectedCount}/4",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prepareSelected() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Prepare")
            }

            Button(
                onClick = { viewModel.startAll() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start All")
            }

            OutlinedButton(
                onClick = { viewModel.stopAll() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.disconnectSelected() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Disconnect Selected")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onBack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.discovered, key = { it.id }) { node ->
                MultiNodeRow(
                    node = node,
                    onToggle = { viewModel.toggleNodeSelection(node.id) }
                )
            }
        }
    }
}

@Composable
private fun MultiNodeRow(
    node: ManagedNode,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = node.isSelected,
                onCheckedChange = { onToggle() }
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "MAC: ${node.mac}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = buildNodeStatus(node),
                    style = MaterialTheme.typography.bodySmall
                )
                node.error?.let { err ->
                    Text(
                        text = "Erro: $err",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun buildNodeStatus(node: ManagedNode): String {
    return when {
        node.error != null -> "Error"
        node.isLogging -> "Logging"
        node.isReady -> "Ready"
        node.isConnected -> "Connected"
        else -> "Discovered"
    }
}