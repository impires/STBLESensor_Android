package com.st.cloud_mqtt

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.st.cloud_mqtt.composable.CloudMqttApplicationConfiguration
import com.st.cloud_mqtt.composable.CloudMqttDeviceConnection
import kotlinx.serialization.Serializable

@Serializable
data object ApplicationConfigurationNavKey : NavKey

@Serializable
data object DeviceConnectionNavKey : NavKey

@Composable
fun EntryProviderScope<NavKey>.CloudMqttDeviceConnectionScreen(
    viewModel: CloudMqttViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<DeviceConnectionNavKey> {
        CloudMqttDeviceConnection(
            viewModel = viewModel,
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.CloudMqttConfigurationScreen(
    viewModel: CloudMqttViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<ApplicationConfigurationNavKey> {
        CloudMqttApplicationConfiguration(
            viewModel = viewModel,
            backState = backState
        )
    }
}