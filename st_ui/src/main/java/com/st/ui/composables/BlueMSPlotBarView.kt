package com.st.ui.composables

import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.st.ui.theme.Grey10
import com.st.ui.theme.Grey3
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryBlue
import com.st.ui.theme.PrimaryPink
import com.st.ui.theme.WarningText
import java.text.DecimalFormat
import kotlin.collections.any
import kotlin.collections.map

private val BottomAxisLabelKey = ExtraStore.Key<List<String>>()

private val BottomAxisValueFormatter = CartesianValueFormatter { context, x, _ ->
    context.model.extraStore[BottomAxisLabelKey][x.toInt()]
}

private val YDecimalFormat = DecimalFormat("#.##'%'")
private val MarkerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(YDecimalFormat)

private fun getColumnProvider(positive: LineComponent) =
    object : ColumnCartesianLayer.ColumnProvider {
        override fun getColumn(
            entry: ColumnCartesianLayerModel.Entry,
            seriesIndex: Int,
            extraStore: ExtraStore,
        ) = positive

        override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore) = positive
    }

@Composable
private fun TextStyle.toGraphicsTypeFace(): Typeface {
    val resolver = LocalFontFamilyResolver.current
    return remember(resolver, this) {
        resolver.resolve(
            fontFamily = this.fontFamily,
            fontWeight = this.fontWeight ?: FontWeight.Normal,
            fontStyle = this.fontStyle ?: FontStyle.Normal,
            fontSynthesis = this.fontSynthesis ?: FontSynthesis.All,
        )
    }.value as Typeface
}

@Composable
fun BlueMSPlotBarView(
    modifier: Modifier = Modifier,
    label: String,
    historyValues: List<Float>,
    currentValue: Float,
    minValue: Float,
    maxValue: Float,
    averageValue: Float
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    val positiveColumn =
        rememberLineComponent(
            fill = fill(PrimaryPink),
            thickness = 8.dp,
            shape = CorneredShape.rounded(topLeftPercent = 40, topRightPercent = 40),
        )

    LaunchedEffect(key1 = historyValues) {
        modelProducer.runTransaction {
            columnSeries {
                series(historyValues)
                extras {
                    it[BottomAxisLabelKey] =
                        historyValues.indices.map { index -> "-${index + 1} ${if (index == 0) "Day" else "Days"}" }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingSmall)
    ) {
        CartesianChartHost(
            modifier = modifier
                .weight(2f)
                .padding(LocalDimensions.current.paddingNormal),
            chart = rememberCartesianChart(
                layers = arrayOf(
                    rememberColumnCartesianLayer(
                        rangeProvider = CartesianLayerRangeProvider.fixed(
                            minY = minValue.toDouble(),
                            maxY = maxValue.toDouble()
                        ),
                        columnProvider = remember(positiveColumn) {
                            getColumnProvider(positiveColumn)
                        }
                        //dataLabel =  rememberTextComponent()
                    )
                ),
                startAxis = VerticalAxis.rememberStart(
                    label = rememberTextComponent(
                        color = Grey10,
                        textSize = MaterialTheme.typography.bodySmall.fontSize,
                        typeface = MaterialTheme.typography.bodySmall.toGraphicsTypeFace()
                    ),
                    guideline = null
                ),
                marker = rememberVicoMarker(valueFormatter = MarkerValueFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(
                    itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned() },
                    label = rememberAxisLabelComponent(
                        color = Grey10,
                        textSize = MaterialTheme.typography.bodySmall.fontSize,
                        typeface = MaterialTheme.typography.bodySmall.toGraphicsTypeFace()
                    ),
                    valueFormatter = BottomAxisValueFormatter,
                    labelRotationDegrees = -90f,
                    guideline = null
                ),
                decorations = listOf(
                    rememberAverageValueVicoHorizontalLine(
                        averageValue = averageValue,
                    )
                )
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = LocalDimensions.current.paddingNormal,
                    end = LocalDimensions.current.paddingNormal
                ), thickness = 1.dp, color = Grey3
        )

        AnimatedContent(targetState = historyValues.any { it != 0f }) { targetState ->
            if (targetState) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = LocalDimensions.current.paddingSmall,
                            start = LocalDimensions.current.paddingNormal,
                            end = LocalDimensions.current.paddingNormal
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )

                    Text(
                        text = "$currentValue",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPink
                    )
                }
            } else {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = LocalDimensions.current.paddingSmall,
                            start = LocalDimensions.current.paddingNormal,
                            end = LocalDimensions.current.paddingNormal
                        ),
                    textAlign = TextAlign.Center,
                    text = "No Data in this period",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = WarningText
                )
            }
        }
    }
}