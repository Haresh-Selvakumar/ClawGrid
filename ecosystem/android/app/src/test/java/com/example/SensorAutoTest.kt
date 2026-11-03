package com.example

import com.example.domain.SensorAutoSettings
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SensorAutoTest {
    private fun report(room: String="bedroom", node: Int=1, config: SensorAutoSettings=SensorAutoSettings()) = JSONObject()
        .put("room",room).put("node_id",node).put("known",true).put("authenticated",true)
        .put("boot_id",7).put("sample_uptime_ms",100).put("age_ms",0).put("last_command",0)
        .put("light",false).put("fan",false).put("physical_outputs_enabled",false)
        .put("auto_flags",0).put("manual_flags",3).put("mode","HOME").put("features_version",2)
        .apply { for(d in listOf("light","fan")) {
            put("${d}_schedule_ms",0);put("${d}_schedule_action",0);put("${d}_schedule_result",0)
            put("${d}_control_source",1);put("${d}_energy_cap_ms",0);put("${d}_timer_ms",0)
            put("${d}_sensor_config",config.packed());put("${d}_override_ms",0)
            put("${d}_sensor_demand",0);put("${d}_winner",1);put("${d}_blocking",1);put("${d}_override_action",0)
        } }
    private fun ack(node: Int=1,status: Int=0)=JSONObject().put("node_id",node).put("id",1)
        .put("phase",2).put("status",status).put("outcome_known",true).put("light",false).put("fan",false)
    @Test fun exactEditableTemperaturesAndFiniteBounds() {
        val c=SensorAutoSettings.parse("29.5","27.0","30",true)
        assertEquals(295,c.onDeciC);assertEquals(c,SensorAutoSettings.unpack(c.packed()))
        for(values in listOf(Triple("28","28","60"),Triple("26","28","60"),Triple("NaN","26","60"),
            Triple("28.01","26","60"),Triple("51","26","60"),Triple("28","-1","60"),
            Triple("28","26","0"),Triple("28","26","3601"))) {
            assertTrue(runCatching { SensorAutoSettings.parse(values.first,values.second,values.third,false) }.isFailure)
        }
    }
    @Test fun acceptedHttpAndAckAloneDoNotCompleteSettings()=runBlocking {
        val wanted=SensorAutoSettings(300,270,90,true);var state=report();val posts=mutableListOf<String>()
        val repo=HardwareRepository(BoardApi { path,body -> when(path) {
            "/api/state"->state
            "/api/control"->{posts+=body!!;JSONObject().put("queued",true).put("id",1)}
            else->ack()
        } },{1000L})
        repo.poll();repo.configureSensorAuto("fan",wanted,7)
        assertEquals("boot 7 9 1 0 ${wanted.packed()}",posts.single())
        assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        state.put("sample_uptime_ms",200).put("last_command",1);repo.poll()
        assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase) // ACK but old accepted config.
        state=report(config=wanted).put("sample_uptime_ms",300).put("last_command",1);repo.poll()
        assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
        assertEquals(wanted,repo.state.value.sample!!.fanAuto!!.settings)
        assertEquals(0,repo.state.value.sample!!.autoFlags) // Priority edits cannot enable AUTO.
    }
    @Test fun configIsRoomNodeAndBootBound()=runBlocking {
        var state=report("living_room",3);var wrongAck=true;val paths=mutableListOf<String>()
        val repo=HardwareRepository(BoardApi { path,_ ->paths+=path;when(path.substringBefore('?')) {
            "/api/state"->state
            "/api/control"->JSONObject().put("node_id",3).put("queued",true).put("id",1)
            else->ack(if(wrongAck) 1 else 3)
        } },{1000L},"living_room")
        repo.poll();repo.configureSensorAuto("fan",SensorAutoSettings(),6)
        assertFalse(paths.any{it.startsWith("/api/control")})
        repo.configureSensorAuto("fan",SensorAutoSettings(),7)
        state.put("sample_uptime_ms",200).put("last_command",1);repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN,repo.state.value.request!!.phase)
        assertTrue(paths.all{it.endsWith("?room=2")})
        wrongAck=false
    }
    @Test fun rejectedSettingsNeverAppearApplied()=runBlocking {
        var state=report()
        val repo=HardwareRepository(BoardApi { path,_ ->when(path) {
            "/api/state"->state
            "/api/control"->JSONObject().put("queued",true).put("id",1)
            else->ack(status=2)
        } },{1000L})
        repo.poll();repo.configureSensorAuto("fan",SensorAutoSettings(290,250),7)
        state.put("sample_uptime_ms",200).put("last_command",1);repo.poll()
        assertEquals(RequestPhase.REJECTED,repo.state.value.request!!.phase)
        assertEquals(280,repo.state.value.sample!!.fanAuto!!.settings.onDeciC)
    }
    @Test fun legacyAndMissingSensorFailClosed()=runBlocking {
        var posts=0;val old=report().put("features_version",1)
        val repo=HardwareRepository(BoardApi { _,body ->if(body!=null)posts++;old },{1000L})
        repo.poll();repo.configureSensorAuto("fan",SensorAutoSettings(),7);assertEquals(0,posts)
        val room2=HardwareRepository(BoardApi { _,_->report("living_room",3) },{1000L},"living_room")
        room2.poll()
        assertTrue(runCatching { room2.configureSensorAuto("light",SensorAutoSettings(sensorsFirst=true),7) }.isFailure)
    }
    @Test fun autoModeNeedsAuthoritativeModeReadbackAndRebootInvalidatesIt()=runBlocking {
        var state=report()
        val repo=HardwareRepository(BoardApi { path,_ ->when(path) {
            "/api/state"->state
            "/api/control"->JSONObject().put("queued",true).put("id",1)
            else->ack()
        } },{1000L})
        repo.poll();repo.configureDeviceMode("fan",true,7)
        state.put("sample_uptime_ms",200).put("last_command",1);repo.poll()
        assertEquals(RequestPhase.PENDING,repo.state.value.request!!.phase)
        state.put("sample_uptime_ms",300).put("auto_flags",2).put("manual_flags",1);repo.poll()
        assertEquals(RequestPhase.REPORTED,repo.state.value.request!!.phase)
        repo.configureDeviceMode("fan",false,7)
        state=report().put("boot_id",8);repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN,repo.state.value.request!!.phase)
    }
}
