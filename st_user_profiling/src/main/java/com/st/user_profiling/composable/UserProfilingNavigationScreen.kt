package com.st.user_profiling.composable

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.st.ui.theme.Grey0
import com.st.user_profiling.ProfileViewModel
import kotlinx.serialization.Serializable
import kotlin.collections.removeLastOrNull

@Composable
fun UserProfilingNavigationScreen(
    viewModel: ProfileViewModel,
    onCloseUserProfiling: () -> Unit = { /** NOOP**/ }
) {
    val backState =
        rememberNavBackStack(LevelProficiencyNav)

    NavDisplay(
        modifier = Modifier
            .background(
                Grey0
            ),
        backStack = backState,
        onBack = {
            backState.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            LevelSelectionNavScreen(viewModel, backState)
            ProfileSelectionNavScreen(viewModel, backState, onCloseUserProfiling)
        }
    )

}

@Composable
private fun EntryProviderScope<NavKey>.ProfileSelectionNavScreen(
    viewModel: ProfileViewModel,
    backState: NavBackStack<NavKey>,
    onCloseUserProfiling: () -> Unit = { /** NOOP**/ }
) {
    entry<UserProfileNavKey> {
        ProfileSelectionScreen(
            viewModel = viewModel,
            backState = backState,
            onCloseUserProfiling = onCloseUserProfiling
        )
    }
}

@Composable
private fun EntryProviderScope<NavKey>.LevelSelectionNavScreen(
    viewModel: ProfileViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<LevelProficiencyNav> {
        LevelProficiencyScreen(
            viewModel = viewModel,
            backState = backState
        )
    }
}

@Serializable
data object UserProfileNavKey : NavKey

@Serializable
data object LevelProficiencyNav : NavKey