/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */
package com.st.external_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.st.core.ARG_EXTERNAL_APP_TYPE
import com.st.core.ARG_NODE_ID
import com.st.external_app.composable.ExternalAppDemoContent
import com.st.external_app.model.ExternalAppDetailType
import com.st.external_app.model.ExternalAppType
import com.st.ui.theme.BlueMSTheme
import com.st.ui.utils.asString
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExternalAppDemoFragment : Fragment() {


    private val viewModel: ExternalAppViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val nodeId = arguments?.getString(ARG_NODE_ID)
            ?: throw IllegalArgumentException("Missing string $ARG_NODE_ID arguments")

        val externalAppTypeCode = arguments?.getInt(ARG_EXTERNAL_APP_TYPE)

        val externalAppType: ExternalAppType =
            if (externalAppTypeCode == null) {
                ExternalAppType.UNDEF
            } else {
                ExternalAppType.fromInt(externalAppTypeCode)
            }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BlueMSTheme {
                    ExternalAppDemoScreen(
                        modifier = Modifier
                            .fillMaxSize(),
                        viewModel = viewModel,
                        externalAppDetail = ExternalAppMap[externalAppType]
                    )
                }
            }
        }
    }
}

val  ExternalAppAIoTCraft = ExternalAppDetailType(
    appIcon = R.drawable.st_aiot_craft_app_icon,
    appTitle = "ST AIoT Craft",
    appShortDescription = "Application for creating AI and IoT solutions",
    appLongDescription = "ST AIoT Craft is a cloud development environment to create in-sensors AI and sensors-to-cloud solutions with ST components. The STAIoT Craft App allows to interact with the cloud environment to deploy and monitor projects. Start to use the ST AIoT Craft App with project examples and ST developers' boards; profile your built in-sensor AI solution with machine learning core technology inside ST MEMS sensors and deploy it on the IoT sensor node.",
    appLink = "https://play.google.com/store/apps/details?id=com.st.staiotcraft"
)

val ExternalAppRobotics = ExternalAppDetailType(
    appIcon = R.drawable.st_robotics_app_icon,
    appTitle = "ST Robotics",
    appShortDescription = "Application for scanning and controlling robotics kits",
    appLongDescription = "The ST Robotics Application for Android enables users to configure and operate various robotic kits and boards, such as the Robotics Evaluation Kit. This application facilitates the discovery, connection, and control of these robotic kits.",
    appLink = "https://play.google.com/store/apps/details?id=com.st.robotics"
)

val  ExternalAppBlueMSClassic = ExternalAppDetailType(
    appIcon = R.drawable.st_ble_sensor_classic_app_icon,
    appTitle = "ST BlueMS Classic (Legacy)",
    appShortDescription = "Application used for connecting ST Development boards",
    appLongDescription = "This demo is no more supported on current version of the app.\nTry to use the ST BLE Sensor Classic application for this old demo.",
    appLink = "https://play.google.com/store/apps/details?id=com.st.bluemsclassic&hl=en&pli=1"
)

val ExternalAppMap: Map<ExternalAppType, ExternalAppDetailType> = mapOf(
    ExternalAppType.AIOTCRAFT to ExternalAppAIoTCraft,
    ExternalAppType.ROBOTICS to ExternalAppRobotics,
    ExternalAppType.BLESENSORCLASSIC to ExternalAppBlueMSClassic,
    ExternalAppType.UNDEF to ExternalAppDetailType(
        appIcon = R.drawable.external_app_icon,
        appTitle = "Application Not Recognized",
        appShortDescription = "Something wrong... this should not happen",
        appLongDescription = LoremIpsum(32).asString(),
        appLink = "https://play.google.com/store"
    )
)

@Composable
fun ExternalAppDemoScreen(
    modifier: Modifier,
    viewModel: ExternalAppViewModel,
    externalAppDetail: ExternalAppDetailType?

) {
    ExternalAppDemoContent(
        modifier = modifier,
        viewModel = viewModel,
        externalAppDetail = externalAppDetail
    )
}
