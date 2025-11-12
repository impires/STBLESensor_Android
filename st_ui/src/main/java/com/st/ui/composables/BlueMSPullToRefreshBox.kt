package com.st.ui.composables

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.st.ui.theme.Grey10
import com.st.ui.theme.NotActiveColor
import com.st.ui.theme.PrimaryYellow

@Composable
@ExperimentalMaterial3Api
fun BlueMSPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    //contentAlignment: Alignment = Alignment.TopStart,
    //indicatorAlignment: Alignment = Alignment.TopCenter,
    isBetaRelease: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {

//    Box(
//        modifier.pullToRefresh(state = state, isRefreshing = isRefreshing, onRefresh = onRefresh),
//        contentAlignment = contentAlignment
//    ) {
//        content()
//
//        Indicator(
//            modifier = Modifier.align(indicatorAlignment),
//            isRefreshing = isRefreshing,
//            state = state
//        )
//    }
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = if(isBetaRelease) PrimaryYellow else NotActiveColor,
                color = Grey10,
                state = state
            )
        },
    ) {
        content()
    }
}