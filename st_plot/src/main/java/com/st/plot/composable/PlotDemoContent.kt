package com.st.plot.composable

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.st.blue_sdk.features.Feature
import com.st.plot.PlotViewModel
import com.st.plot.R
import com.st.plot.utils.fieldsDesc
import com.st.plot.utils.toPlotDesc
import com.st.ui.composables.BlueMsButton
import com.st.ui.theme.Grey6
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryBlue
import java.util.Date
import java.util.Locale.getDefault

private const val WAITING_FEATURE_VALUE = "Feature Value"

@Composable
fun PlotDemoContent(
    modifier: Modifier,
    viewModel: PlotViewModel,
    nodeId: String
) {
    val plottableFeatures by viewModel.plottableFeatures.collectAsStateWithLifecycle()

    var plotDesc by remember { mutableStateOf(WAITING_FEATURE_VALUE) }

    if (plottableFeatures.isNotEmpty()) {
        val context = LocalContext.current
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

        val featureUpdate by viewModel.featureUpdate.collectAsStateWithLifecycle()
        val isPlotting by viewModel.isPlotting.collectAsStateWithLifecycle()
        val selectedFeature by viewModel.selectedFeature.collectAsStateWithLifecycle()
        var showTools by remember { mutableStateOf(false) }
        var makeSnapShot by remember { mutableStateOf(false) }

        var showSettingsDialog by remember { mutableStateOf(false) }
        var showMaxMin by remember { mutableStateOf(false) }

        var plotInterpolationType by remember { mutableStateOf(PlotInterpolationType.LINEAR) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(LocalDimensions.current.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal),
                verticalAlignment = Alignment.CenterVertically
            ) {

                PlottableFeaturesDropDownMenu(
                    modifier = Modifier.weight(2f),
                    values = plottableFeatures.map { it.name },
                    initialValue = selectedFeature!!.name,
                    onValueSelected = {
                        if (isPlotting) {
                            viewModel.stopPlotting(nodeId)
                        }
                        viewModel.setFeature(it)
                    })

                Icon(
                    modifier = Modifier
                        .size(size = LocalDimensions.current.iconNormal)
                        .clickable {
                            if (isPlotting) {
                                viewModel.stopPlotting(nodeId)
                            } else {
                                viewModel.startPlotting(nodeId)
                            }
                        },
                    painter = painterResource(
                        if (isPlotting) {
                            com.st.ui.R.drawable.ic_stop
                        } else {
                            com.st.ui.R.drawable.ic_play
                        }
                    ),
                    tint = PrimaryBlue,
                    contentDescription = null
                )

                Icon(
                    modifier = Modifier
                        .size(size = 32.dp)
                        .clickable {
                            showTools = !showTools
                        },
                    painter = painterResource(
                        R.drawable.ic_tools
                    ),
                    tint = PrimaryBlue,
                    contentDescription = null
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .vertical()
                            .rotate(-90f),
                        text = featureYLabel(selectedFeature!!),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )

                    BlueMSPlot(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(2f),
                        interpolationType = plotInterpolationType,
                        feature = selectedFeature!!,
                        viewModel = viewModel,
                        featureUpdate = featureUpdate,
                        showMaxMin = showMaxMin,
                        makeSnapShot = makeSnapShot,
                        onMakeSnapShotDone = {
                            makeSnapShot = false
                            showTools = false
                        },
                        onSaveSnapshot = { snap ->
                            viewModel.snap = snap
                            val fileName =
                                "SnapShot_${selectedFeature!!.name}_${Date()}.png".replace(' ', '-')
                            pickFileLauncher.launch(fileName)
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showTools) {
                        Icon(
                            modifier = Modifier
                                .size(size = 32.dp)
                                .padding(bottom = LocalDimensions.current.paddingNormal)
                                .clickable {
                                    showSettingsDialog = true
                                    showTools = false
                                },
                            painter = painterResource(
                                R.drawable.ic_settings
                            ),
                            tint = PrimaryBlue,
                            contentDescription = null
                        )
                        Icon(
                            modifier = Modifier
                                .size(size = 32.dp)
                                .padding(bottom = LocalDimensions.current.paddingNormal)
                                .clickable {
                                    makeSnapShot = true
                                },
                            painter = painterResource(
                                R.drawable.ic_photo_camera
                            ),
                            tint = PrimaryBlue,
                            contentDescription = null
                        )
                        Icon(
                            modifier = Modifier
                                .size(size = 32.dp)
                                .clickable {
                                    showMaxMin = !showMaxMin
                                    showTools = false
                                },
                            painter = painterResource(
                                R.drawable.ic_info
                            ),
                            tint = PrimaryBlue,
                            contentDescription = null
                        )
                    }
                    //This is only for having the column with the right width...
                    Icon(
                        modifier = Modifier
                            .size(size = 32.dp)
                            .graphicsLayer(alpha = 0f),
                        painter = painterResource(
                            R.drawable.plot_icon
                        ),
                        tint = PrimaryBlue,
                        contentDescription = null
                    )
                }
            }

            if (featureUpdate == null) {
                plotDesc = WAITING_FEATURE_VALUE
            } else {
                val plotDescValue = featureUpdate!!.toPlotDesc(feature = selectedFeature!!)

                plotDescValue?.let {
                    plotDesc = it
                }
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = plotDesc,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End
            )

        }

        if (showSettingsDialog) {
            AlertDialog(
                modifier = Modifier.alpha(0.9f),
                onDismissRequest = {
                    showSettingsDialog = false
                },
                confirmButton = {
                    BlueMsButton(
                        onClick = {
                            showSettingsDialog = false
                        },
                        text = "OK"
                    )
                },
                title = {
                    Text(
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        text = "Plot Configuration"
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = LocalDimensions.current.paddingNormal),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal)
                    ) {

                        var textSecondsToPlot by remember { mutableStateOf(viewModel.secondsToPlot.toString()) }

                        var textMinValue by remember {
                            mutableStateOf(viewModel.minValue.toString())
                        }

                        var textMaxValue by remember {
                            mutableStateOf(viewModel.maxValue.toString())
                        }
                        var autoScaleEnable by remember {
                            mutableStateOf(viewModel.autoScaleEnable)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                LocalDimensions.current.paddingNormal
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.bodySmall,
                                color = Grey6,
                                text = "Plot Seconds"
                            )

                            OutlinedTextField(
                                modifier = Modifier
                                    .weight(2f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodySmall,
                                value = textSecondsToPlot,
                                //placeholder = { Text("5", color = Grey6) },
                                onValueChange = {
                                    textSecondsToPlot = it
                                    val value = it.toIntOrNull()
                                    value?.let { second ->
                                        viewModel.secondsToPlot = second
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                LocalDimensions.current.paddingNormal
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.bodySmall,
                                color = Grey6,
                                text = "AutoScale"
                            )

                            Checkbox(checked = autoScaleEnable, onCheckedChange = {
                                autoScaleEnable = !autoScaleEnable
                                viewModel.autoScaleValue(autoscale = autoScaleEnable)
                            })
                        }

                        AnimatedVisibility(
                            visible = !autoScaleEnable,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingNormal)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        LocalDimensions.current.paddingNormal
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Grey6,
                                        text = "Max"
                                    )

                                    OutlinedTextField(
                                        modifier = Modifier
                                            .weight(2f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        value = textMaxValue,
                                        //placeholder = { Text("5", color = Grey6) },
                                        onValueChange = {
                                            textMaxValue = it
                                            val value = it.toFloatOrNull()
                                            value?.let { min ->
                                                viewModel.maxValue(max = min)
                                            }
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        LocalDimensions.current.paddingNormal
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Grey6,
                                        text = "Min"
                                    )

                                    OutlinedTextField(
                                        modifier = Modifier
                                            .weight(2f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        value = textMinValue,
                                        //placeholder = { Text("5", color = Grey6) },
                                        onValueChange = {
                                            textMinValue = it
                                            val value = it.toFloatOrNull()
                                            value?.let { min ->
                                                viewModel.minValue(min = min)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                LocalDimensions.current.paddingNormal
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.bodySmall,
                                color = Grey6,
                                text = "Interpolation"
                            )

                            SingleChoiceSegmentedButtonRow {
                                PlotInterpolationType.entries.forEachIndexed { index, type ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = PlotInterpolationType.entries.size
                                        ),
                                        onClick = { plotInterpolationType = type },
                                        selected = plotInterpolationType == type,
                                        label = {
                                            Text(
                                                style = MaterialTheme.typography.bodySmall,
                                                text = type.name.lowercase(getDefault())
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                style = MaterialTheme.typography.titleLarge,
                text = "No features to plot"
            )
        }
    }
}

fun Modifier.vertical() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }

private fun featureYLabel(feature: Feature<*>): String {
    val items = feature.fieldsDesc()
    val unit = items.values.firstOrNull()
    return if (unit.isNullOrEmpty()) {
        feature.name
    } else {
        "${feature.name} (${unit})"
    }
}