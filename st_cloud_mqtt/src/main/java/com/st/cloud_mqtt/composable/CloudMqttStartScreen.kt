package com.st.cloud_mqtt.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.st.cloud_mqtt.ApplicationConfigurationNavKey
import com.st.cloud_mqtt.CloudMqttConfigurationScreen
import com.st.cloud_mqtt.CloudMqttDeviceConnectionScreen
import com.st.cloud_mqtt.CloudMqttViewModel
import com.st.cloud_mqtt.DeviceConnectionNavKey
import com.st.cloud_mqtt.R
import com.st.ui.theme.Grey7
import com.st.ui.theme.Shapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudMqttStartScreen(
    modifier: Modifier,
    viewModel: CloudMqttViewModel
) {

    val backState = rememberNavBackStack(ApplicationConfigurationNavKey)

    val lastState = backState.lastOrNull()
    val selectedIndex by remember(key1 = lastState) {
        derivedStateOf {
            if (lastState == DeviceConnectionNavKey) 1 else 0
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            if (CloudMqttConfig.CloudTabBar != null) {
                CloudMqttConfig.CloudTabBar?.invoke("Cloud MQTT")
            } else {
                PrimaryTabRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                selectedTabIndex = selectedIndex,
                                matchContentSize = false
                            ),
                            width = 60.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            height = 4.dp,
                            shape = Shapes.medium
                        )
                    }) {
                    Tab(
                        selected = 0 == selectedIndex,
                        unselectedContentColor = Grey7,
                        enabled = false,
                        onClick = {},
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.cloud_app_config),
                                contentDescription = stringResource(id = R.string.navigation_tab_cloud_config)
                            )
                        },
                        text = { Text(text = stringResource(id = R.string.navigation_tab_cloud_config)) },
                    )


                    Tab(
                        selected = 1 == selectedIndex,
                        unselectedContentColor = Grey7,
                        enabled = false,
                        onClick = {},
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.cloud_dev_upload),
                                contentDescription = stringResource(id = R.string.navigation_tab_device_connection)
                            )
                        },
                        text = { Text(text = stringResource(id = R.string.navigation_tab_device_connection)) },
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .consumeWindowInsets(paddingValues)
                .padding(paddingValues)
        ) {
            NavDisplay(
                backStack = backState,
                onBack = { backState.removeLastOrNull() },
                entryProvider = entryProvider {
                    CloudMqttConfigurationScreen(viewModel, backState)
                    CloudMqttDeviceConnectionScreen(viewModel, backState)
                }
            )
        }
    }
}