/*
 * Copyright (c) 2022(-0001) STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */
package com.st.neai_classification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.st.blue_sdk.BlueManager
import com.st.blue_sdk.features.Feature
import com.st.blue_sdk.features.extended.neai_class_classification.NeaiClassClassification
import com.st.blue_sdk.features.extended.neai_class_classification.NeaiClassClassificationInfo
import com.st.blue_sdk.features.extended.neai_class_classification.request.WriteStartClassificationCommand
import com.st.blue_sdk.features.extended.neai_class_classification.request.WriteStopClassificationCommand
import com.st.neai_classification.model.NeaiCustomClassName
import com.st.preferences.StPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject


const val USER_CUSTOM_NEAI_CLASS_NAMES_KEY = "user_custom_nei_class_names"

@HiltViewModel
class NeaiClassificationViewModel @Inject constructor(
    private val blueManager: BlueManager,
    private val stPreferences: StPreferences,
    private val coroutineScope: CoroutineScope
) :
    ViewModel() {

    private val features = mutableListOf<Feature<*>>()

    private val _classificationData =
        MutableStateFlow<NeaiClassClassificationInfo?>(
            null
        )
    val classificationData: StateFlow<NeaiClassClassificationInfo?>
        get() = _classificationData.asStateFlow()


    fun writeStopClassificationCommand(nodeId: String) {
        blueManager.nodeFeatures(nodeId = nodeId).find {
            it.name == NeaiClassClassification.NAME
        }?.let {
            val feature = it as NeaiClassClassification

            viewModelScope.launch {
                blueManager.writeFeatureCommand(
                    nodeId = nodeId,
                    featureCommand = WriteStopClassificationCommand(feature = feature)
                )
            }
        }
    }

    var neaiCustomClassName = NeaiCustomClassName()

    fun writeStartClassificationCommand(nodeId: String) {
        blueManager.nodeFeatures(nodeId = nodeId).find {
            it.name == NeaiClassClassification.NAME
        }?.let {
            val feature = it as NeaiClassClassification

            viewModelScope.launch {
                blueManager.writeFeatureCommand(
                    nodeId = nodeId,
                    featureCommand = WriteStartClassificationCommand(feature = feature)
                )
            }
        }
    }

    fun readCustomNames() {
        val jsonDec = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        val serializedString =
            stPreferences.getCustomStringFromKey(USER_CUSTOM_NEAI_CLASS_NAMES_KEY)

        serializedString?.let { neaiCustomClassNameString ->
            try {
                neaiCustomClassName = jsonDec.decodeFromString<NeaiCustomClassName>(
                    neaiCustomClassNameString
                )
            } catch (e: Exception) {
                Log.i("NeaiClassificationViewModel", e.stackTraceToString())
            }
        }
    }

    fun saveCustomNames() {
        val jsonEnc = Json { encodeDefaults = true }

        val serializedString =
            jsonEnc.encodeToString(neaiCustomClassName)

        stPreferences.setCustomStringForKey(
            USER_CUSTOM_NEAI_CLASS_NAMES_KEY,
            serializedString
        )
    }


    fun startDemo(nodeId: String) {

        if (features.isEmpty()) {
            blueManager.nodeFeatures(nodeId).firstOrNull { it.name == NeaiClassClassification.NAME }
                ?.also {
                    features.add(it)
                }
        }

        viewModelScope.launch {
            blueManager.getFeatureUpdates(nodeId, features).collect {
                val data = it.data
                if (data is NeaiClassClassificationInfo) {
                    _classificationData.emit(data)
                }
            }
        }
    }

    fun stopDemo(nodeId: String) {
        coroutineScope.launch {
            blueManager.disableFeatures(nodeId, features)
        }
    }
}
