package com.st.external_app.model

enum class ExternalAppType(val code: Int) {
    UNDEF(0),
    AIOTCRAFT(1),
    ROBOTICS(2),
    BLESENSORCLASSIC(3);

    companion object {
        fun fromInt(code: Int): ExternalAppType {
            return when (code) {
                1 -> AIOTCRAFT
                2 -> ROBOTICS
                3 -> BLESENSORCLASSIC
                else -> UNDEF
            }
        }
    }
}