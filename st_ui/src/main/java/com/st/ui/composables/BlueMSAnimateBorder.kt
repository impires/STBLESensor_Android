package com.st.ui.composables

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.st.ui.theme.Grey0
import com.st.ui.theme.PreviewBlueMSTheme
import com.st.ui.theme.PrimaryBlue

@Composable
fun BlueMSAnimateBorder(
    modifier: Modifier = Modifier,
    colors: List<Color> = emptyList(),
    contentPadding: Dp =8.dp,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = if (colors.isEmpty()) Brush.sweepGradient(
        listOf(
            PrimaryBlue,
            Grey0
        )
    ) else Brush.sweepGradient(colors)

    Surface(modifier = modifier, shape = RoundedCornerShape(size = 16.dp)) {
        Surface(
            modifier = Modifier
                .clipToBounds()
                .fillMaxWidth()
                .padding(4.dp)
                .drawWithContent {
                    rotate(rotation.value) {
                        drawCircle(brush = brush, radius = size.width, blendMode = BlendMode.SrcIn)
                    }
                    drawContent()
                },
            shape = RoundedCornerShape(size = 15.dp)
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun BlueMSAnimatedBorderPreview() {
    PreviewBlueMSTheme {
        BlueMSAnimateBorder{
            Text(text = "Preview\nAnimate\nBorder")
        }
    }
}