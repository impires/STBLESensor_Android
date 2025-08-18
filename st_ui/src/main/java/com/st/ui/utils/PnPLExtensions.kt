package com.st.ui.utils

import androidx.compose.ui.text.intl.Locale

val Map<String, String>.localizedDisplayName: String
    get() {
        if (isEmpty()) return ""
        val locale = Locale.current.language.uppercase()
        if (containsKey(locale)) {
            return getValue(locale)
        }
        return getValue(keys.first())
    }

fun Map<String, String>.localizedDisplayNameSensor(isVespucci: Boolean): String {
    val splitList = localizedDisplayName.split("_")
    return if (splitList.size == 3) {
        var lowHigh = splitList[1]
        if(isVespucci) {
            //With Vespucci replace l and h
            when (lowHigh.lowercase()) {
                "l" -> lowHigh = "Low"
                "h" -> lowHigh = "High"
                else -> {}
            }
        }
        splitList[0] + " " + lowHigh
    } else {
        splitList.first()
    }
}

val Map<String, String>.sensorDisplayName: String
    get() {
        val splitList = localizedDisplayName.split("_")
        return splitList.first()
    }
