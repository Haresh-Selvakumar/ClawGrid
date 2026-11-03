package com.example
import com.example.domain.PlanningMath
import com.example.domain.PlanningRule
import com.example.domain.ContinuousRunObservation
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class PlanningTest {
    @Test fun energyUnitsAndInvalidInputs() {
        assertEquals(60.0,PlanningMath.estimateWh(3600000,60.0),0.00001)
        assertEquals(0.0,PlanningMath.estimateWh(0,60.0),0.0)
        for(w in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,10001.0)) assertTrue(runCatching { PlanningMath.estimateWh(1000,w) }.isFailure)
        assertTrue(runCatching { PlanningMath.estimateWh(-1,1.0) }.isFailure)
    }
    @Test fun scheduleNextOccurrenceAndBounds() {
        val now=Calendar.getInstance().apply { set(2026,10,1,12,0,0);set(Calendar.MILLISECOND,0) }.timeInMillis
        assertEquals(3600000L,PlanningMath.delayUntil(13,0,now))
        assertEquals(86400000L,PlanningMath.delayUntil(12,0,now))
        assertTrue(runCatching { PlanningMath.delayUntil(24,0,now) }.isFailure)
        assertTrue(runCatching { PlanningMath.delayUntil(12,60,now) }.isFailure)
    }
    private fun sample(features:Int=1,boot:Long=9,uptime:Long=100,id:Long=1)=JSONObject()
        .put("room","bedroom").put("known",true).put("authenticated",true).put("boot_id",boot)
        .put("sample_uptime_ms",uptime).put("age_ms",0).put("light",false).put("fan",false)
        .put("physical_outputs_enabled",false).put("last_command",id).put("features_version",features).apply {
            for(d in listOf("light","fan")) for(f in listOf("schedule_ms","schedule_action","schedule_result","control_source","energy_cap_ms")) put("${d}_$f",0)
        }
    @Test fun configurationNeedsAckAndFreshConfigThenDoesNotReplay()=runBlocking {
        var report=sample();var time=1000L;val calls=mutableListOf<Pair<String,String?>>()
        val tx=JSONObject("""{"id":42,"phase":1,"status":0,"outcome_known":false}""")
        val repo=HardwareRepository(BoardApi { path,body -> calls+=path to body
            when(path) {
                "/api/state" -> report
                "/api/control" -> JSONObject("""{"queued":true,"id":42}""")
                "/api/transaction" -> tx
                else -> error(path)
            }
        }) {time}
        repo.poll(); repo.configurePlan("light",1,60000)
        assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        assertTrue(calls.contains("/api/control" to "boot 9 7 0 1 60000"))
        report=sample(uptime=105,id=42).put("light_schedule_ms",59000).put("light_schedule_action",1).put("light_schedule_result",1)
        repo.poll();assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        tx.put("phase",2).put("outcome_known",true);repo.poll()
        assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
        assertNull(repo.state.value.manualVerifiedBoot)
        repo.reset();repo.poll();assertEquals(1,calls.count{it.first=="/api/control"})
    }
    @Test fun legacyFirmwareCannotReceiveConfiguration()=runBlocking {
        val calls=mutableListOf<String>();val repo=HardwareRepository(BoardApi {p,_ -> calls+=p;sample(0) }){1000L}
        repo.poll();repo.configurePlan("light",1,5000)
        assertEquals(listOf("/api/state"),calls)
        val boundCalls=mutableListOf<String>()
        val rebooted=HardwareRepository(BoardApi {p,_ -> boundCalls+=p;sample(boot=10) }){1000L}
        rebooted.poll();rebooted.configurePlan("light",1,5000,expectedBoot=9)
        assertEquals(listOf("/api/state"),boundCalls)
    }
    @Test fun wrongCapReadbackNeverReportsSuccess()=runBlocking {
        var report=sample();val repo=HardwareRepository(BoardApi {p,_ -> when(p) {
            "/api/state" -> report
            "/api/control" -> JSONObject("""{"queued":true,"id":42}""")
            else -> JSONObject("""{"id":42,"phase":2,"status":0,"outcome_known":true}""")
        } }){1000L}
        repo.poll();repo.configurePlan("fan",0,60000,true)
        report=sample(uptime=105,id=42);repo.poll()
        assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        report=sample(uptime=106,id=42).put("fan_energy_cap_ms",60000);repo.poll()
        assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
    }
    @Test fun invalidExtensionMarksTelemetryUnavailable()=runBlocking {
        val repo=HardwareRepository(BoardApi {_,_ -> sample().put("light_schedule_ms",86400001) }){1000L}
        repo.poll();assertFalse(repo.state.value.connected)
    }

    @Test fun fullCountdownStartsAtAuthorizationAndMinutesStayDistinct() {
        assertEquals(60000L,PlanningMath.minutesToMillis(1))
        assertEquals(14400000L,PlanningMath.minutesToMillis(240))
        for(m in listOf(0L,-1L,241L,Long.MAX_VALUE)) assertTrue(runCatching {PlanningMath.minutesToMillis(m)}.isFailure)
        // Review delay or wall-clock adjustment must not eat the new countdown.
        assertEquals(60000L,PlanningMath.authorizationValue(PlanningRule.OFF_COUNTDOWN,60000,5000,99999999))
        assertEquals(60000L,PlanningMath.authorizationValue(PlanningRule.CONTINUOUS_CAP,60000,5000,99999999))
        assertEquals(20000L,PlanningMath.authorizationValue(PlanningRule.SCHEDULE,60000,100000,80000))
        assertTrue(runCatching {PlanningMath.authorizationValue(PlanningRule.SCHEDULE,60000,100000,100001)}.isFailure)
        assertEquals("1:00",PlanningMath.duration(60000))
        assertEquals("0:01",PlanningMath.duration(1))
    }
    @Test fun countdownRequiresAckAndMatchingTimerWithoutChangingContinuousCap()=runBlocking {
        var report=sample().put("light_energy_cap_ms",120000);var time=1000L
        val calls=mutableListOf<Pair<String,String?>>()
        val tx=JSONObject("""{"id":42,"phase":1,"status":0,"outcome_known":false}""")
        val repo=HardwareRepository(BoardApi {path,body -> calls+=path to body
            when(path) {
                "/api/state" -> report
                "/api/control" -> JSONObject("""{"queued":true,"id":42}""")
                else -> tx
            }
        }){time}
        repo.poll();time+=20000 // authorizing after time spent reviewing
        report=sample(uptime=20100).put("light_energy_cap_ms",120000)
        repo.poll();repo.configureCountdown("light",60000,9)
        assertTrue(calls.contains("/api/control" to "boot 9 3 0 0 60000"))
        report=sample(uptime=21100,id=42).put("light_timer_ms",59000).put("light_energy_cap_ms",120000)
        repo.poll();assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        tx.put("phase",2).put("outcome_known",true);repo.poll()
        assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
        assertEquals(120000L,repo.state.value.sample!!.lightPlan.capMs)
        assertEquals(59000L,repo.state.value.sample!!.countdownAt("light",time))
        assertNull(repo.state.value.manualVerifiedBoot)
        repo.reset();repo.poll();assertEquals(1,calls.count{it.first=="/api/control"})
    }
    @Test fun incorrectCountdownReadbackAndWrongBootCannotSucceed()=runBlocking {
        var report=sample();val posts=mutableListOf<String>()
        val repo=HardwareRepository(BoardApi {p,b -> when(p) {
            "/api/state" -> report
            "/api/control" -> {posts+=b!!;JSONObject("""{"queued":true,"id":42}""")}
            else -> JSONObject("""{"id":42,"phase":2,"status":0,"outcome_known":true}""")
        } }){1000L}
        repo.poll();repo.configureCountdown("fan",60000,10);assertTrue(posts.isEmpty())
        for(value in listOf(-1L,59999L,14400001L)) assertTrue(runCatching {repo.configureCountdown("fan",value,9)}.isFailure)
        repo.configureCountdown("fan",60000,9)
        report=sample(uptime=1100,id=42).put("fan_timer_ms",1000)
        repo.poll();assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        report=sample(uptime=1200,id=42).put("fan_timer_ms",59000)
        repo.poll();assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
        assertEquals(1,posts.size)
    }
    @Test fun frozenReportCannotAuthorizeCountdownAfterReviewDelay()=runBlocking {
        var now=1000L;val paths=mutableListOf<String>()
        val frozen=sample()
        val repo=HardwareRepository(BoardApi {p,_ -> paths+=p;frozen }){now}
        repo.poll();now+=20000;repo.poll();repo.configureCountdown("light",60000,9)
        assertFalse(repo.state.value.connected)
        assertFalse(paths.contains("/api/control"))
    }
    @Test fun cancellingCountdownNeverRequiresOrReportsAnOnAction()=runBlocking {
        var report=sample().put("fan_timer_ms",50000).put("fan_energy_cap_ms",60000)
        val bodies=mutableListOf<String>()
        val repo=HardwareRepository(BoardApi {p,b -> when(p) {
            "/api/state" -> report
            "/api/control" -> {bodies+=b!!;JSONObject("""{"queued":true,"id":42}""")}
            else -> JSONObject("""{"id":42,"phase":2,"status":0,"outcome_known":true,"fan":false}""")
        } }){1000L}
        repo.poll();repo.configureCountdown("fan",0,9)
        report=sample(uptime=105,id=42).put("fan_timer_ms",0).put("fan_energy_cap_ms",60000)
        repo.poll();assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
        assertEquals(listOf("boot 9 3 1 0 0"),bodies)
        assertFalse(repo.state.value.sample!!.fan)
        assertEquals(60000L,repo.state.value.sample!!.fanPlan.capMs)
    }
    @Test fun countdownRoomAndAckAreNodeBound()=runBlocking {
        var report=sample().put("room","living_room").put("node_id",3)
        val calls=mutableListOf<Pair<String,String?>>()
        val repo=HardwareRepository(BoardApi {p,b -> calls+=p to b;when(p.substringBefore('?')) {
            "/api/state" -> report
            "/api/control" -> JSONObject("""{"node_id":3,"queued":true,"id":42}""")
            else -> JSONObject("""{"node_id":1,"id":42,"phase":2,"status":0,"outcome_known":true}""")
        } },{1000L},"living_room")
        repo.poll();repo.configureCountdown("fan",120000,9)
        report=sample(uptime=1100,id=42).put("room","living_room").put("node_id",3).put("fan_timer_ms",119000)
        repo.poll();assertFalse(repo.state.value.connected)
        assertEquals(RequestPhase.OUTCOME_UNKNOWN,repo.state.value.request!!.phase)
        assertTrue(calls.all{it.first.endsWith("?room=2")})
        assertEquals("boot 9 3 1 0 120000",calls.single{it.first.startsWith("/api/control")}.second)
        repo.reset();repo.poll();assertEquals(1,calls.count{it.first.startsWith("/api/control")})
    }
    @Test fun continuousObservationDoesNotGuessAnUnknownRunStart() {
        val run=ContinuousRunObservation()
        assertNull(run.observe(9,true,1000,false))
        assertEquals(0L,run.observe(9,false,2000,true))
        assertEquals(150L,run.observe(9,true,2150,true))
        assertEquals(300L,run.observe(9,true,2300,true))
        assertNull(run.observe(9,true,4000,false)) // missed OFF/ON could be hidden in a gap
        assertNull(run.observe(10,true,0,true)) // reboot cannot inherit a run
        run.observe(10,false,100,true)
        assertEquals(100L,run.observe(10,true,200,true))
        assertNull(run.observe(10,true,1,true)) // counter rollback/wrap is not a huge run
        run.clear();assertNull(run.observe(10,true,300,true))
    }
    @Test fun observedRunAndCountdownBecomeUnavailableWhenStale()=runBlocking {
        var now=1000L;var report=sample().put("light_on_ms",100)
        val repo=HardwareRepository(BoardApi {_,_ -> report }){now}
        repo.poll();now+=500
        report=sample(uptime=200).put("light",true).put("light_on_ms",250).put("light_timer_ms",50000)
        repo.poll();assertEquals(150L,repo.state.value.sample!!.observedRunAt("light",now))
        assertEquals(50000L,repo.state.value.sample!!.countdownAt("light",now))
        now+=3500;repo.age();assertNull(repo.state.value.sample!!.observedRunAt("light",now))
        assertNull(repo.state.value.sample!!.countdownAt("light",now))
        report=sample(uptime=500).put("light",true).put("light_on_ms",1000);repo.poll()
        assertNull(repo.state.value.sample!!.observedRunAt("light",now))
    }
}
