package com.st.demo_showcase.ui.composable

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.st.demo_showcase.ui.DemoShowCaseViewModel
import com.st.demo_showcase.ui.demo_list.*
import com.st.demo_showcase.ui.demo_show_case.DemoShowCaseFwDirectUpdateNavKey
import com.st.demo_showcase.ui.demo_show_case.DemoShowCaseUserProfilingNavKey
import com.st.ui.composables.JSON_FILE_TYPE
import com.st.ui.theme.Grey0

@Composable
fun DemoListNavKeyScreen(
    modifier: Modifier = Modifier,
    nodeId: String,
    viewModel: DemoShowCaseViewModel,
    externBackState: NavBackStack<NavKey>,
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isExpert by viewModel.isExpert.collectAsStateWithLifecycle()
    val demos by viewModel.availableDemo.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()
    val statusModelDTMI by viewModel.statusModelDTMI.collectAsStateWithLifecycle()
    val pinnedDevices by viewModel.pinnedDevices.collectAsStateWithLifecycle(
        emptyList()
    )

    val isBeta by viewModel.isBeta.collectAsStateWithLifecycle()

    val fwUpdateAvailable by viewModel.fwUpdateAvailable.collectAsStateWithLifecycle()

    val backState =
        rememberNavBackStack(DemoListNavKey)

//    LaunchedEffect(key1 = Unit) {
//        viewModel.setCurrentDemo(null)
//    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { fileUri ->
        if (fileUri != null) {
            viewModel.setDtmiModel(nodeId, fileUri)
        }
    }

    LaunchedEffect(key1 = backState.lastOrNull()) {
        viewModel.onNavKeyChange(backState)
    }

    NavDisplay(
        modifier = modifier
            .background(
                Grey0
            ),
        backStack = backState,
        onBack = {
            backState.removeLastOrNull()
            viewModel.setCurrentDemo(null)
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {

            //Demos List screen
            entry<DemoListNavKey> {
                DemoListScreen(
                    modifier = modifier,
                    device = device,
                    pinnedDevices = pinnedDevices,
                    isLoggedIn = isLoggedIn,
                    isExpert = isExpert,
                    isBetaApplication = isBeta,
                    fwUpdateAvailable = fwUpdateAvailable,
                    availableDemos = demos,
                    onDemoReordered = { from, to ->
                        viewModel.saveReorder(from, to)
                    },
                    onPinChange = { isPin ->
                        device?.device?.address?.let {
                            if (isPin) {
                                viewModel.addToPinDevices(it)
                            } else {
                                viewModel.removeFromPinDevices(it)
                            }
                        }
                    },
                    onDemoSelected = { selectedDemo ->
                        viewModel.setCurrentDemo(selectedDemo)

                        selectedDemo.navigateTo(
                            backState = backState,
                            nodeId = nodeId,
                            isExpert = isExpert
                        )
                    },
                    onLoginRequired = {
                        viewModel.login()
                    },
                    onExpertRequired = {
                        externBackState.add(DemoShowCaseUserProfilingNavKey)
                    },
                    onLastFwRequired = {

                        externBackState.add(
                            DemoShowCaseFwDirectUpdateNavKey(
                                nodeId,
                                viewModel.updateUrl.value
                            )
                        )
                    },
                    statusModelDTMI = statusModelDTMI,
                    onCustomDTMIClicked = { pickFileLauncher.launch(arrayOf(JSON_FILE_TYPE)) }
                )
            }

            //Demos
            FlowDemo()
            BeamForming()
            Environmental()
            Level()
            FitnessActivity()
            Compass()
            HighSpeedDataLog2()
            SimpleHighSpeedDataLog2()
            AIoTCraftHighSpeedDataLog2()
            BlueVoiceOpus()
            BlueVoiceFullDuplex()
            NavigationGesture()
            NEAIAnomalyDetection()
            NEAIClassification()
            NEAIExtrapolation()
            EventCounter()
            Piano()
            PnPL()
            Plot()
            NfcWriting()
            BinaryContent()
            ExtConfiguration()
            ToFObjectsDetection()
            ColorAmbientLight()
            GNSS()
            EletricChargeVariation()
            MotionIntensity()
            ActivityRecognition()
            CarryPosition()
            MemsGesture()
            MotionAlgorithms()
            Pedometer()
            ProximityGestureRecognition()
            Switch()
            Registers()
            AccelerationEvent()
            SourceLocalization()
            AudioClassification()
            LedControl()
            NodeStatus()
            TextualMonitor()
            HeartRate()
            SensorFusion()
            PredictedMaintenance()
            FftAmplitude()
            MultiNeuralNetwork()
            ExternalApplication()
            AssetTracking()
            RawPnPL()
            MotorControl()
            CloudAzureIoTCentral()
            CloudMQTT()
            MedicalSignal()
            FUOTA(externBackState,backState)
        },
        transitionSpec = {
            // Slide in from right when navigating forward
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { -it })
        },
        popTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(
                initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        })
}