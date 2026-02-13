package com.st.neai_classification.model

import kotlinx.serialization.Serializable

@Serializable
data class NeaiCustomClassName(
    var useCustomNames: Boolean = true,
    var customNames: MutableList<String> = MutableList(8) { "CL ${it + 1}" }
)
