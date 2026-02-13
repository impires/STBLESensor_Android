package com.st.flow_demo

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.st.flow_demo.composable.common.FlowDemoFlowDetailScreen
import com.st.flow_demo.composable.common.FlowDemoFlowUploadScreen
import com.st.flow_demo.composable.custom_flow.FlowDemoFlowExpertEditingScreen
import com.st.flow_demo.composable.custom_flow.FlowDemoFlowIfApplicationCreationScreen
import com.st.flow_demo.composable.custom_flow.FlowDemoFlowSaveScreen
import com.st.flow_demo.composable.custom_flow.FlowDemoFlowsExpertScreen
import com.st.flow_demo.composable.custom_flow.FlowDemoFunctionConfigurationScreen
import com.st.flow_demo.composable.custom_flow.FlowDemoOutputConfigurationScreen
import com.st.flow_demo.composable.custom_flow.FlowDemoSensorConfigurationScreen
import com.st.flow_demo.composable.example_flow.FlowDemoFlowCategoriesExampleScreen
import com.st.flow_demo.composable.example_flow.FlowDemoFlowCategoryExampleScreen
import com.st.flow_demo.composable.example_flow.FlowDemoPnPLControlScreen
import com.st.flow_demo.composable.more_info.FlowDemoMoreInfoScreen
import com.st.flow_demo.composable.sensor_screen.FlowDemoSensorDetailScreen
import com.st.flow_demo.composable.sensor_screen.FlowDemoSensorsScreen
import com.st.ui.theme.LocalDimensions
import kotlinx.serialization.Serializable

@Serializable
data object FlowDemoFlowCategoriesExampleNavKey : NavKey

@Serializable
data object FlowDemoSensorsNavKey : NavKey

@Serializable
data object FlowDemoPnPLControlNavKey : NavKey

@Serializable
data object FlowDemoFlowsExpertNavKey : NavKey

@Serializable
data object FlowDemoMoreInfoNavKey : NavKey

@Serializable
data object FlowDemoFlowUploadNavKey : NavKey

@Serializable
data object FlowDemoFlowExpertEditingNavKey : NavKey

@Serializable
data object FlowDemoFlowDetailNavKey : NavKey

@Serializable
data object FlowDemoFlowIfApplicationCreationNavKey : NavKey

@Serializable
data object FlowDemoFlowSaveNavKey : NavKey

@Serializable
data object FlowDemoSensorConfigurationNavKey : NavKey

@Serializable
data object FlowDemoFunctionConfigurationNavKey : NavKey

@Serializable
data object FlowDemoOutputConfigurationNavKey : NavKey

@Serializable
data class FlowDemoSensorDetailNavKey(val sensorId: String) : NavKey

@Serializable
data class FlowDemoFlowCategoryExampleNavKey(val category: String) : NavKey

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowCategoryExampleScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowCategoryExampleNavKey> { key ->
        FlowDemoFlowCategoryExampleScreen(
            viewModel = viewModel,
            category = key.category,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoSensorDetailScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoSensorDetailNavKey> { key ->
        FlowDemoSensorDetailScreen(
            viewModel = viewModel,
            sensorId = key.sensorId,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoOutputConfigurationScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoOutputConfigurationNavKey> {
        FlowDemoOutputConfigurationScreen(
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            viewModel = viewModel,
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFunctionConfigurationScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFunctionConfigurationNavKey> {
        FlowDemoFunctionConfigurationScreen(
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            viewModel = viewModel,
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoSensorConfigurationScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoSensorConfigurationNavKey> {
        FlowDemoSensorConfigurationScreen(
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            viewModel = viewModel,
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowSaveScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowSaveNavKey> {
        FlowDemoFlowSaveScreen(
            viewModel = viewModel,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowIfApplicationCreationScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowIfApplicationCreationNavKey> {
        FlowDemoFlowIfApplicationCreationScreen(
            viewModel = viewModel,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowDetailScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowDetailNavKey> {
        FlowDemoFlowDetailScreen(
            viewModel = viewModel,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowExpertEditingScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowExpertEditingNavKey> {
        FlowDemoFlowExpertEditingScreen(
            viewModel = viewModel,
            backState = backState,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            )
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowUploadScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowUploadNavKey> {
        FlowDemoFlowUploadScreen(
            viewModel = viewModel,
            backState = backState,
            paddingValues = PaddingValues(all = LocalDimensions.current.paddingNormal)
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoMoreInfoScreen(
    viewModel: FlowDemoViewModel
) {
    entry<FlowDemoMoreInfoNavKey> {
        FlowDemoMoreInfoScreen(
            viewModel = viewModel,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            )
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowsExpertScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowsExpertNavKey> {
        FlowDemoFlowsExpertScreen(
            viewModel = viewModel,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            ),
            backState = backState
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoPnPLControlScreen(
    viewModel: FlowDemoViewModel
) {
    entry<FlowDemoPnPLControlNavKey> {
        FlowDemoPnPLControlScreen(
            viewModel = viewModel,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            )
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoSensorsScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoSensorsNavKey> {
        FlowDemoSensorsScreen(
            viewModel = viewModel,
            backState = backState,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            )
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.FlowDemoFlowCategoriesExampleScreen(
    viewModel: FlowDemoViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<FlowDemoFlowCategoriesExampleNavKey> {
        FlowDemoFlowCategoriesExampleScreen(
            viewModel = viewModel,
            backState = backState,
            paddingValues = PaddingValues(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            )
        )
    }
}