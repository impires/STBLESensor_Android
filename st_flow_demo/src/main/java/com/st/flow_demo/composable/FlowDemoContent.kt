package com.st.flow_demo.composable

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.st.flow_demo.FlowDemoFlowCategoriesExampleNavKey
import com.st.flow_demo.FlowDemoSensorsNavKey
import com.st.flow_demo.FlowDemoPnPLControlNavKey
import com.st.flow_demo.FlowDemoMoreInfoNavKey
import com.st.flow_demo.FlowDemoViewModel
import com.st.flow_demo.R
import com.st.flow_demo.FlowDemoFlowCategoriesExampleScreen
import com.st.flow_demo.FlowDemoFlowCategoryExampleScreen
import com.st.flow_demo.FlowDemoFlowDetailScreen
import com.st.flow_demo.FlowDemoFlowExpertEditingScreen
import com.st.flow_demo.FlowDemoFlowIfApplicationCreationScreen
import com.st.flow_demo.FlowDemoFlowSaveScreen
import com.st.flow_demo.FlowDemoFlowUploadScreen
import com.st.flow_demo.FlowDemoFlowsExpertScreen
import com.st.flow_demo.FlowDemoFunctionConfigurationScreen
import com.st.flow_demo.FlowDemoMoreInfoScreen
import com.st.flow_demo.FlowDemoOutputConfigurationScreen
import com.st.flow_demo.FlowDemoPnPLControlScreen
import com.st.flow_demo.FlowDemoSensorConfigurationScreen
import com.st.flow_demo.FlowDemoSensorDetailScreen
import com.st.flow_demo.FlowDemoSensorsScreen
import com.st.ui.theme.Shapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowDemoContent(
    modifier: Modifier,
    viewModel: FlowDemoViewModel
) {
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    val haptic = LocalHapticFeedback.current
    val backState = rememberNavBackStack(FlowDemoFlowCategoriesExampleNavKey)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            if (FlowConfig.FlowTabBar != null) {
                FlowConfig.FlowTabBar?.invoke("Flow creation")
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = 0
                            backState.clear()
                            backState.add(FlowDemoFlowCategoriesExampleNavKey)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_flows),
                                contentDescription = stringResource(id = R.string.navigation_tab_flows)
                            )
                        },
                        text = { Text(text = stringResource(id = R.string.navigation_tab_flows)) },
                    )

                    if (viewModel.isPnPLExported()) {
                        Tab(
                            selected = 1 == selectedIndex,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedIndex = 1
                                backState.clear()
                                backState.add(FlowDemoPnPLControlNavKey)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.pnpl_icon),
                                    contentDescription = "Control"
                                )
                            },
                            text = {
                                Text(
                                    text = viewModel.getRunningFlowFromOptionBytes()
                                        ?: stringResource(id = R.string.navigation_tab_control)
                                )
                            },
                        )
                    }


                    Tab(
                        selected = if (viewModel.isPnPLExported()) {
                            2 == selectedIndex
                        } else {
                            1 == selectedIndex
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = if (viewModel.isPnPLExported())
                                2
                            else
                                1
                            backState.clear()
                            backState.add(FlowDemoSensorsNavKey)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sensor),
                                contentDescription = stringResource(id = R.string.navigation_tab_sensors)
                            )
                        },
                        text = { Text(text = stringResource(id = R.string.navigation_tab_sensors)) },
                    )

                    Tab(
                        selected = if (viewModel.isPnPLExported()) {
                            3 == selectedIndex
                        } else {
                            2 == selectedIndex
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIndex = if (viewModel.isPnPLExported())
                                3
                            else
                                2

                            backState.clear()
                            backState.add(FlowDemoMoreInfoNavKey)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = stringResource(id = R.string.navigation_tab_more)
                            )
                        },
                        text = { Text(text = stringResource(id = R.string.navigation_tab_more)) },
                    )
                }
            }
        }
    ) { paddingValues ->
        Box {
            NavDisplay(
                modifier = Modifier
                    .consumeWindowInsets(paddingValues)
                    .padding(paddingValues),
                backStack = backState,
                onBack = { backState.removeLastOrNull() },
                entryProvider = entryProvider {
                    FlowDemoFlowCategoriesExampleScreen(viewModel, backState)
                    FlowDemoSensorsScreen(viewModel, backState)
                    FlowDemoPnPLControlScreen(viewModel)
                    FlowDemoFlowsExpertScreen(viewModel, backState)
                    FlowDemoMoreInfoScreen(viewModel)
                    FlowDemoFlowUploadScreen(viewModel, backState)
                    FlowDemoFlowExpertEditingScreen(viewModel, backState)
                    FlowDemoFlowDetailScreen(viewModel, backState)
                    FlowDemoFlowIfApplicationCreationScreen(viewModel, backState)
                    FlowDemoFlowSaveScreen(viewModel, backState)
                    FlowDemoSensorConfigurationScreen(viewModel, backState)
                    FlowDemoFunctionConfigurationScreen(viewModel, backState)
                    FlowDemoOutputConfigurationScreen(viewModel, backState)
                    FlowDemoSensorDetailScreen(viewModel, backState)
                    FlowDemoFlowCategoryExampleScreen(viewModel, backState)
                },
                transitionSpec = {
                    // Slide in from right when navigating forward
                    slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { -it })
                },
                popTransitionSpec = {
                    // Slide in from left when navigating back
                    slideInHorizontally(
                        initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                },
                predictivePopTransitionSpec = {
                    // Slide in from left when navigating back
                    slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                }
            )
        }
    }
}