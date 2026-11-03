package com.example

import com.example.data.remote.*
import com.example.domain.SensorAutoSettings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SensorOrderTest {
    private fun state(order: Int=0,uptime: Long=100,command: Int=0)=JSONObject()
        .put("room","bedroom").put("node_id",1).put("known",true).put("authenticated",true)
        .put("boot_id",80).put("sample_uptime_ms",uptime).put("last_command",command).put("age_ms",0)
        .put("light",false).put("fan",false).put("physical_outputs_enabled",false)
        .put("features_version",3).put("auto_flags",0).put("manual_flags",3).put("mode","HOME")
        .put("telemetry_schema",4).put("temperature_status","OK").put("temperature_deci_c",275).put("temperature_age_ms",1000)
        .put("light_input_available",true).put("light_input_raw",true).put("light_sample_age_ms",0)
        .put("temperature_sample_age_ms",1000).put("sensor_order",order).put("sensor_snapshot_ms",uptime)
        .put("evaluation_cycle",20).put("evaluation_started_us",100).put("evaluation_clock_valid",true)
        .apply { for((d,step) in listOf("light" to if(order==0) 1 else 2,"fan" to if(order==0) 2 else 1)) {
            put("${d}_sensor_config",SensorAutoSettings().packed());put("${d}_override_ms",0)
            put("${d}_sensor_demand",2);put("${d}_winner",1);put("${d}_blocking",1);put("${d}_override_action",0)
            put("${d}_schedule_ms",0);put("${d}_schedule_action",0);put("${d}_schedule_result",0);put("${d}_control_source",1);put("${d}_energy_cap_ms",0)
            put("${d}_eval_step",step);put("${d}_eval_start_us",100+step*10);put("${d}_eval_elapsed_us",3)
            put("${d}_sensor_requested",1);put("${d}_sensor_result",2)
        } }
    private fun ack()=JSONObject().put("node_id",1).put("id",1).put("phase",2).put("status",0).put("outcome_known",true)
    @Test fun orderChangeRequiresAckAndExactFreshReadbackWithoutModeChange()=runBlocking {
        var data=state();val bodies=mutableListOf<String>()
        val repo=HardwareRepository(BoardApi { path,body->when(path) {
            "/api/state"->data
            "/api/control"->{bodies+=body!!;JSONObject().put("node_id",1).put("queued",true).put("id",1)}
            else->ack()
        } },{1000L})
        repo.poll();assertEquals(27.5,repo.state.value.sample!!.temperatureAt(1000)!!,0.0)
        assertEquals(true,repo.state.value.sample!!.lightInputRaw)
        repo.configureSensorOrder(true,80);assertEquals("boot 80 11 0 0 1",bodies.single())
        data=state(0,200,1);repo.poll();assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        data=state(1,300,1);repo.poll();assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
        val s=repo.state.value.sample!!;assertEquals(0,s.autoFlags);assertEquals(3,s.manualFlags)
        assertEquals(1,s.sensorEvaluation!!.fan.step);assertEquals(2,s.sensorEvaluation!!.light.step)
        assertEquals(27.5,s.temperatureAt(1000)!!,0.0);assertTrue(s.lightInputAvailable)
    }
    @Test fun contradictoryOrderingAndMissingEvidenceNeverAppearFresh()=runBlocking {
        var data=state(1).put("light_eval_step",1)
        val repo=HardwareRepository(BoardApi { _,_->data },{1000L});repo.poll();assertFalse(repo.state.value.connected)
        data=state();data.remove("light_sample_age_ms");repo.poll();assertFalse(repo.state.value.connected)
    }
    @Test fun staleAndInvalidTemperatureRemainUnavailableWhileLightMetadataSurvives()=runBlocking {
        var now=1000L;val data=state().put("temperature_status","INVALID").put("temperature_deci_c",JSONObject.NULL).put("temperature_age_ms",JSONObject.NULL)
        val repo=HardwareRepository(BoardApi { _,_->data },{now});repo.poll()
        assertTrue(repo.state.value.connected);assertNull(repo.state.value.sample!!.temperatureAt(now))
        assertTrue(repo.state.value.sample!!.lightInputAvailable);assertEquals(1000L,repo.state.value.sample!!.sensorEvaluation!!.temperatureAttemptAge)
        now+=3001;repo.age();assertFalse(repo.state.value.connected);assertNull(repo.state.value.sample!!.temperatureAt(now))
    }
    @Test fun oldFirmwareAndRoom2CannotAcknowledgeOrderConfiguration()=runBlocking {
        var posts=0;val old=state().put("features_version",2)
        val repo=HardwareRepository(BoardApi { _,body->if(body!=null)posts++;old },{1000L});repo.poll();repo.configureSensorOrder(true,80);assertEquals(0,posts)
        val two=HardwareRepository(BoardApi { _,_->state() },{1000L},"living_room")
        assertTrue(runCatching { two.configureSensorOrder(true,80) }.isFailure)
    }
    @Test fun changedBootInvalidatesPendingOrderRatherThanReplaying()=runBlocking {
        var data=state();var posts=0
        val repo=HardwareRepository(BoardApi { path,body->if(body!=null)posts++;when(path) {
            "/api/state"->data
            "/api/control"->JSONObject().put("queued",true).put("id",1)
            else->ack()
        } },{1000L})
        repo.poll();repo.configureSensorOrder(true,80)
        data=state(0,1).put("boot_id",81);repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN,repo.state.value.request!!.phase);assertEquals(1,posts)
    }
}
