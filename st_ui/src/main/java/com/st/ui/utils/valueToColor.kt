package com.st.ui.utils

import androidx.compose.ui.graphics.Color

fun valueToColor(value: Int, colorStops: Array<Pair<Float, Color>>): Color {

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