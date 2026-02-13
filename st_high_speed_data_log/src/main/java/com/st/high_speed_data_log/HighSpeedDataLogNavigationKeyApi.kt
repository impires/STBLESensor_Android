package com.st.high_speed_data_log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.st.high_speed_data_log.composable.HsdlSensors
import com.st.high_speed_data_log.composable.HsdlTags
import com.st.ui.composables.CommandRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data object HsdlSensorsNavKey: NavKey

@Serializable
data object HsdlTagsNavKey: NavKey

@Composable
fun EntryProviderScope<NavKey>.HsdlTagsScreen(
    lazyState: LazyListState,
    tags: List<ComponentWithInterface>,
    status: List<JsonObject>,
    isLoading: Boolean,
    onValueChange: (String, Pair<String, Any>) -> Unit,
    onSendCommand: (String, CommandRequest?) -> Unit
) {
    entry<HsdlTagsNavKey> {
        HsdlTags(
            state = lazyState,
            tags = tags,
            status = status,
            isLoading = isLoading,
            onValueChange = onValueChange,
            onSendCommand = onSendCommand
        )
    }
}

@Composable
fun EntryProviderScope<NavKey>.HsdlSensorsScreen(
    isLogging: Boolean,
    lazyState: LazyListState,
    sensors: List<ComponentWithInterface>,
    status: List<JsonObject>,
    isLoading: Boolean,
    onValueChange: (String, Pair<String, Any>) -> Unit,
    onAfterUcf: () -> Unit,
    onBeforeUcf: () -> Unit,
    onErrorUcf: (String) -> Unit,
    onSendCommand: (String, CommandRequest?) -> Unit
) {
    entry<HsdlSensorsNavKey> {
        if (isLogging.not()) {
            HsdlSensors(
                state = lazyState,
                sensors = sensors,
                status = status,
                isLoading = isLoading,
                onValueChange = onValueChange,
                onAfterUcf = onAfterUcf,
                onBeforeUcf = onBeforeUcf,
                onErrorUcf = onErrorUcf,
                onSendCommand = onSendCommand
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.st_hsdl_logging))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}