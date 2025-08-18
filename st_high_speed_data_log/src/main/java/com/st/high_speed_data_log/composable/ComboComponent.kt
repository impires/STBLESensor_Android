package com.st.high_speed_data_log.composable

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.st.blue_sdk.board_catalog.models.DtmiContent
import com.st.high_speed_data_log.ComponentWithInterface
import com.st.high_speed_data_log.HsdlConfig
import com.st.pnpl.composable.Component
import com.st.pnpl.util.imageResource
import com.st.ui.R
import com.st.ui.composables.CommandRequest
import com.st.ui.theme.BlueMSTheme
import com.st.ui.theme.Grey6
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryBlue
import com.st.ui.theme.Shapes
import com.st.ui.utils.localizedDisplayNameSensor
import kotlinx.serialization.json.JsonObject

@Composable
fun ComboComponent(
    modifier: Modifier = Modifier,
    filterMountedSensor: Map.Entry<String, List<ComponentWithInterface>>,
    isLoading: Boolean,
    isOpen: Boolean,
    showNotMounted: Boolean = true,
    status: List<JsonObject>,
    onValueChange: (String, Pair<String, Any>) -> Unit,
    onBeforeUcf: () -> Unit,
    onAfterUcf: () -> Unit,
    onErrorUcf: (String) -> Unit,
    onSendCommand: (String, CommandRequest?) -> Unit,
    onOpenComboComponent: (String) -> Unit
) {
    var isOpenComboComponent by remember(filterMountedSensor,isOpen) { mutableStateOf(value = "") }

    val iconsList = filterMountedSensor.value.map { componentWithInterface ->
        when (componentWithInterface.first.contentType) {
            DtmiContent.DtmiComponentContent.ContentType.SENSOR ->
                componentWithInterface.first.sensorType.imageResource

            DtmiContent.DtmiComponentContent.ContentType.ALGORITHM ->
                R.drawable.sensor_type_algorithm

            DtmiContent.DtmiComponentContent.ContentType.ACTUATORS ->
                R.drawable.actuator_type_class

            else -> R.drawable.ic_component_info
        }
    }.chunked(2)

    val subTitle = filterMountedSensor.value.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ", "
    ) { componentWithInterface ->
        when (componentWithInterface.first.contentType) {
            DtmiContent.DtmiComponentContent.ContentType.SENSOR -> {
                if (HsdlConfig.isVespucci) {
                    //Try to understand if there is the Low/High indication
                    val sensorName =
                        componentWithInterface.first.displayName.localizedDisplayNameSensor(
                            isVespucci = true
                        ).split(" ")
                    if (sensorName.size == 2) {
                        "${componentWithInterface.first.sensorType.name} ${sensorName.last()}"
                    } else {
                        componentWithInterface.first.sensorType.name
                    }
                } else {
                    componentWithInterface.first.sensorType.name
                }
            }

            DtmiContent.DtmiComponentContent.ContentType.ALGORITHM ->
                "algorithm"

            DtmiContent.DtmiComponentContent.ContentType.ACTUATORS ->
                "actuators"

            else -> "info"
        }
    }

    ComboComponentHeader(
        modifier = modifier,
        title = filterMountedSensor.key,
        subTitle = subTitle,
        iconsList = iconsList,
        isOpen = isOpen,
        onOpenComboComponent = onOpenComboComponent,
        content = {
            filterMountedSensor.value.forEach { componentWithInterface ->
                val name = componentWithInterface.first.name
                val data = (status.find { it.containsKey(name) })?.get(name)
                data?.let {
                    Component(
                        modifier = modifier.padding(
                            start = LocalDimensions.current.paddingNormal,
                            end = LocalDimensions.current.paddingNormal
                        ),
                        name = name,
                        data = data,
                        enabled = isLoading.not(),
                        enableCollapse = true,
                        isOpen = isOpenComboComponent == name,
                        isVespucci = HsdlConfig.isVespucci,
                        showNotMounted = showNotMounted,
                        componentModel = componentWithInterface.first,
                        interfaceModel = componentWithInterface.second,
                        onValueChange = { onValueChange(name, it) },
                        onSendCommand = { onSendCommand(name, it) },
                        onBeforeUcf = onBeforeUcf,
                        onAfterUcf = onAfterUcf,
                        onErrorUcf = onErrorUcf,
                        onOpenComponent = {
                            isOpenComboComponent = if (it == isOpenComboComponent) "" else it
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ComboComponentHeader(
    modifier: Modifier = Modifier,
    title: String,
    isOpen: Boolean,
    subTitle: String? = null,
    iconsList: List<List<Int>>,
    onOpenComboComponent: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.small,
        color = if (isOpen)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surface,
        shadowElevation = LocalDimensions.current.elevationNormal,
        onClick = { onOpenComboComponent(title) }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            //verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingMedium),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalDimensions.current.paddingNormal),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(
                    modifier = Modifier
                        .border(
                            border = BorderStroke(1.dp, PrimaryBlue),
                            shape = Shapes.small
                        )
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingSmall)
                ) {
                    iconsList.forEach { iconRow ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingSmall)
                        ) {
                            iconRow.forEach { icon ->
                                Icon(
                                    modifier = Modifier.size(LocalDimensions.current.iconSmall),
                                    painter = painterResource(id = icon),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 18.sp,
                    )

                    subTitle?.let {
                        Text(
                            modifier = Modifier.basicMarquee(),
                            text = subTitle,
                            color = Grey6,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }

                if (isOpen) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
            if (isOpen) {
                content()
            }
        }
    }
}

/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun ComboComponentHeaderPreview2() {
    BlueMSTheme {
        ComboComponentHeader(
            title = "ComboComponent2",
            isOpen = false,
            onOpenComboComponent = {},
            iconsList = listOf(
                listOf(
                    R.drawable.sensor_type_accelerometer,
                    R.drawable.sensor_type_tmos
                )
            ),
        ) {
            Text("Content")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComboComponentHeaderPreview3LongSubtitle() {
    BlueMSTheme {
        ComboComponentHeader(
            title = "ComboComponent3",
            subTitle = "[acc, tmos, mlc, bla, bla, bla, bla, bla, bla]",
            isOpen = true,
            onOpenComboComponent = {},
            iconsList = listOf(
                listOf(
                    R.drawable.sensor_type_accelerometer,
                    R.drawable.sensor_type_tmos
                ), listOf(R.drawable.sensor_type_mlc)
            ),
        ) {
            Text("Content")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComboComponentHeaderPreview3ShortSubtitle() {
    BlueMSTheme {
        ComboComponentHeader(
            title = "ComboComponent3",
            subTitle = "[acc, tmos]",
            isOpen = false,
            onOpenComboComponent = {},
            iconsList = listOf(
                listOf(
                    R.drawable.sensor_type_accelerometer,
                    R.drawable.sensor_type_tmos
                ), listOf(R.drawable.sensor_type_mlc)
            ),
        ) {
            Text("Content")
        }
    }
}