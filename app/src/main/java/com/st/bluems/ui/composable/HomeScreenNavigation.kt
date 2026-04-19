package com.st.bluems.ui.composable

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.st.bluems.NFCConnectionViewModel
import com.st.bluems.ui.home.HomeViewModel
import com.st.catalog.CatalogScreen
import com.st.catalog.CatalogViewModel
import com.st.core.GlobalConfig
import com.st.demo_showcase.ui.DemoShowCaseViewModel
import com.st.demo_showcase.ui.composable.DemoShowCaseNavKeyScreen
import com.st.licenses.composable.LicensesScreen
import com.st.ui.theme.BlueMSTheme
import com.st.ui.theme.Grey0
import com.st.user_profiling.ProfileViewModel
import com.st.user_profiling.composable.UserProfilingNavigationScreen
import kotlinx.serialization.Serializable
import kotlin.collections.removeLastOrNull
import com.st.bluems.multinode.MultiNodeViewModel

@Composable
fun HomeScreenNavigation(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    nfcViewModel: NFCConnectionViewModel,
    isBleEnabled: Boolean,
    onEnableBle: () -> Unit,
    isLocationEnable: Boolean,
    onEnableLocation: () -> Unit
) {
    val backState =
        rememberNavBackStack(HomeScreenNavKey)

    GlobalConfig.removeBackState = {
        if(backState.size>1) {
            backState.removeLastOrNull()
        }
    }

    BlueMSTheme {
        NavDisplay(
            modifier = Modifier
                .background(
                    Grey0
                ),
            backStack = backState,
            onBack = {
                if (backState.size > 1) {
                    backState.removeLastOrNull()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<HomeScreenNavKey> {
                    val multiNodeViewModel: MultiNodeViewModel = hiltViewModel()

                    DeviceListScreenNavigation(
                        modifier = modifier,
                        backState = backState,
                        viewModel = viewModel,
                        nfcViewModel = nfcViewModel,
                        multiNodeViewModel = multiNodeViewModel,
                        isBleEnabled = isBleEnabled,
                        onEnableBle = onEnableBle,
                        isLocationEnable = isLocationEnable,
                        onEnableLocation = onEnableLocation
                    )
                }

                entry<DemoShowCaseNavKey> { key ->
//                    AndroidFragment<DemoShowCase>(
//                        arguments = bundleOf(Pair(ARG_NODE_ID, viewModel.currentNodeId))
//                    )
                    val demoViewModel: DemoShowCaseViewModel = hiltViewModel()
                    DemoShowCaseNavKeyScreen(viewModel = demoViewModel, nodeId = key.nodeId)
                }

                entry<LicensesNavKey> {
                    LicensesScreen(onBack = {
                        backState.removeLastOrNull()
                    })
                }

                entry<CatalogNavKey> {
                    val catalogViewModel: CatalogViewModel = hiltViewModel()
                    CatalogScreen(
                        viewModel = catalogViewModel,
                        onCloseCatalog = { backState.removeLastOrNull() })
                }

                entry<HomeScreenProfileNavKey> {
                    val viewModel: ProfileViewModel = viewModel()
                    UserProfilingNavigationScreen(
                        viewModel = viewModel,
                        onCloseUserProfiling = { backState.removeLastOrNull() }
                    )
                }
            })
    }
}


@Serializable
data object HomeScreenNavKey : NavKey

@Serializable
data class DemoShowCaseNavKey(val nodeId: String) : NavKey

@Serializable
data object LicensesNavKey : NavKey

@Serializable
data object CatalogNavKey : NavKey

@Serializable
data object HomeScreenProfileNavKey : NavKey