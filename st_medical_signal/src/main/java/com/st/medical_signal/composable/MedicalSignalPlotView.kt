package com.st.medical_signal.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.st.blue_sdk.features.extended.medical_signal.MedicalInfo
import com.st.ui.theme.ErrorText
import com.st.ui.theme.Grey10
import com.st.ui.theme.Grey2
import com.st.ui.theme.InfoText
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryPink
import com.st.ui.theme.SecondaryBlue
import com.st.ui.theme.Shapes
import com.st.ui.theme.SuccessText
import kotlin.collections.toFloatArray
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

private val mColors: Array<Color> = arrayOf(
    InfoText,
    ErrorText,
    SuccessText,
    Grey10,
    SecondaryBlue,
    PrimaryPink
)

private data class BlueMSPlotEntry(
    val x: Long, val y: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlueMSPlotEntry

        if (x != other.x) return false
        if (!y.contentEquals(other.y)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.contentHashCode()
        return result
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun MedicalSignalPlotView(
    modifier: Modifier = Modifier,
    featureUpdate: MutableList<MedicalInfo>,
    featureTime: Int,
    resetZoomTime: Long,
    type: String
) {
    val featureDescription by remember(key1 = featureTime) {
        derivedStateOf {
            if (featureUpdate.isNotEmpty()) {
                var string = featureUpdate.first().sigType.value.description
                if (featureUpdate.first().sigType.value.yMeasurementUnit != null) {
                    string += " [" + featureUpdate.first().sigType.value.yMeasurementUnit + "]"
                }
                string
            } else {
                ""
            }
        }
    }

    var prevMedInfo by remember { mutableStateOf<MedicalInfo?>(null) }

    var firstInternalTimeStamp by remember {
        mutableStateOf<Int?>(null)
    }

    var legend by remember {
        mutableStateOf<Array<String>>(arrayOf())
    }

    var cubicInterpolation by remember { mutableStateOf(false) }

    var dataPoints: List<BlueMSPlotEntry> by remember {
        mutableStateOf(
            listOf()
        )
    }

    var labelYAxis by remember { mutableIntStateOf(7) }

    var maxY by remember { mutableFloatStateOf(-Float.MAX_VALUE) }
    var minY by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    var isAutoScaleMinMaxEnabled by remember { mutableStateOf(true) }
    var showLegend by remember { mutableStateOf(false) }

    var displayWindowTimeSecond by remember { mutableIntStateOf(10) }

    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(key1 = featureTime) {
        val iterator = featureUpdate.iterator()

        featureUpdate.firstOrNull()?.let {
            if (it.sigType.value.nLabels != 0) {
                labelYAxis = it.sigType.value.nLabels
            }

            isAutoScaleMinMaxEnabled = it.sigType.value.isAutoscale
            if (!isAutoScaleMinMaxEnabled) {
                maxY = it.sigType.value.maxGraphValue.toFloat()
                minY = it.sigType.value.minGraphValue.toFloat()
            }

            displayWindowTimeSecond = it.sigType.value.displayWindowTimeSecond

            cubicInterpolation = it.sigType.value.cubicInterpolation
        }

        while (iterator.hasNext()) {
            val update = iterator.next()

            if (prevMedInfo == null) {
                //We draw anything... only save data
                prevMedInfo = update.copy()
                firstInternalTimeStamp = update.internalTimeStamp.value
                if (update.sigType.value.numberOfSignals > 1) {
                    showLegend = true
                    legend = update.sigType.value.signalLabels.toTypedArray()
                } else {
                    showLegend = false
                    legend = arrayOf(
                        update.sigType.value.signalLabels.firstOrNull()
                            ?: update.sigType.value.description
                    )
                }
                showLegend = update.sigType.value.showLegend
            } else {
                val timeDiff =
                    (update.internalTimeStamp.value - prevMedInfo!!.internalTimeStamp.value).toFloat()

                if (timeDiff != 0.0f) { ///????/////
                    //This is the delta time between Samples
                    val deltaBetweenSample =
                        timeDiff * prevMedInfo!!.sigType.value.numberOfSignals / prevMedInfo!!.values.value.size


                    //Fill the data
                    if (prevMedInfo!!.sigType.value.numberOfSignals > 1) {
                        val dataSets =
                            prevMedInfo!!.values.value.chunked(prevMedInfo!!.sigType.value.numberOfSignals)

                        dataSets.forEachIndexed { indexSet, dataSet ->
                            val localDataPoints = dataPoints.toMutableList()
                            localDataPoints.add(
                                BlueMSPlotEntry(
                                    prevMedInfo!!.internalTimeStamp.value + (deltaBetweenSample * indexSet).toLong() - firstInternalTimeStamp!!,
                                    dataSet.map { it.toFloat() }.toFloatArray()
                                )
                            )
                            dataPoints = removeEntryOlderThan(
                                localDataPoints.toList(),
                                displayWindowTimeSecond.seconds
                            )
                        }
                    } else {
                        prevMedInfo!!.values.value.forEachIndexed { index, data ->
                            val localDataPoints = dataPoints.toMutableList()
                            localDataPoints.add(
                                BlueMSPlotEntry(
                                    prevMedInfo!!.internalTimeStamp.value + (deltaBetweenSample * index).toLong() - firstInternalTimeStamp!!,
                                    floatArrayOf(data.toFloat())
                                )
                            )
                            dataPoints = removeEntryOlderThan(
                                localDataPoints.toList(),
                                displayWindowTimeSecond.seconds
                            )
                        }
                    }
                    prevMedInfo = update.copy()
                }
            }
            iterator.remove()
        }

        //Compute the Max and the Min
        if (isAutoScaleMinMaxEnabled) {
            maxY = -Float.MAX_VALUE
            minY = Float.MAX_VALUE
            dataPoints.forEach { data ->
                data.y.forEach { y ->
                    if (y < minY)
                        minY = y
                    if (y > maxY)
                        maxY = y
                }
            }
            if (maxY == minY) {
                maxY += 1
                minY -= 1
            }
        }
    }

    LaunchedEffect(key1 = resetZoomTime) {
        dataPoints = listOf()
        maxY = -Float.MAX_VALUE
        minY = Float.MAX_VALUE
        firstInternalTimeStamp = null
        prevMedInfo = null
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.small,
        shadowElevation = LocalDimensions.current.elevationNormal
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(LocalDimensions.current.paddingNormal),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal)
        ) {
            Text(text = featureDescription, style = MaterialTheme.typography.titleSmall)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
            ) {

                if (dataPoints.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 40.dp, top = 10.dp, bottom = 25.dp, end = 10.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val minValue = if (dataPoints.size < 2) {
                            2
                        } else {
                            dataPoints.size
                        }
                        val spacing = width / (minValue - 1)
                        val labelLineSpacing = height / (labelYAxis)

                        // Draw Horizontal Grid Lines
                        for (i in 0..labelYAxis) {

                            val y = height - labelLineSpacing * i

                            val labelValue = (minY + ((maxY - minY) / labelYAxis) * i).toInt()

                            // Draw Grid Lines
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )


                            // Draw Y-Axis Label (Text)
                            drawText(
                                textMeasurer = textMeasurer,
                                text = "$labelValue",
                                topLeft = Offset(-35.dp.toPx(), y - 10.dp.toPx()),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            )
                        }

                        if (dataPoints.isNotEmpty()) {
                            dataPoints.first().y.indices.forEach { comp ->
                                val path: Path =
                                    if (cubicInterpolation) {
                                        Path().apply {
                                            val firstX = 0 * spacing
                                            val firstY =
                                                height - (((dataPoints[0].y[comp] - minY) / (maxY - minY)) * height)
                                            moveTo(firstX, firstY)

                                            for (index in 1 until dataPoints.size) {
                                                val prevX = (index - 1) * spacing
                                                val prevY =
                                                    height - (((dataPoints[index - 1].y[comp] - minY) / (maxY - minY)) * height)

                                                val currX = index * spacing
                                                val currY =
                                                    height - (((dataPoints[index].y[comp] - minY) / (maxY - minY)) * height)

                                                // Calculate control points for a smooth curve (Cubic Bezier)
                                                // 0.2f to 0.5f is a good intensity range
                                                val intensity = 0.3f
                                                val controlPoint1X =
                                                    prevX + (currX - prevX) * intensity
                                                val controlPoint1Y = prevY

                                                val controlPoint2X =
                                                    currX - (currX - prevX) * intensity
                                                val controlPoint2Y = currY

                                                cubicTo(
                                                    controlPoint1X,
                                                    controlPoint1Y, // Control point 1
                                                    controlPoint2X,
                                                    controlPoint2Y, // Control point 2
                                                    currX,
                                                    currY                   // Destination point
                                                )
                                            }
                                        }
                                    } else {
                                        Path().apply {
                                            dataPoints.forEachIndexed { index, value ->
                                                // Calculate X and Y coordinates
                                                // We flip Y because (0,0) is the top-left in Canvas
                                                val x = index * spacing
                                                val y =
                                                    height - (((value.y[comp] - minY) / (maxY - minY)) * height)

                                                if (index == 0) moveTo(x, y) else lineTo(x, y)
                                            }
                                        }

                                    }
                                drawPath(
                                    path = path,
                                    color = mColors[comp % mColors.size],
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                } else {
                    Text(text = "Waiting values")
                }

                if (showLegend) {
                    Box(
                        modifier = Modifier
                            .alpha(0.8f)
                            .clip(Shapes.small)
                            .background(Grey2)
                            .align(Alignment.BottomEnd)
                            .padding(LocalDimensions.current.paddingSmall)
                    ) {
                        Row {
                            for (i in legend.indices) {
                                Text(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .background(mColors[i % mColors.size]),
                                    text = "  ",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                                Text(
                                    modifier = Modifier.padding(start = 4.dp),
                                    text = legend[i],
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                        )
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