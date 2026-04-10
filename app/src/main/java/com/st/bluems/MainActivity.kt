/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */
package com.st.bluems

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.st.bluems.ui.home.HomeScreen
import com.st.bluems.ui.home.HomeViewModel
import com.st.core.api.ApplicationAnalyticsService.ApplicationNameEtna
import com.st.terms.StTermsConfig
import com.st.terms.composable.LicenseAgreementScreen
import com.st.ui.theme.BlueMSTheme
import com.st.ui.theme.Grey0
import com.st.user_profiling.ProfileViewModel
import com.st.user_profiling.StUserProfilingConfig
import com.st.user_profiling.composable.UserProfilingNavigationScreen
import com.st.user_profiling.model.LevelProficiency
import com.st.user_profiling.model.ProfileType
import com.st.welcome.StWelcomeConfig
import com.st.welcome.composable.WelcomeScreen
import com.st.welcome.model.WelcomePage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import kotlin.getValue


@Serializable
data object WelcomeNavKey : NavKey

@Serializable
data object TermsNavKey : NavKey

@Serializable
data object UserProfilingNavKey : NavKey

@Serializable
data object BlueMSApplicationNavKey : NavKey

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val nfcViewModel: NFCConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            viewModel.initApplicationAnalytics(
                ApplicationNameEtna.STBLESensorDev,
                application,
                this
            )
        } else {
            viewModel.initApplicationAnalytics(
                ApplicationNameEtna.STBLESensorRel,
                application,
                this
            )
        }

        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }

        val versionName = pInfo.versionName?.replace('.', '_') ?: "NoName"
        val packageName = pInfo.packageName.split('.').last()

        val keySearched = "${packageName}_${versionName}"

        //for using NFC deep Link node auto-connect
        val nfcIntent = intent
        val appLinkData = nfcIntent.data
        if (appLinkData != null) {

            val sPairingPin: ByteArray? = appLinkData.getQueryParameter("Pin")?.toByteArray(
                StandardCharsets.UTF_8
            )
            nfcViewModel.setNFCPairingPin(sPairingPin)

            val mNodeTag: String? = appLinkData.getQueryParameter("Add")
            nfcViewModel.setNFCNodeId(mNodeTag)
        }

        viewModel.reportApplicationAnalytics(applicationContext)

        setContent {
            BlueMSTheme {
                val backState =
                    rememberNavBackStack(
                        when {
                            viewModel.shouldShowTerms -> TermsNavKey
                            //viewModel.shouldShowWelcome -> WelcomeNavKey
                            //We propose again the Terms Screen for avoiding problem for lateinit var...
                            viewModel.shouldShowWelcome -> TermsNavKey
                            viewModel.shouldShowProfile -> UserProfilingNavKey
                            else -> BlueMSApplicationNavKey
                        }
                    )


                LaunchedEffect(key1 = Unit) {
                    setUpTerms(backState)
                    setUpWelcome(backState)
                    setUpUserProfiling(backState)
                }

                NavDisplay(
                    modifier = Modifier
                        .background(
                            Grey0
                        ),
                    backStack = backState,
//                    onBack = {
//                        backState.removeLastOrNull()
//                    },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<TermsNavKey> {
                            LicenseAgreementScreen(onLicenseAgree = {
                                StTermsConfig.onDone(true)
                            })
                        }

                        entry<WelcomeNavKey> {
                            WelcomeScreen(
                                welcomePages = StWelcomeConfig.welcomePages,
                                onSkip = StWelcomeConfig.onSkip
                            )
                        }

                        entry<UserProfilingNavKey> {
                            val viewModel: ProfileViewModel by viewModels()
                            UserProfilingNavigationScreen(
                                viewModel = viewModel
                            )
                        }

                        entry<BlueMSApplicationNavKey> {
                            val homeViewModel: HomeViewModel = hiltViewModel()
                            HomeScreen(homeViewModel = homeViewModel, nfcViewModel = nfcViewModel)
                        }
                    }
                )
            }
        }
    }


    private fun setUpUserProfiling(backState: NavBackStack<NavKey>) {
        LevelProficiency.fromString(viewModel.level)?.let { level ->
            StUserProfilingConfig.defaultLevelProficiency = level
        }

        ProfileType.fromString(viewModel.type)?.let { type ->
            StUserProfilingConfig.defaultProfileType = type
        }

        StUserProfilingConfig.onDone = { level: LevelProficiency, type: ProfileType ->
            viewModel.profileShow(level = level, type = type)
            backState.removeLastOrNull()
            backState.add(BlueMSApplicationNavKey)
        }
    }

    private fun setUpWelcome(backState: NavBackStack<NavKey>) {
        StWelcomeConfig.welcomePages = listOf(
            WelcomePage(
                title = getString(R.string.st_welcome_title1),
                description = getString(R.string.st_welcome_message1),
                drawableRes = R.drawable.welcome1
            ),
            WelcomePage(
                title = getString(R.string.st_welcome_title2),
                description = getString(R.string.st_welcome_message2),
                drawableRes = R.drawable.welcome2
            ),
            WelcomePage(
                title = getString(R.string.st_welcome_title3),
                description = getString(R.string.st_welcome_message3),
                drawableRes = R.drawable.welcome3
            ),
            WelcomePage(
                title = getString(R.string.st_welcome_title4),
                description = getString(R.string.st_welcome_message4),
                drawableRes = R.drawable.welcome4
            )
        )

        StWelcomeConfig.onSkip = {
            viewModel.welcomeShow()
            backState.removeLastOrNull()
            backState.add(UserProfilingNavKey)
        }
    }

    private fun setUpTerms(backState: NavBackStack<NavKey>) {
        StTermsConfig.onDone = { isAccepted ->
            viewModel.termsAccepted(isAccepted)

            backState.removeLastOrNull()
            backState.add(WelcomeNavKey)
        }
    }
}
