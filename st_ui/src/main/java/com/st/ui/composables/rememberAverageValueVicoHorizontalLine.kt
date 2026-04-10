package com.st.ui.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.st.ui.theme.PrimaryYellow
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent

@Composable
fun rememberAverageValueVicoHorizontalLine(averageValue: Float): HorizontalLine {
    val fill = Fill(PrimaryYellow.copy(alpha = 0.6f))
    val line = rememberLineComponent(fill = fill, thickness = 1.dp, shape = RectangleShape)
    val labelComponent =
        rememberTextComponent(
            margins = Insets(start = 6.dp),
            padding = Insets(start = 8.dp, end = 8.dp, bottom = 2.dp),
            background =
                ShapeComponent(fill,RoundedCornerShape(topStartPercent = 40, topEndPercent = 40)),
        )

    return remember(key1 = averageValue) {
        HorizontalLine(
            y = { averageValue.toDouble() },
            line = line,
            labelComponent = labelComponent,
            label = { "Average $averageValue" },
            verticalLabelPosition = Position.Vertical.Top,
        )
    }
}