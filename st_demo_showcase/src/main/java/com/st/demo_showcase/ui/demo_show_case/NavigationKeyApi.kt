package com.st.demo_showcase.ui.demo_show_case

import androidx.navigation3.runtime.NavKey
import com.st.demo_showcase.models.Demo
import kotlinx.serialization.Serializable

@Serializable
data object DemoShowCaseMainScreenNavKey : NavKey

@Serializable
data class DemoShowCaseFwDirectUpdateNavKey(
    val nodeId: String,
    val fwUrl: String = "",
    val fwLock: Boolean = false
) : NavKey

@Serializable
data class DemoShowCaseDebugConsoleNavKey(
    val nodeId: String
) : NavKey

@Serializable
data class DemoShowCasePnplSettingsNavKey(
    val nodeId: String,
    val demoName: String? = null,
    val currentDemo: Demo
) : NavKey

@Serializable
data class DemoShowCaseFwUpdateNavKey(
    val nodeId: String
) : NavKey

@Serializable
data class DemoShowCaseLogSettingsNavKey(
    val nodeId: String,
    val currentDemo: Demo?=null
) : NavKey

@Serializable
data object DemoShowCaseUserProfilingNavKey: NavKey

@Serializable
data object DemoShowCaseDownloadTermsNavKey : NavKey