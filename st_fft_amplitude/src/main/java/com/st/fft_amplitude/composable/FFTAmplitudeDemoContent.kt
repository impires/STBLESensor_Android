package com.st.fft_amplitude.composable

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.st.fft_amplitude.FFTAmplitudeViewModel
import com.st.fft_amplitude.utilites.LineConf
import com.st.ui.composables.BlueMsButton
import com.st.ui.composables.BlueMsButtonOutlined
import com.st.ui.theme.Grey2
import com.st.ui.theme.Grey6
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

private const val Y_NUMBER_LINES = 6

@Composable
fun FFTAmplitudeDemoContent(
    modifier: Modifier,
    viewModel: FFTAmplitudeViewModel
) {

    var showDetails by remember { mutableStateOf(false) }

    val loadingStatus by viewModel.loadingStatus.collectAsStateWithLifecycle()

    val mFftData by viewModel.mFftData.collectAsStateWithLifecycle()

    val mFftMax by viewModel.mFftMax.collectAsStateWithLifecycle()

    val mXStats by viewModel.mXStats.collectAsStateWithLifecycle()
    val mYStats by viewModel.mYStats.collectAsStateWithLifecycle()
    val mZStats by viewModel.mZStats.collectAsStateWithLifecycle()

    var maxY by remember { mutableFloatStateOf(-Float.MAX_VALUE) }
    var maxX by remember { mutableFloatStateOf(-Float.MAX_VALUE) }
    //minX and minY == 0

    var nComponents by remember { mutableIntStateOf(3) }

    val textMeasurerY = rememberTextMeasurer()
    val textMeasurerX = rememberTextMeasurer()

    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val context = LocalContext.current
    var snap by remember { mutableStateOf<Bitmap?>(null) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { fileUri ->
        fileUri?.let {
            val result = viewModel.saveImage(context, fileUri)
            if (result) {
                Toast.makeText(context, "File Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error Saving File", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = LocalDimensions.current.paddingNormal)
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            shape = Shapes.small,
            shadowElevation = LocalDimensions.current.elevationNormal
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (mFftData.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(
                                start = 40.dp,
                                end = 20.dp,
                                top = 20.dp,
                                bottom = 20.dp
                            )
                    ) {
                        val width = size.width - 20.dp.toPx()
                        val labelHeight = 20.dp.toPx()
                        val height = size.height - labelHeight
                        nComponents = mFftData.size.coerceAtMost(LineConf.LINES.size)
                        var labelLineSpacing = height / (Y_NUMBER_LINES)

                        maxY = -Float.MAX_VALUE
                        maxX = -Float.MAX_VALUE

                        //Compute the Max and
                        for (comp in 0 until nComponents) {
                            for (index in 0..<mFftData[comp].size) {
                                val y = mFftData[comp][index]
                                if (y > maxY)
                                    maxY = y
                            }
                            if (comp == 0) {
                                maxX = (mFftData[comp].size - 1) * viewModel.mFreqStep
                            }
                        }

                        val spacing = width / maxX

                        // Draw Horizontal Grid Lines
                        for (i in 0..6) {

                            val y = height - labelLineSpacing * i

                            val labelValue = maxY / Y_NUMBER_LINES * i

                            // Draw Grid Lines
                            drawLine(
                                color = if (i == 0) Color.Black else Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = if (i == 0) 2.dp.toPx() else 1.dp.toPx()
                            )


                            // Draw Y-Axis Label
                            drawText(
                                textMeasurer = textMeasurerY,
                                text = "%.2f".format(
                                    Locale.getDefault(),
                                    labelValue
                                ),
                                topLeft = Offset(-35.dp.toPx(), y - 10.dp.toPx()),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = if (i == 0) Color.Black else Color.Gray
                                )
                            )
                        }

                        // Draw Vertical Grid Lines
                        val lineDistance =
                            if (maxX < 100f) {
                                10
                            } else if (maxX < 1000f) {
                                100
                            } else {
                                1000
                            }

                        val numLines = (maxX / lineDistance).toInt()
                        labelLineSpacing = width / (maxX / lineDistance)

                        for (i in 0..numLines) {
                            val x = i * labelLineSpacing

                            val labelValue = i * lineDistance

                            // Draw Grid Lines
                            drawLine(
                                color = if (i == 0) Color.Black else Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = if (i == 0) 2.dp.toPx() else 1.dp.toPx()
                            )


                            // Draw X-Axis Label
                            val labelValueString = "$labelValue"

                            val labelStyle = TextStyle(
                                fontSize = 10.sp,
                                color = if (i == 0) Color.Black else Color.Gray
                            )

                            val textLayoutResult =
                                textMeasurerX.measure(text = labelValueString, style = labelStyle)
                            val textWidth = textLayoutResult.size.width

                            drawText(
                                textMeasurer = textMeasurerX,
                                text = labelValueString,
                                topLeft = Offset(x - (textWidth / 2), height + 10.dp.toPx()),
                                style = labelStyle
                            )
                        }

                        for (comp in 0 until nComponents) {
                            val path = Path().apply {
                                for (index in 0..<mFftData[comp].size) {
                                    val x = index * spacing * viewModel.mFreqStep
                                    val y = height - (mFftData[comp][index] / maxY) * height
                                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = LineConf.LINES[comp].color,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                } else {
                    Text(text = "Data acquisition ongoing…")
                }

                if (showDetails) {
                    Box(
                        modifier = Modifier
                            .width(intrinsicSize = IntrinsicSize.Max)
                            .alpha(0.8f)
                            .clip(Shapes.small)
                            .background(Grey2)
                            .align(Alignment.TopEnd)
                            .animateContentSize()
                            .padding(LocalDimensions.current.paddingSmall)
                    ) {

                        Column(
                            modifier = Modifier
                                .padding(all = LocalDimensions.current.paddingNormal),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Frequency Detail info",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                            if (mFftMax.isNotEmpty()) {
                                if (mFftMax.size > 1) {
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "X: Max: %.4f @ %.2f Hz",
                                            mFftMax[0].amplitude,
                                            mFftMax[0].frequency
                                        ),
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                                if (mFftMax.size > 2) {
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "Y: Max: %.4f @ %.2f Hz",
                                            mFftMax[1].amplitude,
                                            mFftMax[1].frequency
                                        ),
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }

                                if (mFftMax.size == 3) {
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "Z: Max: %.4f @ %.2f Hz",
                                            mFftMax[1].amplitude,
                                            mFftMax[1].frequency
                                        ),
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "Not Available",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }

                            Text(
                                text = "Time Data Info",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                            if ((mXStats == null) && (mYStats == null) && (mZStats == null)) {
                                Text(
                                    text = "Not Available",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            } else {
                                if (mXStats != null) {
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "X: Acc Peak: %.2f m/s^2\n\t\tRMS Speed %.2f mm/s",
                                            mXStats!!.accPeak,
                                            mXStats!!.rmsSpeed
                                        ),
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }

                                if (mYStats != null) {
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "Y: Acc Peak: %.2f m/s^2\n\t\tRMS Speed %.2f mm/s",
                                            mYStats!!.accPeak,
                                            mYStats!!.rmsSpeed
                                        ),
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }

                                if (mZStats != null) {
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "Y: Acc Peak: %.2f m/s^2\n\t\tRMS Speed %.2f mm/s",
                                            mZStats!!.accPeak,
                                            mZStats!!.rmsSpeed
                                        ),
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(LocalDimensions.current.paddingSmall), color = Grey6
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                for (i in 0..<nComponents) {
                                    Text(
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .background(LineConf.LINES[i].color),
                                        text = "  ",
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                    Text(
                                        modifier = Modifier.padding(start = 4.dp),
                                        text = LineConf.LINES[i].name,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .alpha(0.8f)
                            .clip(Shapes.small)
                            .background(Grey2)
                            .align(Alignment.TopEnd)
                            .padding(LocalDimensions.current.paddingSmall)
                    ) {
                        Row {
                            for (i in 0..<nComponents) {
                                Text(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .background(LineConf.LINES[i].color),
                                    text = "  ",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                                Text(
                                    modifier = Modifier.padding(start = 4.dp),
                                    text = LineConf.LINES[i].name,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingLarge)
        ) {
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    modifier = Modifier.padding(bottom = LocalDimensions.current.paddingNormal),
//                    textAlign = TextAlign.Start,
                    text = "Loading Progress:", style = MaterialTheme.typography.titleSmall
                )

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    progress = { loadingStatus / 100f })

            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal)
            ) {

                BlueMsButtonOutlined(text = "Details", onClick = {
                    showDetails = !showDetails
                })

                BlueMsButton(text = "Snapshot", onClick = {
                    coroutineScope.launch {
                        snap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    }
                })
            }
        }
    }

    if (snap != null) {
        AlertDialog(
            modifier = Modifier.alpha(0.9f),
            onDismissRequest = { snap = null },
            dismissButton = {
                BlueMsButtonOutlined(
                    onClick = {
                        snap = null
                    },
                    text = "Cancel"
                )
            },
            confirmButton = {
                BlueMsButton(
                    onClick = {
                        viewModel.snap = snap
                        val fileName =
                            "SnapShot_FFT_${Date()}.png".replace(' ', '-')
                        pickFileLauncher.launch(fileName)
                        snap = null
                    },
                    text = "Save"
                )
            },
            title = {
                Text(
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    text = "FFT"
                )
            },
            text = {
                Image(bitmap = snap!!.asImageBitmap(), contentDescription = "Snapshot")
            }
        )
    }
}

