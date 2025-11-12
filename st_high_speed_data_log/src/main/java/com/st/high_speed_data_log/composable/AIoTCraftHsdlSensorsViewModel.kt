package com.st.high_speed_data_log.composable

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AIoTCraftHsdlSensorsViewModel : ViewModel() {
    private val _dialogShownOnce = MutableStateFlow(false)
    val dialogShownOnce: StateFlow<Boolean> = _dialogShownOnce

    fun markDialogAsShown() {
        _dialogShownOnce.value = true
    }
}