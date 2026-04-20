package com.st.multinode

interface AcquisitionServiceController {
    fun startLogging(
        nodeIds: List<String>,
        enableServer: Boolean = false,
        maxPayloadSize: Int = 248,
        maxConnectionRetries: Int = 3
    )
    fun stopLogging(nodeIds: List<String>)
    fun stopAll()
}