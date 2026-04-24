package com.st.multinode.logging

import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.extended.ext_configuration.ExtConfiguration
import com.st.blue_sdk.features.extended.pnpl.PnPL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeLogEngineFactory @Inject constructor(
    private val blueManager: BlueManager,
    private val officialSdLogEngine: OfficialSdLogEngine,
    private val legacyExtConfigLogEngine: LegacyExtConfigLogEngine
) {

    fun get(nodeId: String): NodeLogEngine {
        val features = blueManager.nodeFeatures(nodeId)

        val hasPnpl = features.any { it is PnPL }
        if (hasPnpl) {
            return officialSdLogEngine
        }

        val hasExtConfig = features.any { it is ExtConfiguration }
        if (hasExtConfig) {
            return legacyExtConfigLogEngine
        }

        return officialSdLogEngine
    }
}