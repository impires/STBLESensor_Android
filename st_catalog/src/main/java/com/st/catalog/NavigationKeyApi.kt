package com.st.catalog

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.st.catalog.composable.BoardScreen
import com.st.catalog.composable.CatalogList
import com.st.catalog.composable.FirmwareList
import com.st.ui.theme.LocalDimensions
import kotlinx.serialization.Serializable

@Serializable
data object CatalogListNavKey : NavKey

@Serializable
data class BoardDetailsNavKey(val boardPart: String) : NavKey

@Serializable
data class FirmwaresListNavKey(val boardPart: String) : NavKey

@Composable
fun EntryProviderScope<NavKey>.FirmwaresListScreen(
    backState: NavBackStack<NavKey>,
    viewModel: CatalogViewModel
) {
    entry<FirmwaresListNavKey>(
        metadata = NavDisplay.transitionSpec {
            slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(800)
            ) togetherWith ExitTransition.KeepUntilTransitionsFinished
            //togetherWith  ExitTransition.None
            //togetherWith  ExitTransition.KeepUntilTransitionsFinished
            //togetherWith fadeOut(animationSpec = tween(800))
        } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(
                //fadeIn(animationSpec = tween(800)) togetherWith slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(800)
            )
        } + NavDisplay.predictivePopTransitionSpec {
            EnterTransition.None togetherWith slideOutVertically(
                //fadeIn(animationSpec = tween(800)) togetherWith slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(800)
            )
        }
    ) { key ->
        FirmwareList(
            boardPart = key.boardPart,
            backStack = backState,
            viewModel = viewModel
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.BoardScreen(
    viewModel: CatalogViewModel,
    backState: NavBackStack<NavKey>
) {
    entry<BoardDetailsNavKey> { key ->
        val boardId: String
        val boardPart = key.boardPart
        try {
            boardId =
                viewModel.boardsDescription.value.first { it.boardPart == boardPart }.bleDevId
        } catch (e: NoSuchElementException) {
            Text("boardPart =$boardPart not recognized")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LocalDimensions.current.paddingMedium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "boardPart = [$boardPart] not recognized"
                )
            }
            return@entry
        }
        BoardScreen(
            boardId = boardId,
            boardPart = boardPart,
            backStack = backState,
            viewModel = viewModel
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.CatalogListScreen(
    backState: NavBackStack<NavKey>,
    viewModel: CatalogViewModel,
    onCloseCatalog: () -> Unit
) {
    entry<CatalogListNavKey> {
        CatalogList(
            backStack = backState,
            viewModel = viewModel,
            onBack = onCloseCatalog
        )
    }
}