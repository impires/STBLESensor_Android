package com.st.multinode.logging

interface NodeLogEngine {

    suspend fun prepareFlow(nodeId: String, flowJson: String)

    suspend fun startFlow(nodeId: String, flowJson: String)

    suspend fun stopFlow(nodeId: String)

    suspend fun requestStatus(nodeId: String)

    suspend fun release(nodeId: String)

    fun getLogComponentName(nodeId: String): String

    fun getControllerComponentName(nodeId: String): String
}