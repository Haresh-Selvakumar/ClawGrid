package com.example.data.remote

data class SensorRuleEvaluation(val step: Int, val requestedOn: Boolean, val result: Int,
    val startUs: Long, val elapsedUs: Long)
data class SensorEvaluation(val order: Int, val cycle: Long, val snapshotMs: Long, val startedUs: Long,
    val clockValid: Boolean, val lightAge: Long?, val temperatureAttemptAge: Long?,
    val light: SensorRuleEvaluation, val fan: SensorRuleEvaluation)
