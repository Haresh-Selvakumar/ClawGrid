package com.example.domain

import java.util.Calendar
import java.util.Locale

enum class PlanningRule { SCHEDULE, CONTINUOUS_CAP, OFF_COUNTDOWN }

/** Observed software run only; a connection made mid-ON cannot infer its start. */
class ContinuousRunObservation {
    private var boot: Long? = null
    private var baseline: Long? = null
    private var previousTotal: Long? = null
    fun clear() { boot=null; baseline=null; previousTotal=null }
    fun observe(currentBoot: Long, on: Boolean, totalMs: Long, contiguous: Boolean): Long? {
        require(currentBoot>0 && totalMs>=0)
        if(boot!=currentBoot || !contiguous || previousTotal?.let { totalMs<it } == true) baseline=null
        boot=currentBoot; previousTotal=totalMs
        if(!on) { baseline=totalMs; return 0L }
        return baseline?.let { totalMs-it }
    }
}

/** Pure calculations; no dispatch and no fabricated appliance measurements. */
object PlanningMath {
    fun minutesToMillis(minutes: Long): Long {
        require(minutes in 1L..240L)
        return minutes*60000
    }
    fun authorizationValue(rule: PlanningRule, value: Long, dueWallMs: Long, nowWallMs: Long): Long {
        val result=if(rule==PlanningRule.SCHEDULE) dueWallMs-nowWallMs else value
        require(if(rule==PlanningRule.SCHEDULE) result in 1000L..86400000L else result in 60000L..14400000L)
        return result
    }
    fun duration(ms: Long): String {
        require(ms>=0)
        val seconds=(ms+999)/1000
        return String.format(Locale.US,"%d:%02d",seconds/60,seconds%60)
    }
    fun delayUntil(hour: Int, minute: Int, now: Long): Long {
        require(hour in 0..23 && minute in 0..59)
        val target=Calendar.getInstance().apply {
            timeInMillis=now; set(Calendar.HOUR_OF_DAY,hour); set(Calendar.MINUTE,minute)
            set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
            if(timeInMillis <= now) add(Calendar.DAY_OF_MONTH,1)
        }
        return (target.timeInMillis-now).also { require(it in 1000L..86400000L) }
    }
    fun estimateWh(onMs: Long, watts: Double): Double {
        require(onMs >= 0 && watts.isFinite() && watts in 0.0..10000.0)
        return onMs / 3600000.0 * watts
    }
}
