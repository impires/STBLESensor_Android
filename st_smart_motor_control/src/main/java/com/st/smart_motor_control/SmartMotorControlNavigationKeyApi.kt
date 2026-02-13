package com.st.smart_motor_control

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.st.blue_sdk.board_catalog.models.DtmiContent
import com.st.smart_motor_control.composable.MotorControl
import com.st.smart_motor_control.composable.MotorControlTags
import com.st.smart_motor_control.model.MotorControlFault
import com.st.ui.composables.CommandRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data object MotorControlNavKey : NavKey

@Serializable
data object MotorControlTagsNavKey : NavKey

@Composable
fun EntryProviderScope<NavKey>.motorControlTagsScreen(
    lazyState: LazyListState,
    tags: List<Pair<DtmiContent.DtmiComponentContent, DtmiContent.DtmiInterfaceContent>>,
    status: List<JsonObject>,
    isLoading: Boolean,
    onValueChange: (String, Pair<String, Any>) -> Unit,
    onSendCommand: (String, CommandRequest?) -> Unit
) {
    entry<MotorControlTagsNavKey> {
            MotorControlTags(
                lazyState = lazyState,
                tags = tags,
                status = status,
                isLoading = isLoading,
                onValueChange = onValueChange,
                onSendCommand = onSendCommand
            )
    }
}

@Composable
fun EntryProviderScope<NavKey>.motorControlScreen(
    isLoading: Boolean,
    faultStatus: MotorControlFault,
    temperature: Int?,
    speedRef: Int?,
    speedMeas: Int?,
    busVoltage: Int?,
    neaiClassName: String?,
    neaiClassProb: Float?,
    cubeAiClassName: String?,
    cubeAiClassProb: Float?,
    isMotorRunning: Boolean,
    isLogging: Boolean,
    motorSpeed: Int,
    motorSpeedControl: DtmiContent.DtmiPropertyContent.DtmiIntegerPropertyContent?,
    onSendCommand: (String, CommandRequest?) -> Unit,
    onValueChange: (String, Pair<String, Any>) -> Unit,
    temperatureUnit: String,
    speedRefUnit: String,
    speedMeasUnit: String,
    busVoltageUnit: String
) {
    entry<MotorControlNavKey> {
        MotorControl(
            isLoading = isLoading,
            faultStatus = faultStatus,
            temperature = temperature,
            speedRef = speedRef,
            speedMeas = speedMeas,
            busVoltage = busVoltage,
            neaiClassName = neaiClassName,
            neaiClassProb = neaiClassProb,
            cubeAiClassName = cubeAiClassName,
            cubeAiClassProb = cubeAiClassProb,
            isRunning = isMotorRunning,
            isLogging = isLogging,
            motorSpeed = motorSpeed,
            motorSpeedControl = motorSpeedControl,
            onSendCommand = onSendCommand,
            onValueChange = onValueChange,
            temperatureUnit = temperatureUnit,
            speedRefUnit = speedRefUnit,
            speedMeasUnit = speedMeasUnit,
            busVoltageUnit = busVoltageUnit
        )
    }
}