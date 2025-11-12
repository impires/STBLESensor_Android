package com.st.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.st.ui.theme.ErrorText
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.NotActiveColor
import com.st.ui.theme.PreviewBlueMSTheme
import com.st.ui.theme.PrimaryBlue
import com.st.ui.theme.PrimaryYellow
import com.st.ui.theme.SecondaryBlue
import com.st.ui.theme.Shapes
import com.st.ui.theme.SuccessText

@Composable
fun BlueMSHorizontalProgressBar(
    modifier: Modifier = Modifier,
    label: String,
    backgroundColor: Color = PrimaryBlue,
    textColor: Color = Color.Unspecified,
    value: Int,
    labelLow: String = "Bad",
    labelHigh: String = "Good",
    insideCard: Boolean = true
) {
    if (insideCard) {
        Surface(
            modifier = modifier,
            shape = Shapes.small,
            color = backgroundColor,
            shadowElevation = LocalDimensions.current.elevationNormal
        ) {
            BlueMSHorizontalProgressBarContent(
                label = label,
                textColor = textColor,
                value = value,
                labelLow = labelLow,
                labelHigh = labelHigh
            )
        }
    } else {
        BlueMSHorizontalProgressBarContent(
            label = label,
            textColor = textColor,
            value = value,
            labelLow = labelLow,
            labelHigh = labelHigh
        )
    }
}

@Composable
private fun BlueMSHorizontalProgressBarContent(
    modifier: Modifier = Modifier,
    label: String,
    textColor: Color = Color.Unspecified,
    value: Int,
    labelLow: String = "Bad",
    labelHigh: String = "Good",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = LocalDimensions.current.paddingNormal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val colorStops = arrayOf(
            0.0f to ErrorText,
            0.6f to PrimaryYellow,
            1f to SuccessText
        )

        val colorValue = valueToColor(value, colorStops)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LocalDimensions.current.paddingNormal),
            horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = textColor
            )
            Text(
                text = "$value%",
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(
                    start = LocalDimensions.current.paddingSmall,
                    end = LocalDimensions.current.paddingNormal,
                    bottom = LocalDimensions.current.paddingNormal
                )
                .horizontalRelativePosition(value)
        ) {
            VerticalDivider(
                color = colorValue,
                thickness = 4.dp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(bottom = LocalDimensions.current.paddingNormal)
                .background(
                    shape = Shapes.small,
                    brush = Brush.horizontalGradient(colorStops = colorStops)
                )
        )


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LocalDimensions.current.paddingNormal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelLow,
                color = textColor
            )
            Text(
                text = labelHigh,
                color = textColor
            )
        }
    }
}


private fun Modifier.horizontalRelativePosition(
    value: Int
) = layout { measurable, constraints ->
    // Measure the composable
    val placeable = measurable.measure(constraints)

    val placeableY = 0
    val placeableX = placeable.width * value / 100
    layout(placeable.width, placeable.height) {
        // Where the composable gets placed
        placeable.placeRelative(placeableX, placeableY)
    }
}

private fun valueToColor(value: Int, colorStops: Array<Pair<Float, Color>>): Color {

    val percent = value / 100f

    if (percent < colorStops[1].first) {
        val color1 = colorStops[0].second
        val color2 = colorStops[1].second

        val range = colorStops[1].first - colorStops[0].first

        val rangePercentage = percent / range


        val resultRed: Float = color1.red + rangePercentage * (color2.red - color1.red)
        val resultGreen: Float = color1.green + rangePercentage * (color2.green - color1.green)
        val resultBlue: Float = color1.blue + rangePercentage * (color2.blue - color1.blue)

        return Color(resultRed, resultGreen, resultBlue)
    } else {
        val color1 = colorStops[1].second
        val color2 = colorStops[2].second

        val range = colorStops[2].first - colorStops[1].first

        val rangePercentage = (percent - colorStops[1].first) / range

        val resultRed: Float = color1.red + rangePercentage * (color2.red - color1.red)
        val resultGreen: Float = color1.green + rangePercentage * (color2.green - color1.green)
        val resultBlue: Float = color1.blue + rangePercentage * (color2.blue - color1.blue)

        return Color(resultRed, resultGreen, resultBlue)
    }
}

/** ----------------------- PREVIEW --------------------------------------- **/

@Preview(showBackground = true)
@Composable
private fun BlueMSHorizontalProgressBarPreview100() {
    PreviewBlueMSTheme {
        BlueMSHorizontalProgressBarContent(
            label = "BlueMS",
            value = 100
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSHorizontalProgressBarPreview0() {
    PreviewBlueMSTheme {
        BlueMSHorizontalProgressBar(
            label = "BlueMS",
            backgroundColor = NotActiveColor,
            value = 0,
            insideCard = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueMSHorizontalProgressBarPreview50() {
    PreviewBlueMSTheme {
        BlueMSHorizontalProgressBar(
            label = "BlueMS",
            backgroundColor = SecondaryBlue,
            textColor = ErrorText,
            value = 50,
            labelLow = "Luca",
            labelHigh = "Pezz"
        )
    }
}
