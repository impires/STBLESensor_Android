package com.st.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PreviewBlueMSTheme
import com.st.ui.theme.Shapes
import com.st.ui.theme.WarningBackground
import com.st.ui.theme.WarningPressed
import com.st.ui.theme.WarningText
import kotlin.math.atan
import kotlin.math.sqrt

@Composable
fun BlueMSDiagonalLabel(
    modifier: Modifier = Modifier,
    text: String,
    textEnable: Boolean = true,
    backgroundColor: Color = WarningBackground,
    alpha: Float = 0.9f,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        //initial height set at 0.dp
        var componentHeight by remember { mutableStateOf(120.dp) }
        var componentWidth by remember { mutableStateOf(60.dp) }

        // get local density from composable
        val density = LocalDensity.current


        val value by remember(key1 = componentHeight, key2 = componentWidth) {
            derivedStateOf {
                sqrt(componentHeight.value * componentHeight.value + componentWidth.value * componentWidth.value)
            }
        }

        val angle by remember(key1 = componentHeight, key2 = componentWidth) {
            derivedStateOf {
                Math.toDegrees(atan((componentHeight.value / componentWidth.value).toDouble()))
            }
        }

        Box(modifier = Modifier
            .onGloballyPositioned {
                componentHeight = with(density) {
                    it.size.height.toDp() - 24.dp
                }
                componentWidth = with(density) {
                    it.size.width.toDp() - 24.dp
                }
            }) {
            content()
        }
        if (textEnable) {
            Text(
                modifier = Modifier.width(value.dp)
                    .rotate(degrees = angle.toFloat())
                    .border(border = BorderStroke(2.dp, WarningPressed), shape = Shapes.small)
                    .alpha(alpha)
                    .background(color = backgroundColor, shape = Shapes.small)
                    .padding(LocalDimensions.current.paddingNormal),
                maxLines = 1,
                text = text,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = WarningText
            )
        }
    }
}


/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun BlueMSDiagonalLabelPreview() {
    PreviewBlueMSTheme {
        BlueMSDiagonalLabel(
            text = "Label"
        ){
            Text(text = "Preview\nDiagonal\nLabel")
        }
    }
}