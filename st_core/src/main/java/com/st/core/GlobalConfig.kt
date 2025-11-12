package com.st.core

object GlobalConfig {

    //For IoT Craft for filtering Devices after Firmware Update/Change
    var deviceTypes: Array<String>? = null
    var navigateBack: ((nodeId: String) -> Unit)? = null
}