package com.st.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.shape.rounded
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.common.Position
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.st.ui.theme.PrimaryYellow

@Composable
fun rememberAverageValueVicoHorizontalLine(averageValue: Float): HorizontalLine {
    val fill = fill(PrimaryYellow.copy(alpha = 0.6f))
    val line = rememberLineComponent(fill = fill, thickness = 1.dp, shape = Shape.Rectangle)
    val labelComponent =
        rememberTextComponent(
            margins = insets(start = 6.dp),
            padding = insets(start = 8.dp, end = 8.dp, bottom = 2.dp),
            background =
                shapeComponent(fill, CorneredShape.rounded(bottomLeft = 4.dp, bottomRight = 4.dp)),
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