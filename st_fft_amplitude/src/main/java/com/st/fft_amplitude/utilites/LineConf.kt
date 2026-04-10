package com.st.fft_amplitude.utilites

import androidx.compose.ui.graphics.Color
import com.st.ui.theme.PrimaryPink
import com.st.ui.theme.SecondaryBlue
import com.st.ui.theme.SuccessPressed

data class LineConf(val color: Color, val name: String) {
    companion object {
        val LINES = arrayOf(
            LineConf(PrimaryPink, "X"),
            LineConf(SecondaryBlue, "Y"),
            LineConf(SuccessPressed, "Z")
        )
    }
}
