package com.st.plot.utils

internal data class BlueMSPlotEntry(
    val x: Long, val y: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlueMSPlotEntry

        if (x != other.x) return false
        if (!y.contentEquals(other.y)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.contentHashCode()
        return result
    }
}