package com.example.domain

import java.math.BigDecimal

/** Exact validated tenths; never silently round or substitute a demo default. */
data class SensorAutoSettings(val onDeciC: Int = 280, val offDeciC: Int = 260,
    val overrideSeconds: Int = 60, val sensorsFirst: Boolean = false) {
    init {
        require(onDeciC in 1..500 && offDeciC in 0 until onDeciC) { "OFF must be below ON, within 0–50 °C." }
        require(overrideSeconds in 3..3600) { "Override must be 3–3600 whole seconds." }
    }
    fun packed(): Long = offDeciC.toLong() or (onDeciC.toLong() shl 9) or
        (overrideSeconds.toLong() shl 18) or (if (sensorsFirst) 1L shl 30 else 0)
    companion object {
        fun parse(on: String, off: String, seconds: String, sensorsFirst: Boolean): SensorAutoSettings {
            fun tenths(text: String) = BigDecimal(text.trim()).movePointRight(1).intValueExact()
            return SensorAutoSettings(tenths(on), tenths(off), seconds.trim().toInt(), sensorsFirst)
        }
        fun unpack(value: Long): SensorAutoSettings {
            require(value in 0..0x7fffffffL)
            return SensorAutoSettings(((value shr 9) and 511).toInt(), (value and 511).toInt(),
                ((value shr 18) and 4095).toInt(), value and (1L shl 30) != 0L)
        }
    }
}
