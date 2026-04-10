package com.st.plot.composable
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.st.blue_sdk.features.Feature
import com.st.blue_sdk.features.FeatureUpdate
import com.st.plot.PlotViewModel
import com.st.plot.utils.fieldsDesc
import com.st.ui.theme.ErrorText
import com.st.ui.theme.Grey10
import com.st.ui.theme.Grey2
import com.st.ui.theme.InfoText
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryPink
import com.st.ui.theme.SecondaryBlue
import com.st.ui.theme.Shapes
import com.st.ui.theme.SuccessText
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import com.st.plot.utils.BlueMSPlotEntry
import com.st.plot.utils.toBlueMSPlotEntry
import kotlinx.coroutines.launch

private fun featureLegend(feature: Feature<*>): Array<String> {
    val items = feature.fieldsDesc()
    return items.keys.toTypedArray()
}

private fun featureUnit(feature: Feature<*>): Array<String> {
    val items = feature.fieldsDesc()
    return items.values.toTypedArray()
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

private val mColors: Array<Color> = arrayOf(
    InfoText,
    ErrorText,
    SuccessText,
    Grey10,
    SecondaryBlue,
    PrimaryPink
)

@Composable
fun BlueMSPlot(
    modifier: Modifier,
    feature: Feature<*>,
    viewModel: PlotViewModel,
    interpolationType: PlotInterpolationType = PlotInterpolationType.LINEAR,
    featureUpdate: FeatureUpdate<*>?,
    showMaxMin: Boolean,
    makeSnapShot: Boolean,
    onMakeSnapShotDone: () -> Unit = { /** NOOP **/ },
    onSaveSnapshot: (Bitmap) -> Unit = { /** NOOP **/ }
) {

    var dataPoints: List<BlueMSPlotEntry> by remember {
        mutableStateOf(
            listOf()
        )
    }

    var mFirstNotificationTimeStamp by remember {
        mutableLongStateOf(0)
    }

    var legend by remember {
        mutableStateOf<Array<String>>(arrayOf())
    }

    var unit by remember {
        mutableStateOf<Array<String>>(arrayOf())
    }

    val boundary by viewModel.boundary.collectAsStateWithLifecycle()

    var maxPlotEntry by remember { mutableStateOf(floatArrayOf()) }
    var minPlotEntry by remember { mutableStateOf(floatArrayOf()) }

    var labelYAxis by remember { mutableIntStateOf(7) }

    var maxY by remember { mutableFloatStateOf(-Float.MAX_VALUE) }
    var minY by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    var isAutoScaleMinMaxEnabled by remember { mutableStateOf(true) }

    val textMeasurer = rememberTextMeasurer()

    var showLegend by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    LaunchedEffect(key1 = makeSnapShot) {
        if(makeSnapShot) {
            coroutineScope.launch {
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                onSaveSnapshot(bitmap)
            }
            onMakeSnapShotDone()
        }
    }


    LaunchedEffect(key1 = feature) {
        dataPoints = listOf()
        legend = featureLegend(feature)
        unit = featureUnit(feature)

        maxPlotEntry = FloatArray(legend.size) { -Float.MAX_VALUE }
        minPlotEntry = FloatArray(legend.size) { Float.MAX_VALUE }

        mFirstNotificationTimeStamp = System.currentTimeMillis()
        showLegend = !showMaxMin
    }

    LaunchedEffect(key1 = showMaxMin) {
        showLegend = !showMaxMin
    }

    LaunchedEffect(key1 = boundary) {
        boundary.nLabels?.let {
            labelYAxis = it
        }
        if (boundary.enableAutoScale) {
            isAutoScaleMinMaxEnabled = true
        } else {
            isAutoScaleMinMaxEnabled = false
            if (boundary.max != null)
                maxY = boundary.max!!
            if (boundary.min != null)
                minY = boundary.min!!
        }
    }

    LaunchedEffect(key1 = featureUpdate) {
        featureUpdate?.let { update ->

            val plotEntry =
                update.toBlueMSPlotEntry(feature = feature, xOffset = mFirstNotificationTimeStamp)

            plotEntry?.let { lastData ->

                //if we have a number of data different from the number of legends
                //rebuild the plot
                if (lastData.y.size != legend.size) {
                    legend = featureLegend(feature)
                    unit = featureUnit(feature)

                    maxPlotEntry = FloatArray(legend.size) { -Float.MAX_VALUE }
                    minPlotEntry = FloatArray(legend.size) { Float.MAX_VALUE }

                    mFirstNotificationTimeStamp = System.currentTimeMillis()
                }

                //Compute the Max
                for (i in lastData.y.indices) {
                    if (lastData.y[i] > maxPlotEntry[i]) {
                        maxPlotEntry[i] = lastData.y[i]
                    }
                    if (lastData.y[i] < minPlotEntry[i]) {
                        minPlotEntry[i] = lastData.y[i]
                    }
                }

                //Update the plot
                val localDataPoints = dataPoints.toMutableList()
                localDataPoints.add(lastData)
                dataPoints = removeEntryOlderThan(
                    localDataPoints.toList(),
                    viewModel.secondsToPlot.seconds
                )

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
        }
    }

    Box(
        modifier = modifier.drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            },
        contentAlignment = Alignment.Center
    ) {

        if (dataPoints.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(start = 40.dp, bottom = 20.dp, top = 10.dp, end = 10.dp)
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

                    val labelValue = minY + ((maxY - minY) / labelYAxis) * i

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

                            // Calculate X and Y coordinates
                            // We flip Y because (0,0) is the top-left in Canvas

                            if (dataPoints.isNotEmpty()) {
                                val x = 0f
                                val y =
                                    height - (((dataPoints.first().y[comp] - minY) / (maxY - minY)) * height)
                                moveTo(x, y)
                            }

                            for (index in 1 until dataPoints.size) {
                                val toX = index * spacing
                                val toY =
                                    height - (((dataPoints[index].y[comp] - minY) / (maxY - minY)) * height)
                                when (interpolationType) {
                                    PlotInterpolationType.LINEAR -> lineTo(toX, toY)

                                    PlotInterpolationType.CUBIC -> {
                                        val fromX = (index - 1) * spacing
                                        val fromY =
                                            height - (((dataPoints[index - 1].y[comp] - minY) / (maxY - minY)) * height)
                                        cubicTo(
                                            x1 = fromX + (toX - fromX) / 2f,
                                            y1 = fromY,
                                            x2 = fromX + (toX - fromX) / 2f,
                                            y2 = toY,
                                            x3 = toX,
                                            y3 = toY
                                        )
                                    }
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

        if (showMaxMin) {
            Box(
                modifier = Modifier
                    .alpha(0.8f)
                    .clip(Shapes.small)
                    .background(Grey2)
                    .align(Alignment.BottomEnd)
                    .padding(LocalDimensions.current.paddingSmall)
            ) {
                Column {
                    Text(
                        text = "Max:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                    for (i in maxPlotEntry.indices) {
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = "${legend[i]}: ${maxPlotEntry[i]} ${unit[i]}",
                            color = mColors[i % mColors.size],
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        )
                    }
                    Text(
                        text = "Min:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                    for (i in minPlotEntry.indices) {
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = "${legend[i]}: ${minPlotEntry[i]} ${unit[i]}",
                            color = mColors[i % mColors.size],
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