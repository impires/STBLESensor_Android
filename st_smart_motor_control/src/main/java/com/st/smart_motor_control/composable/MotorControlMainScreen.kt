package com.st.smart_motor_control.composable

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.st.blue_sdk.board_catalog.models.DtmiContent
import com.st.pnpl.composable.PnPLInfoWarningSpontaneousMessage
import com.st.smart_motor_control.MotorControlConfig
import com.st.smart_motor_control.MotorControlNavKey
import com.st.smart_motor_control.MotorControlTagsNavKey
import com.st.smart_motor_control.R
import com.st.smart_motor_control.SmartMotorControlViewModel
import com.st.smart_motor_control.model.MotorControlFault
import com.st.smart_motor_control.motorControlScreen
import com.st.smart_motor_control.motorControlTagsScreen
import com.st.ui.composables.BlueMSPullToRefreshBox
import com.st.ui.composables.BlueMsButton
import com.st.ui.composables.CommandRequest
import com.st.ui.composables.ComposableLifecycle
import com.st.ui.theme.ErrorText
import com.st.ui.theme.LocalDimensions
import com.st.ui.theme.PrimaryBlue
import com.st.ui.theme.SecondaryBlue
import com.st.ui.theme.Shapes
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotorControlMainScreen(
    modifier: Modifier,
    viewModel: SmartMotorControlViewModel,
    nodeId: String
) {

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.startDemo(nodeId = nodeId)
            Lifecycle.Event.ON_STOP -> viewModel.stopDemo(nodeId = nodeId)
            else -> Unit
        }
    }

    val isLogging by viewModel.isLogging.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val status by viewModel.componentStatusUpdates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSDCardInserted by viewModel.isSDCardInserted.collectAsStateWithLifecycle()


    val faultStatus by viewModel.faultStatus.collectAsStateWithLifecycle()
    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val speedRef by viewModel.speedRef.collectAsStateWithLifecycle()
    val speedMeas by viewModel.speedMeas.collectAsStateWithLifecycle()
    val busVoltage by viewModel.busVoltage.collectAsStateWithLifecycle()
    val neaiClassName by viewModel.neaiClassName.collectAsStateWithLifecycle()
    val neaiClassProb by viewModel.neaiClassProb.collectAsStateWithLifecycle()

    val cubeAiClassName by viewModel.cubeAiClassName.collectAsStateWithLifecycle()
    val cubeAiClassProb by viewModel.cubeAiClassProb.collectAsStateWithLifecycle()

    val isMotorRunning by viewModel.isMotorRunning.collectAsStateWithLifecycle()

    val motorSpeed by viewModel.motorSpeed.collectAsStateWithLifecycle()

    val motorSpeedControl by viewModel.motorSpeedControl.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isConnectionLost by viewModel.isConnectionLost.collectAsStateWithLifecycle()


    val context = LocalContext.current


    SmartMotorControlScreen(
        modifier = modifier,
        tags = tags,
        status = status,
        isSDCardInserted = isSDCardInserted,
        isLogging = isLogging,
        isLoading = isLoading,
        isBetaApplication = viewModel.isBeta,
        onValueChange = { name, value ->
            if (isLoading.not()) {
                viewModel.sendChange(
                    nodeId = nodeId,
                    name = name,
                    value = value
                )
            }
        },
        onSendCommand = { name, value ->
            if (isLoading.not()) {
                viewModel.sendCommand(
                    nodeId = nodeId,
                    name = name,
                    value = value
                )
            }
        },
        onStartStopLog = {
            if (it) {
                if (isLogging.not() && isLoading.not()) {
                    viewModel.startLog(nodeId)
                }
            } else {
                if (isMotorRunning.not() && isLoading.not()) {
                    viewModel.stopLog(nodeId)
                } else {
                    Toast.makeText(
                        context,
                        "Motor is still Running...\nStop before the Motor",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onRefresh = {
            if (isLogging.not() && isLoading.not()) {
                viewModel.refresh(nodeId)
            }
        },
        faultStatus = faultStatus,
        temperature = temperature,
        speedRef = speedRef,
        speedMeas = speedMeas,
        busVoltage = busVoltage,
        neaiClassName = neaiClassName,
        neaiClassProb = neaiClassProb,
        cubeAiClassName = cubeAiClassName,
        cubeAiClassProb = cubeAiClassProb,
        isMotorRunning = isMotorRunning,
        motorSpeed = motorSpeed,
        motorSpeedControl = motorSpeedControl,
        temperatureUnit = viewModel.temperatureUnit,
        speedRefUnit = viewModel.speedRefUnit,
        speedMeasUnit = viewModel.speedMeasUnit,
        busVoltageUnit = viewModel.busVoltageUnit
    )

    statusMessage?.let {
        PnPLInfoWarningSpontaneousMessage(
            messageType = statusMessage!!,
            onDismissRequest = { viewModel.cleanStatusMessage() })
    }

    if (isConnectionLost) {
        BasicAlertDialog(onDismissRequest = { viewModel.resetConnectionLost() })
        {
            Surface(
                modifier = Modifier
                    //.wrapContentWidth()
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = Shapes.medium
            ) {
                Column(modifier = Modifier.padding(all = LocalDimensions.current.paddingMedium)) {
                    Text(
                        text = "Warning:",
                        modifier = Modifier.padding(bottom = LocalDimensions.current.paddingNormal),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        letterSpacing = 0.15.sp,
                        color = ErrorText
                    )
                    Text(
                        text = "Lost Connection with the Node",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.25.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = LocalDimensions.current.paddingSmall,
                                end = LocalDimensions.current.paddingSmall,
                                top = LocalDimensions.current.paddingNormal
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        BlueMsButton(
                            modifier = Modifier.padding(end = LocalDimensions.current.paddingSmall),
                            text = "Close",
                            onClick = {
                                viewModel.disconnect()
                            }
                        )

                        BlueMsButton(
                            modifier = Modifier.padding(end = LocalDimensions.current.paddingSmall),
                            text = "Cancel",
                            onClick = {
                                viewModel.resetConnectionLost()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SmartMotorControlScreen(
    modifier: Modifier,
    tags: List<Pair<DtmiContent.DtmiComponentContent, DtmiContent.DtmiInterfaceContent>> = emptyList(),
    status: List<JsonObject>,
    isLogging: Boolean,
    isSDCardInserted: Boolean = false,
    isLoading: Boolean = false,
    isBetaApplication: Boolean,
    onValueChange: (String, Pair<String, Any>) -> Unit,
    onSendCommand: (String, CommandRequest?) -> Unit,
    onStartStopLog: (Boolean) -> Unit = { /**NOOP **/ },
    onRefresh: () -> Unit = { /**NOOP **/ },
    faultStatus: MotorControlFault = MotorControlFault.None,
    temperature: Int? = null,
    speedRef: Int? = null,
    speedMeas: Int? = null,
    busVoltage: Int? = null,
    neaiClassName: String? = null,
    neaiClassProb: Float? = null,
    cubeAiClassName: String? = null,
    cubeAiClassProb: Float? = null,
    temperatureUnit: String,
    speedRefUnit: String,
    speedMeasUnit: String,
    busVoltageUnit: String,
    isMotorRunning: Boolean = false,
    motorSpeed: Int = 1024,
    motorSpeedControl: DtmiContent.DtmiPropertyContent.DtmiIntegerPropertyContent? = null,
) {
    val sensorsActuatorsTitle = stringResource(id = R.string.st_motor_control_configuration)
    val tagsTitle = stringResource(id = R.string.st_motor_control_tags)
    var currentTitle by remember { mutableStateOf(sensorsActuatorsTitle) }
    var openStopDialog by remember { mutableStateOf(value = false) }

    val pullRefreshState = rememberPullToRefreshState()

    val backState =
        rememberNavBackStack(if (isLogging) MotorControlTagsNavKey else MotorControlNavKey)

    val lastState = backState.lastOrNull()
    val selectedIndex by remember(key1 = lastState) {
        derivedStateOf {
            if (lastState == MotorControlTagsNavKey) 1 else 0
        }
    }

    val haptic = LocalHapticFeedback.current

    val context = LocalContext.current


    val lazyState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            if (MotorControlConfig.motorControlTabBar != null) {
                MotorControlConfig.motorControlTabBar?.invoke(currentTitle, isLoading)
            } else {
                PrimaryTabRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                selectedTabIndex = selectedIndex,
                                matchContentSize = false
                            ),
                            width = 60.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            height = 4.dp,
                            shape = Shapes.medium
                        )
                    }) {
                    Tab(
                        selected = 0 == selectedIndex,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentTitle = sensorsActuatorsTitle
                            backState.removeLastOrNull()
                            backState.add(MotorControlNavKey)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_motor_control),
                                contentDescription = stringResource(id = R.string.st_motor_control)
                            )
                        },
                        text = { Text(text = stringResource(id = R.string.st_motor_control)) },
                        enabled = !isLoading
                    )

                    Tab(
                        selected = 1 == selectedIndex,
                        onClick = {
                            if (!isLoading) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentTitle = tagsTitle
                                backState.removeLastOrNull()
                                backState.add(MotorControlTagsNavKey)
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tags),
                                contentDescription = stringResource(id = R.string.st_motor_control_tags)
                            )
                        },
                        text = { Text(text = stringResource(id = R.string.st_motor_control_tags)) },
                        enabled = !isLoading
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                containerColor = SecondaryBlue,
                expanded = !lazyState.isScrollInProgress,
                onClick = {
                    if (isSDCardInserted) {
                        if (isLogging) {
                            onStartStopLog(false)
                            openStopDialog = MotorControlConfig.showStopDialog
                        } else {
                            onStartStopLog(true)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.st_motor_control_missingSdCard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                icon = {
                    Icon(
                        tint = PrimaryBlue,
                        imageVector = if (isLogging) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                text = { Text(text = if (isLogging) "Stop" else "Start") },
            )
        }
    ) { paddingValues ->
        BlueMSPullToRefreshBox(
            modifier = modifier
                .consumeWindowInsets(paddingValues)
                .padding(paddingValues),
            state = pullRefreshState,
            isRefreshing = isLoading,
            isBetaRelease = isBetaApplication,
            onRefresh = onRefresh
        ) {
            NavDisplay(
                backStack = backState,
                onBack = { backState.removeLastOrNull() },
                entryProvider = entryProvider {
                    motorControlScreen(
                        isLoading,
                        faultStatus,
                        temperature,
                        speedRef,
                        speedMeas,
                        busVoltage,
                        neaiClassName,
                        neaiClassProb,
                        cubeAiClassName,
                        cubeAiClassProb,
                        isMotorRunning,
                        isLogging,
                        motorSpeed,
                        motorSpeedControl,
                        onSendCommand,
                        onValueChange,
                        temperatureUnit,
                        speedRefUnit,
                        speedMeasUnit,
                        busVoltageUnit
                    )

                    motorControlTagsScreen(
                        lazyState,
                        tags,
                        status,
                        isLoading,
                        onValueChange,
                        onSendCommand
                    )
                })
        }
    }

    if (openStopDialog) {
        StopLoggingDialog(
            onDismissRequest = { openStopDialog = false }
        )
    }
}