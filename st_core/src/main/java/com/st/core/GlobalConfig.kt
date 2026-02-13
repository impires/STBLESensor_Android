package com.st.core

object GlobalConfig {

    //For IoT Craft for filtering Devices after Firmware Update/Change
    var deviceTypes: Array<String>? = null
    //This single function make the disconnection and Navigate back for Navigation2
    var navigateBack: ((nodeId: String) -> Unit)? = null

    //For Navigation 3 ... we have 2 different API for having more freedom for the backstate
    var removeBackState: (() -> Unit) = {}
    var disconnectFromNode: ((nodeId: String) -> Unit) = {}
    val navigateBack3: ((nodeId: String) -> Unit) = {
        disconnectFromNode(it)
        removeBackState()
    }
}