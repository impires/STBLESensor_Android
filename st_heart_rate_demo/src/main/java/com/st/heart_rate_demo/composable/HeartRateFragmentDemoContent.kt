package com.st.heart_rate_demo.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.st.heart_rate_demo.HeartRateDemoViewModel
import com.st.heart_rate_demo.R
import com.st.heart_rate_demo.utils.BlueMSPlotEntry
import com.st.ui.theme.ErrorText
import com.st.ui.theme.Grey0
import com.st.ui.theme.Grey5
import com.st.ui.theme.InfoText
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.Shapes
import com.st.ui.theme.SuccessText
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

private val SECONDS_TO_PLOT_DEFAULT = 10.seconds
private const val NUMBER_LINES = 6

@OptIn(ExperimentalTime::class)
@Composable
fun HeartRateFragmentDemoContent(
    modifier: Modifier = Modifier,
    viewModel: HeartRateDemoViewModel
) {

    val locationData by viewModel.locationData.collectAsStateWithLifecycle()
    val heartData by viewModel.heartData.collectAsStateWithLifecycle()

    var mFirstNotificationTimeStamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var heartRateData: List<BlueMSPlotEntry> by remember {
        mutableStateOf(
            listOf()
        )
    }
    var maxHeartRateY by remember { mutableFloatStateOf(-Float.MAX_VALUE) }
    var minHeartRateY by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    var energyData: List<BlueMSPlotEntry> by remember {
        mutableStateOf(
            listOf()
        )
    }
    var maxEnergyY by remember { mutableFloatStateOf(-Float.MAX_VALUE) }
    var minEnergyY by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    val textMeasurerHeartRate = rememberTextMeasurer()
    val textMeasurerEnergy = rememberTextMeasurer()


    LaunchedEffect(key1 = heartData) {
        heartData.first?.let { data ->
            if (data.heartRate.value > 0) {
                val actualTimeStamp = System.currentTimeMillis()
                val localHeartData = heartRateData.toMutableList()
                localHeartData.add(
                    BlueMSPlotEntry(
                        actualTimeStamp - mFirstNotificationTimeStamp,
                        data.heartRate.value.toFloat()
                    )
                )

                heartRateData = removeEntryOlderThan(
                    localHeartData.toList(),
                    SECONDS_TO_PLOT_DEFAULT
                )

                //Compute the Max and the Min
                maxHeartRateY = -Float.MAX_VALUE
                minHeartRateY = Float.MAX_VALUE
                heartRateData.forEach { data ->
                    val y = data.y
                    if (y < minHeartRateY)
                        minHeartRateY = y
                    if (y > maxHeartRateY)
                        maxHeartRateY = y
                }
            }

            if (data.energyExpended.value > 0) {
                val actualTimeStamp = System.currentTimeMillis()
                val localEnergyData = energyData.toMutableList()
                localEnergyData.add(
                    BlueMSPlotEntry(
                        actualTimeStamp - mFirstNotificationTimeStamp,
                        data.energyExpended.value.toFloat()
                    )
                )
                energyData = removeEntryOlderThan(
                    localEnergyData.toList(),
                    SECONDS_TO_PLOT_DEFAULT
                )

                //Compute the Max and the Min
                maxEnergyY = -Float.MAX_VALUE
                minEnergyY = Float.MAX_VALUE
                energyData.forEach { data ->
                    val y = data.y
                    if (y < minEnergyY)
                        minEnergyY = y
                    if (y > maxEnergyY)
                        maxEnergyY = y
                }
            }
        }
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = LocalDimensions.current.paddingLarge)
    ) {
        //Heart Rate Position & Parameter
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.25f),
            shape = Shapes.small,
            shadowElevation = LocalDimensions.current.elevationNormal
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalDimensions.current.paddingNormal),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    LocalDimensions.current.paddingSmall
                )
            ) {
                BodySensorLocationView(location = locationData.bodySensorLocation.value)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(
                        LocalDimensions.current.paddingSmall
                    )
                ) {
                    Text(
                        text = "Skin Contact",
                        style = MaterialTheme.typography.titleSmall
                    )

                    heartData.first?.let { data ->
                        Row(
                            modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (data.skinContactDetected.value) {
                                Icon(
                                    modifier = Modifier
                                        .size(
                                            LocalDimensions.current.iconSmall
                                        ),
                                    painter = painterResource(id = R.drawable.ic_baseline_check_24),
                                    tint = SuccessText,
                                    contentDescription = "Skin Contact"
                                )
                                Text(
                                    modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
                                    style = MaterialTheme.typography.bodySmall,
                                    text = "Yes"
                                )
                            } else {
                                Icon(
                                    modifier = Modifier
                                        .size(
                                            LocalDimensions.current.iconSmall
                                        ),
                                    painter = painterResource(id = R.drawable.ic_baseline_close_24),
                                    tint = ErrorText,
                                    contentDescription = "Skin Contact"
                                )
                                Text(
                                    modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
                                    style = MaterialTheme.typography.bodySmall,
                                    text = "No"
                                )
                            }
                        }
                    }

                    Text(
                        text = "Position",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
                        text = locationData.bodySensorLocation.value.name,
                        style = MaterialTheme.typography.bodySmall
                    )


                    Text(
                        text = "RR Interval",
                        style = MaterialTheme.typography.titleSmall
                    )

                    heartData.first?.let { data ->
                        Row(
                            modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!data.rrInterval.value.isNaN()) {
                                Icon(
                                    modifier = Modifier
                                        .size(
                                            LocalDimensions.current.iconSmall
                                        ),
                                    painter = painterResource(id = R.drawable.ic_baseline_check_24),
                                    tint = SuccessText,
                                    contentDescription = "RR interval"
                                )
                                Text(
                                    modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
                                    style = MaterialTheme.typography.bodySmall,
                                    text = String.format(
                                        Locale.getDefault(),
                                        "%1.2f %s",
                                        data.rrInterval.value,
                                        data.rrInterval.unit
                                    )
                                )
                            } else {
                                Icon(
                                    modifier = Modifier
                                        .size(
                                            LocalDimensions.current.iconSmall
                                        ),
                                    painter = painterResource(id = R.drawable.ic_baseline_block_24),
                                    tint = ErrorText,
                                    contentDescription = "RR interval"
                                )
                                Text(
                                    modifier = Modifier.padding(start = LocalDimensions.current.paddingNormal),
                                    style = MaterialTheme.typography.bodySmall,
                                    text = "Not Supported"
                                )
                            }
                        }
                    }
                }
            }
        }

        //Heart Rate Plot
        heartData.first?.let { data ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                shape = Shapes.small,
                shadowElevation = LocalDimensions.current.elevationNormal
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LocalDimensions.current.paddingNormal),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        LocalDimensions.current.paddingNormal
                    )
                ) {
                    AnimatedContent(
                        targetState = heartData.second,
                        label = "heart rate Animation"
                    ) { _ ->
                        val value = data.heartRate.value
                        Column(
                            modifier = Modifier.width(LocalDimensions.current.imageNormal),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(LocalDimensions.current.imageNormal),
                                painter = painterResource(id = R.drawable.ic_heart_rate),
                                tint = if (value < 0) {
                                    Grey5
                                } else {
                                    ErrorText
                                },
                                contentDescription = "Heart"
                            )
                            Text(
                                text = if (value < 0) {
                                    ""
                                } else {
                                    String.format(
                                        Locale.getDefault(),
                                        "%d %s",
                                        value,
                                        data.heartRate.unit
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(2f)
                    ) {
                        if (heartRateData.isNotEmpty()) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 40.dp, vertical = 20.dp)
                            ) {
                                val width = size.width
                                val height = size.height
                                val minValue = if (heartRateData.size < 2) {
                                    2
                                } else {
                                    heartRateData.size
                                }
                                val spacing = width / (minValue - 1)
                                val labelLineSpacing = height / (NUMBER_LINES)

                                // Draw Horizontal Grid Lines
                                for (i in 0..NUMBER_LINES) {

                                    val y = height - labelLineSpacing * i

                                    val labelValue =
                                        minHeartRateY + ((maxHeartRateY - minHeartRateY) / NUMBER_LINES) * i

                                    // Draw Grid Lines
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )


                                    // Draw Y-Axis Label (Text)
                                    drawText(
                                        textMeasurer = textMeasurerEnergy,
                                        text = labelValue.toInt().toString(),
                                        topLeft = Offset(-35.dp.toPx(), y - 10.dp.toPx()),
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    )
                                }
                                if (heartRateData.isNotEmpty()) {
                                    val path = Path().apply {
                                        heartRateData.forEachIndexed { index, value ->
                                            // Calculate X and Y coordinates
                                            // We flip Y because (0,0) is the top-left in Canvas
                                            val x = index * spacing
                                            val y =
                                                height - (((value.y - minHeartRateY) / (maxHeartRateY - minHeartRateY)) * height)

                                            if (index == 0) moveTo(x, y) else lineTo(x, y)
                                        }
                                    }

                                    drawPath(
                                        path = path,
                                        color = ErrorText,
                                        style = Stroke(width = 2.dp.toPx())
                                    )

                                    //Plot the circles
                                    heartRateData.forEachIndexed { index, value ->
                                        // Calculate X and Y coordinates
                                        // We flip Y because (0,0) is the top-left in Canvas
                                        val x = index * spacing
                                        val y =
                                            height - (((value.y - minHeartRateY) / (maxHeartRateY - minHeartRateY)) * height)

                                        drawCircle(
                                            color = ErrorText,
                                            radius = 10f,
                                            center = Offset(x = x, y = y)
                                        )
                                        drawCircle(
                                            color = Grey0,
                                            radius = 6f,
                                            center = Offset(x = x, y = y)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            //Energy Plot
            if (data.energyExpended.value != -1) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f),
                    shape = Shapes.small,
                    shadowElevation = LocalDimensions.current.elevationNormal
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LocalDimensions.current.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            LocalDimensions.current.paddingNormal
                        )
                    ) {
                        AnimatedContent(
                            targetState = data.energyExpended.value,
                            label = "Energy Animation"
                        ) { value ->
                            Column(
                                modifier = Modifier.width(LocalDimensions.current.imageNormal),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(LocalDimensions.current.imageNormal),
                                    painter = painterResource(id = R.drawable.ic_calories),
                                    tint = if (value < 0) {
                                        Grey5
                                    } else {
                                        InfoText
                                    },
                                    contentDescription = "Energy"
                                )
                                Text(
                                    text = if (value < 0) {
                                        ""
                                    } else {
                                        String.format(
                                            Locale.getDefault(),
                                            "%d %s",
                                            value,
                                            data.energyExpended.unit
                                        )
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(2f)
                        ) {
                            if (energyData.isNotEmpty()) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 40.dp, vertical = 20.dp)
                                ) {
                                    val width = size.width
                                    val height = size.height
                                    val minValue = if (energyData.size < 2) {
                                        2
                                    } else {
                                        energyData.size
                                    }
                                    val spacing = width / (minValue - 1)
                                    val labelLineSpacing = height / (NUMBER_LINES)

                                    // Draw Horizontal Grid Lines
                                    for (i in 0..NUMBER_LINES) {

                                        val y = height - labelLineSpacing * i

                                        val labelValue =
                                            minEnergyY + ((maxEnergyY - minEnergyY) / NUMBER_LINES) * i

                                        // Draw Grid Lines
                                        drawLine(
                                            color = Color.LightGray.copy(alpha = 0.5f),
                                            start = Offset(0f, y),
                                            end = Offset(width, y),
                                            strokeWidth = 1.dp.toPx()
                                        )


                                        // Draw Y-Axis Label (Text)
                                        drawText(
                                            textMeasurer = textMeasurerHeartRate,
                                            text = labelValue.toInt().toString(),
                                            topLeft = Offset(-35.dp.toPx(), y - 10.dp.toPx()),
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        )
                                    }
                                    if (energyData.isNotEmpty()) {
                                        val path = Path().apply {
                                            energyData.forEachIndexed { index, value ->
                                                // Calculate X and Y coordinates
                                                // We flip Y because (0,0) is the top-left in Canvas
                                                val x = index * spacing
                                                val y =
                                                    height - (((value.y - minEnergyY) / (maxEnergyY - minEnergyY)) * height)

                                                if (index == 0) moveTo(x, y) else lineTo(x, y)
                                            }
                                        }

                                        drawPath(
                                            path = path,
                                            color = InfoText,
                                            style = Stroke(width = 2.dp.toPx())
                                        )

                                        //Plot the circles
                                        energyData.forEachIndexed { index, value ->
                                            // Calculate X and Y coordinates
                                            // We flip Y because (0,0) is the top-left in Canvas
                                            val x = index * spacing
                                            val y =
                                                height - (((value.y - minEnergyY) / (maxEnergyY - minEnergyY)) * height)

                                            drawCircle(
                                                color = InfoText,
                                                radius = 10f,
                                                center = Offset(x = x, y = y)
                                            )

                                            drawCircle(
                                                color = Grey0,
                                                radius = 6f,
                                                center = Offset(x = x, y = y)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun removeEntryOlderThan(
    list: List<BlueMSPlotEntry>,
    timeRange: Duration?
): List<BlueMSPlotEntry> {
    if (timeRange == null)
        return list
    var xMax = Long.MIN_VALUE
    var xMin = Long.MAX_VALUE

    list.forEach { data ->
        if (data.x > xMax) {
            xMax = data.x
        }
        if (data.x < xMin) {
            xMin = data.x
        }
    }

    val plotRangeMs = (xMax - xMin).toDouble().milliseconds
    if (plotRangeMs > timeRange) {
        val minValidX = (xMax - timeRange.toDouble(DurationUnit.MILLISECONDS)).toFloat()
        val newList = list.filter { it.x >= minValidX }
        return newList
    } else {
        return list
    }
}
