package com.st.ui.composables

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.st.ui.R
import com.st.ui.theme.BlueMSTheme
import com.st.ui.theme.ErrorText
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryYellow
import com.st.ui.theme.SuccessText
import com.st.ui.utils.valueToColor
import kotlin.math.min

private val colorStops = arrayOf(
    0.0f to ErrorText,
    0.6f to PrimaryYellow,
    1f to SuccessText
)

@Composable
fun BlueMSCircularTextView(
    modifier: Modifier = Modifier,
    coloredText: Boolean = false,
    values: List<Pair<String, Int>> = listOf()
) {
    val context = LocalContext.current

    val configuration = LocalConfiguration.current

    val smallScreen by remember(key1 = configuration) {
        derivedStateOf {
            val screenWidthDp = configuration.screenWidthDp
            screenWidthDp < 400
        }
    }

    Box(
        modifier = modifier
            //.fillMaxSize()
            .size((300).dp)
            .padding(start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                bottom = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingMedium)
    ) {
        values.forEachIndexed { i, value ->
            val animateFloat = remember(key1 = value.second) { Animatable(0f) }

            val colorText = if (coloredText)
                valueToColor(value.second, colorStops).toArgb()
            else
                ContextCompat.getColor(
                    context,
                    R.color.labelPlotContrast
                )

            LaunchedEffect(animateFloat) {
                animateFloat.animateTo(
                    targetValue = value.second.toFloat(),
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                )
            }
            Canvas(modifier = modifier.fillMaxSize()) {
                val sweepAngle = (animateFloat.value / 100f) * 270f
                val startAngle0 = 270f
                val height = size.height - 10f - 90f * i
                val width = size.width - 10f - 90f * i
                val minDim = min(width, height)
                val style = Stroke(width = 40f, cap = StrokeCap.Round)
                val size = Size(minDim, minDim)
                val offsetX = (width - minDim) / 2 + 45f * i
                val offsetY = (height - minDim) / 2 + 45f * i

                drawArc(
                    color = Color.LightGray,
                    startAngle = startAngle0,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(offsetX, offsetY),
                    size = size,
                    style = style
                )

                drawArc(
                    brush = Brush.linearGradient(
//                        listOf(
//                            ErrorText,
//                            PrimaryYellow,
//                            SuccessText
//                        ),
                        colorStops = colorStops,
                        start = Offset((size.width - 10f) / 2 + offsetX, 0f + offsetY),
                        end = Offset(0f + offsetX, (size.height - 10f) / 2 + offsetY),
                        tileMode = TileMode.Clamp
                    ),
                    startAngle = startAngle0,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(offsetX, offsetY),
                    size = size,
                    style = style
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        value.first + " ${value.second}%",
                        size.width / 2 - 70f + 45f * i + (if (smallScreen) 40 else 90),
                        10f + 45f * i,
                        Paint().apply {
                            textSize = 40f
                            textAlign = Paint.Align.RIGHT
                            color = colorText
                        }
                    )
                }
            }
        }
    }
}


/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun BlueMSCircularTextView3Preview() {
    BlueMSTheme {
        BlueMSCircularTextView(
            values = listOf(
                Pair("Luca", 30),
                Pair("Pezzoni", 80),
                Pair("Agrate", 53)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSCircularTextView6Preview() {
    BlueMSTheme {
        BlueMSCircularTextView(
            values = listOf(
                Pair("Luca", 30),
                Pair("Pezzoni", 80),
                Pair("Agrate", 53),
                Pair("Lorenzo", 46),
                Pair("Andrea", 80),
                Pair("Alberto", 90)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSCircularTextView8Preview() {
    BlueMSTheme {
        BlueMSCircularTextView(
            values = listOf(
                Pair("Luca Pezzoni", 30),
                Pair("Giuseppe", 80),
                Pair("Agrate", 53),
                Pair("Lorenzo", 46),
                Pair("Andrea", 80),
                Pair("Alberto", 100),
                Pair("Davide", 10),
                Pair("Diego", 90)
            ),
            coloredText = true
        )
    }
}


