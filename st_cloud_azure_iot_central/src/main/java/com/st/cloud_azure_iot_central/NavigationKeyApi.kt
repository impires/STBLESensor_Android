package com.st.cloud_azure_iot_central

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.st.cloud_azure_iot_central.composable.CloudAzureApplicationDetails
import com.st.cloud_azure_iot_central.composable.CloudAzureApplicationSelection
import com.st.cloud_azure_iot_central.composable.CloudAzureDeviceConnection
import com.st.cloud_azure_iot_central.composable.CloudAzureDeviceSelection
import kotlinx.serialization.Serializable

@Serializable
data object ApplicationSelectionNavKey : NavKey

@Serializable
data object DeviceSelectionNavKey : NavKey

@Serializable
data object DeviceConnectionNavKey : NavKey

@Serializable
data class ApplicationDetailsNavKey(val appId: Int) : NavKey

@Composable
fun EntryProviderScope<NavKey>.CloudAzureApplicationDetailsScreen(
    viewModel: CloudAzureIotCentralViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<ApplicationDetailsNavKey> { key ->
        CloudAzureApplicationDetails(
            viewModel = viewModel,
            appId = key.appId,
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.CloudAzureDeviceConnectionScreen(
    viewModel: CloudAzureIotCentralViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<DeviceConnectionNavKey> {
        CloudAzureDeviceConnection(
            viewModel = viewModel,
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.CloudAzureDeviceSelectionScreen(
    viewModel: CloudAzureIotCentralViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<DeviceSelectionNavKey> {
        CloudAzureDeviceSelection(
            viewModel = viewModel,
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.CloudAzureApplicationSelectionScreen(
    viewModel: CloudAzureIotCentralViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<ApplicationSelectionNavKey> {
        CloudAzureApplicationSelection(
            viewModel = viewModel,
            backState = backState
        )
    }
}