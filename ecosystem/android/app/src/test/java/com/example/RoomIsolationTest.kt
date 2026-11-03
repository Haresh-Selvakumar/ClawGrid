package com.example
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RoomIsolationTest {
    private fun report(room: String, node: Int, boot: Long=4, uptime: Long=100) = JSONObject()
        .put("room",room).put("node_id",node).put("known",true).put("authenticated",true)
        .put("boot_id",boot).put("sample_uptime_ms",uptime).put("age_ms",0)
        .put("light",false).put("fan",false).put("last_command",0)
        .put("physical_outputs_enabled",false).put("light_input_available",false).put("light_input_raw",JSONObject.NULL)
    @Test fun independentRoomsAndStaleOrRebootedNode() = runBlocking {
        var now=1000L;var a=report("bedroom",1);var c=report("living_room",3)
        val calls=mutableListOf<String>()
        val api=BoardApi { path,_ -> calls+=path; if(path.endsWith("room=2")) c else a }
        val one=HardwareRepository(api,{now},"bedroom");val two=HardwareRepository(api,{now},"living_room")
        one.poll();two.poll();assertTrue(one.state.value.connected && two.state.value.connected)
        now+=3001;c=report("living_room",3,uptime=200);two.poll();one.age()
        assertFalse(one.state.value.connected);assertTrue(two.state.value.connected)
        c=report("living_room",3,boot=5,uptime=1);two.poll()
        assertEquals(4L,one.state.value.sample!!.boot);assertEquals(5L,two.state.value.sample!!.boot)
        assertTrue(calls.all { it=="/api/state" || it=="/api/state?room=2" })
    }
    @Test fun wrongNodeAndWrongRoomReportsCannotConnect() = runBlocking {
        var data=report("living_room",1)
        val two=HardwareRepository(BoardApi { _,_ -> data },{1000L},"living_room")
        two.poll();assertFalse(two.state.value.connected)
        data=report("bedroom",3);two.poll();assertFalse(two.state.value.connected)
        data=report("living_room",3).put("light_input_available",true).put("light_input_raw",true)
        two.poll();assertFalse(two.state.value.connected)
    }
    @Test fun manualProposalAndConfirmationRemainRoomBound() = runBlocking {
        val bodies=mutableListOf<String>();var wrongAck=false
        val two=HardwareRepository(BoardApi { path,body ->
            assertTrue(path.endsWith("?room=2"));if(body!=null)bodies+=body
            when(path.substringBefore('?')) {
                "/api/state" -> report("living_room",3)
                "/api/proposal" -> JSONObject().put("node_id",3).put("proposal_id",7).put("target","physical_bedroom")
                    .put("room","living_room").put("device","light").put("action","ON").put("expires_in_ms",15000).put("authorized",false)
                "/api/confirm" -> JSONObject().put("node_id",3).put("queued",true).put("id",1)
                else -> JSONObject().put("node_id",if(wrongAck) 1 else 3).put("id",1).put("phase",2).put("status",0).put("outcome_known",true).put("light",true)
            }
        },{1000L},"living_room")
        two.poll();two.prepare(true);two.confirm();assertEquals(RequestPhase.PENDING,two.state.value.request!!.phase)
        assertTrue(bodies.contains("manual physical_bedroom living_room light ON 0"))
        assertTrue(bodies.contains("7 physical_bedroom living_room light ON"))
        wrongAck=true;two.poll();assertFalse(two.state.value.connected)
        assertEquals(RequestPhase.OUTCOME_UNKNOWN,two.state.value.request!!.phase)
    }
    @Test fun explicitRoomVoiceAndConflictsKeepFixedGate() {
        fun result(text:String,confidence:Double=.9)=JSONObject().put("text",text).put("result",JSONArray(text.split(' ').map {
            JSONObject().put("word",it).put("conf",confidence)
        })).toString()
        val decision=PhoneVoicePolicy.decide(listOf(result("fan off")),"living_room")
        assertEquals("living_room",decision.command!!.room)
        assertEquals(PhoneVoiceReason.UNSUPPORTED_WORDS,PhoneVoicePolicy.decide(listOf(result("turn on bedroom light")),"living_room").reason)
        assertEquals(PhoneVoiceReason.LOW_CONFIDENCE,PhoneVoicePolicy.decide(listOf(result("light on",.699)),"living_room").reason)
        assertNull(PhoneVoicePolicy.decide(listOf(result("light and fan on")),"living_room").command)
        assertEquals(.7,PhoneVoicePolicy.MIN_WORD_CONFIDENCE,0.0)
    }
}
