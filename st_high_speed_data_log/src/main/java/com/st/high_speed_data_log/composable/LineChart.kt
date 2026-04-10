package com.st.high_speed_data_log.composable

//import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.Legend
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.components.YAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.st.high_speed_data_log.R
import com.st.high_speed_data_log.model.BlueMSPlotEntry
import com.st.high_speed_data_log.model.ChartType
import com.st.high_speed_data_log.model.StreamDataChannel
import com.st.ui.theme.ErrorText
import com.st.ui.theme.Grey10
import com.st.ui.theme.Grey2
import com.st.ui.theme.InfoText
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryPink
import com.st.ui.theme.SecondaryBlue
import com.st.ui.theme.Shapes
import com.st.ui.theme.SuccessText
//import java.util.LinkedList
import java.util.Locale
import kotlin.math.sqrt

private val mColors: Array<Color> = arrayOf(
    InfoText,
    ErrorText,
    SuccessText,
    Grey10,
    SecondaryBlue,
    PrimaryPink
)
private const val NUMBER_OF_LABELS = 6

fun String.getChannelsName(channelNum: Int = 3) =
    when (this) {
        "Mag", "Acc" -> listOf("x", "y", "z")
        "Temp" -> listOf("Temp")
        "Press" -> listOf("Press")
        else -> List(channelNum) { "Channel $it" }
    }

fun Modifier.vertical() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2)
        )
    }
}

@Composable
fun LineChart(
    modifier: Modifier = Modifier,
    name: String,
    chartType: ChartType,
    label: String,
    maxSize: Int = 100,
    data: List<StreamDataChannel> = emptyList()
) {
    val context = LocalContext.current
    //var lineChart: LineChart? by remember { mutableStateOf(value = null) }
    var prevChartType by remember { mutableStateOf(value = chartType) }
    var prevLabel by remember { mutableStateOf(value = label) }

    var maxY by remember { mutableFloatStateOf(-Float.MAX_VALUE) }
    var minY by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    var dataPoints: List<BlueMSPlotEntry> by remember {
        mutableStateOf(
            listOf()
        )
    }

    var legend by remember {
        mutableStateOf<Array<String>>(arrayOf())
    }

    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(key1 = label, key2 = chartType) {
        if (prevLabel != label || chartType != prevChartType) {
            dataPoints = listOf()
            prevChartType = chartType
            prevLabel = label
        }
    }

    LaunchedEffect(key1 = data) {
        if (data.isNotEmpty()) {
            //Creating the Legends
            if (chartType == ChartType.SUM) {
                legend = arrayOf("Modulo")
            } else {
                val channels = data.firstOrNull()?.data?.size ?: 0

                legend = Array(channels) { i ->
                    if (i < name.getChannelsName(channels).size) {
                        name.getChannelsName(channels)[i]
                    } else {
                        ""
                    }
                }
            }

            //Create the data points..
            if (data.isNotEmpty()) {
                when(chartType) {
                    ChartType.ALL -> {
                        data.forEachIndexed { i, value ->
                            val index:Long = (dataPoints.lastOrNull()?.x ?: 0) + 1

                            val localDataPoints = dataPoints.toMutableList()
                            localDataPoints.add(BlueMSPlotEntry(x = index, y = value.data.toFloatArray()))
                            dataPoints = localDataPoints.takeLast(maxSize).toList()

                        }
                    }
                    ChartType.SUM -> {
                        data.forEachIndexed { i, value ->
                            if (value.data.size == 3) {
                                val value = sqrt(
                                    ((value.data[0] * value.data[0]) +
                                            (value.data[1] * value.data[1]) +
                                            (value.data[2] * value.data[2])).toDouble()
                                ).toFloat()

                                val index:Long = (dataPoints.lastOrNull()?.x ?: 0) + 1

                                val localDataPoints = dataPoints.toMutableList()
                                localDataPoints.add(BlueMSPlotEntry(x = index, y = floatArrayOf(value)))
                                dataPoints = localDataPoints.takeLast(maxSize).toList()
                            }
                        }
                    }
                   else -> {
                       data.forEachIndexed { i, value ->
                           val index:Long = (dataPoints.lastOrNull()?.x ?: 0) + 1

                           val localDataPoints = dataPoints.toMutableList()
                           localDataPoints.add(BlueMSPlotEntry(x = index, y = floatArrayOf(value.data[chartType.channelIndex])))
                           dataPoints = localDataPoints.takeLast(maxSize).toList()

                       }
                   }
                }

                //Search the Max and Min
                maxY = -Float.MAX_VALUE
                minY = Float.MAX_VALUE
                dataPoints.forEach { values ->
                    values.y.forEach { value ->
                        if (value > maxY) {
                            maxY = value
                        }
                        if(value<minY) {
                            minY = value
                        }
                    }
                }

                if (maxY == minY) {
                    maxY += 1
                    minY -= 1
                }
            }
//            if(data.firstOrNull()?.data?.isNotEmpty() ?: false) {
//                if(dataPoints.size> maxSize) {
//                    dataPoints.removeAt(0)
//                }
//            }

        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
//                start = LocalDimensions.current.paddingNormal,
//                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f)
        ) {
            Text(
                modifier = Modifier
                    .vertical()
                    .rotate(-90f),
                text = "[${label}]"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (dataPoints.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 40.dp, top = 20.dp, bottom=20.dp, end = 10.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val minValue = if (dataPoints.size < 2) {
                            2
                        } else {
                            dataPoints.size
                        }
                        val spacing = width / (minValue - 1)
                        val labelLineSpacing = height / (NUMBER_OF_LABELS)

                        // Draw Horizontal Grid Lines
                        for (i in 0..NUMBER_OF_LABELS) {

                            val y = height - labelLineSpacing * i

                            val labelValue = minY + ((maxY - minY) / NUMBER_OF_LABELS) * i

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
                                text = "%.2f".format(
                                    Locale.getDefault(),
                                    labelValue
                                ),
                                topLeft = Offset(-35.dp.toPx(), y - 10.dp.toPx()),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            )
                        }

                        if (dataPoints.isNotEmpty()) {
                            dataPoints.first().y.indices.forEach { comp ->
                                val path = Path().apply {
                                    dataPoints.forEachIndexed { index, value ->
                                        // Calculate X and Y coordinates
                                        // We flip Y because (0,0) is the top-left in Canvas
                                        val x = index * spacing
                                        val y =
                                            height - (((value.y[comp] - minY) / (maxY - minY)) * height)

                                        if (index == 0) moveTo(x, y) else lineTo(x, y)
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

                Box(
                    modifier = Modifier
                        .alpha(0.8f)
                        .clip(Shapes.small)
                        .background(Grey2)
                        .align(Alignment.TopEnd)
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

//            if (false) {
//                AndroidView(
//                    modifier = Modifier
//                        .fillMaxSize(),
//                    factory = { ctx ->
//                        LineChart(ctx).also { chart ->
//                            Log.w("LineChart", "factory for $name $chartType")
//                            chart.description.text = label
//                            chart.description.isEnabled = false
//
//                            chart.setTouchEnabled(true)
//                            chart.isDragEnabled = true
//                            chart.setScaleEnabled(true)
//                            chart.setPinchZoom(true)
//
//                            val xl = chart.xAxis
//                            xl.position = XAxis.XAxisPosition.BOTTOM
//                            xl.setDrawLabels(false)
//                            xl.setDrawGridLines(false)
//                            xl.setAvoidFirstLastClipping(true)
//
//                            chart.axisRight.isEnabled = false
//                            val leftAxis = chart.axisLeft
//                            leftAxis.setDrawGridLines(true)
//                            leftAxis.textColor =
//                                ContextCompat.getColor(ctx, com.st.ui.R.color.labelPlotContrast)
//
////                chart.legend.setDrawInside(true)
//                            chart.legend.horizontalAlignment =
//                                Legend.LegendHorizontalAlignment.RIGHT
//                            chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
////                chart.legend.orientation = Legend.LegendOrientation.VERTICAL
//                            chart.legend.textColor =
//                                ContextCompat.getColor(ctx, com.st.ui.R.color.labelPlotContrast)
//
//                            val mLineColors = ctx.resources.getIntArray(R.array.dataSetColor)
//
//                            val channels = data.firstOrNull()?.data?.size ?: 0
//                            chart.data = LineData(
//                                List(channels) { i ->
//                                    val channelsName =
//                                        if (i < name.getChannelsName(channels).size) {
//                                            name.getChannelsName(channels)[i]
//                                        } else {
//                                            ""
//                                        }
//
//                                    LineDataSet(LinkedList(), channelsName).apply {
//                                        axisDependency = YAxis.AxisDependency.LEFT
//                                        setDrawCircles(false)
//                                        setDrawValues(false)
//                                        setColor(mLineColors[i % mLineColors.size])
//                                        setDrawHighlightIndicators(false)
//                                    }
//                                }
//                            )
//
//                            chart.isAutoScaleMinMaxEnabled = true
//
//                            lineChart = chart
//                        }
//                    },
//                    update = {
//                        Log.w("LineChart", "update for $name")
//                        Log.w("LineChart", "data = $data")
//                        lineChart?.let { chart ->
//                            if (chart.description.text != label || chartType != prevChartType) {
//                                prevChartType = chartType
//                                chart.description.text = label
//                                val mLineColors =
//                                    context.resources.getIntArray(R.array.dataSetColor)
//
//                                if (chartType == ChartType.SUM) {
//                                    chart.data = LineData(
//                                        listOf(
//                                            LineDataSet(LinkedList(), "Modulo").apply {
//                                                axisDependency = YAxis.AxisDependency.LEFT
//                                                setDrawCircles(false)
//                                                setDrawValues(false)
//                                                setColor(mLineColors.last())
//                                                setDrawHighlightIndicators(false)
//                                            }
//                                        )
//                                    )
//                                } else {
//                                    val channels = data.firstOrNull()?.data?.size ?: 0
//                                    chart.data = LineData(
//                                        List(channels) { i ->
//
//                                            val channelsName =
//                                                if (i < name.getChannelsName(channels).size) {
//                                                    name.getChannelsName(channels)[i]
//                                                } else {
//                                                    ""
//                                                }
//
//                                            LineDataSet(
//                                                LinkedList(),
//                                                channelsName
//                                            ).apply {
//                                                axisDependency = YAxis.AxisDependency.LEFT
//                                                setDrawCircles(false)
//                                                setDrawValues(false)
//                                                setColor(mLineColors[i % mLineColors.size])
//                                                setDrawHighlightIndicators(false)
//                                            }
//                                        }
//                                    )
//                                }
//
//                                chart.resetViewPortOffsets()
//                            }
//
//                            chart.data?.let { lineData ->
//                                lineData.dataSets.forEachIndexed { channelIndex, channel ->
//                                    if (chartType == ChartType.SUM) {
//                                        val size = channel.entryCount
//                                        val xLastEntry =
//                                            if (size > maxSize) {
//                                                (channel as LineDataSet).values.lastOrNull()?.x?.plus(
//                                                    1
//                                                )
//                                                    ?: 0f
//                                            } else {
//                                                size.toFloat()
//                                            }
//
//                                        data.forEachIndexed { i, value ->
//                                            if (value.data.size == 3) {
//                                                channel.addEntry(
//                                                    Entry(
//                                                        xLastEntry + i.toFloat(),
//
//                                                        sqrt(
//                                                            ((value.data[0] * value.data[0]) +
//                                                                    (value.data[1] * value.data[1]) +
//                                                                    (value.data[2] * value.data[2])).toDouble()
//                                                        ).toFloat()
//                                                    )
//                                                )
//                                            }
//
//                                            if (size > maxSize) {
//                                                channel.removeFirst()
//                                            }
//                                        }
//                                    } else {
//                                        if (chartType == ChartType.ALL || chartType.channelIndex == channelIndex) {
//                                            val size = channel.entryCount
//                                            val xLastEntry =
//                                                if (size > maxSize) {
//                                                    (channel as LineDataSet).values.lastOrNull()?.x?.plus(
//                                                        1
//                                                    )
//                                                        ?: 0f
//                                                } else {
//                                                    size.toFloat()
//                                                }
//
//                                            data.forEachIndexed { i, value ->
//                                                if (channelIndex < value.data.size) {
//                                                    channel.addEntry(
//                                                        Entry(
//                                                            xLastEntry + i.toFloat(),
//                                                            value.data[channelIndex]
//                                                        )
//                                                    )
//                                                }
//
//                                                if (size > maxSize) {
//                                                    channel.removeFirst()
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//
//                                lineData.notifyDataChanged()
//                                chart.notifyDataSetChanged()
//                                chart.invalidate()
//
//                                chart.setVisibleXRangeMaximum(maxSize.toFloat())
//
//                                chart.data.dataSets?.map { it as LineDataSet }?.firstOrNull()?.let {
//                                    if (it.values.isNotEmpty()) {
//                                        val lastIndex = it.entryCount - 1
//                                        val lastEntry = it.values[lastIndex]
//                                        chart.centerViewToAnimated(
//                                            lastEntry.x,
//                                            lastEntry.y,
//                                            YAxis.AxisDependency.RIGHT,
//                                            300L
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                )
//            }
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            text = "[s]"
        )

        Spacer(
            modifier = Modifier.height(
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        )
    }
}
