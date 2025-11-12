package com.st.cloud_mqtt.composable

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.st.cloud_mqtt.CloudMqttNavigationApplicationConfiguration
import com.st.cloud_mqtt.CloudMqttNavigationDeviceConnection
import com.st.cloud_mqtt.CloudMqttViewModel
import com.st.cloud_mqtt.R
import com.st.ui.theme.Grey7
import com.st.ui.theme.Shapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudMqttStartScreen(
    modifier: Modifier,
    viewModel: CloudMqttViewModel,
    navController: NavHostController = rememberNavController()
) {

    var selectedIndex by remember {
        mutableIntStateOf(0)
    }

    val isBrokerConfigured by viewModel.isBrokerConfigured.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            if (CloudMqttConfig.CloudTabBar != null) {
                CloudMqttConfig.CloudTabBar?.invoke("Cloud MQTT")
            } else {
                PrimaryTabRow(modifier = Modifier
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = 0
                            navController.navigate(CloudMqttNavigationApplicationConfiguration.route) {
                                navController.graph.startDestinationRoute?.let { screenRoute ->
                                    popUpTo(screenRoute) {
                                        saveState = false
                                    }
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
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
                        unselectedContentColor = if (isBrokerConfigured) LocalContentColor.current else Grey7,
                        enabled = isBrokerConfigured,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = 1
                            navController.navigate(CloudMqttNavigationDeviceConnection.route) {
                                navController.graph.startDestinationRoute?.let { screenRoute ->
                                    popUpTo(screenRoute) {
                                        saveState = false
                                    }
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
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
        Box(modifier = Modifier.consumeWindowInsets(paddingValues).padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = CloudMqttNavigationApplicationConfiguration.route
            ) {
                composable(route = CloudMqttNavigationApplicationConfiguration.route,
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(500)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(500)
                        )
                    }) {
                    selectedIndex = 0
                    CloudMqttApplicationConfiguration(
                        viewModel = viewModel
                    )
                }


                composable(route = CloudMqttNavigationDeviceConnection.route,
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(500)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(500)
                        )
                    }) {
                    selectedIndex = 1
                    CloudMqttDeviceConnection(
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}