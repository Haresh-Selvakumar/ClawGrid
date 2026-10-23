package com.example

import com.example.data.remote.RealtimeWire
import com.example.data.remote.EcosystemClient
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class RealtimeWireTest {
    private fun fixture() = JSONObject(javaClass.classLoader!!.getResourceAsStream("realtime-vector.json")!!.bufferedReader().readText())
    @Test fun pythonEncryptedFixtureAndRoundTrip() {
        val f = fixture()
        val wire = RealtimeWire(f.getString("token"), f.getString("home_id"), f.getString("gateway_id"))
        val value = wire.decrypt(f.getJSONObject("envelope"))
        assertEquals(7, value.getInt("sequence"))
        assertTrue(value.isNull("sensor"))
        assertEquals("snapshot", wire.decrypt(wire.encrypt(value)).getString("kind"))
    }
    @Test fun wrongKeyAndScopeRejected() {
        val f = fixture()
        assertTrue(runCatching { RealtimeWire("wrong",f.getString("home_id"),f.getString("gateway_id")).decrypt(f.getJSONObject("envelope")) }.isFailure)
        assertTrue(runCatching { RealtimeWire(f.getString("token"),f.getString("gateway_id"),f.getString("home_id")).decrypt(f.getJSONObject("envelope")) }.isFailure)
    }
    @Test fun staleAndUnauthenticatedNeverFresh() {
        val a=JSONObject().put("known",true).put("authenticated",true).put("age_ms",2999)
        val s=JSONObject().put("node_a",a)
        assertTrue(EcosystemClient.fresh(s))
        a.put("age_ms",3000);assertFalse(EcosystemClient.fresh(s))
        a.put("age_ms",-1);assertFalse(EcosystemClient.fresh(s))
        a.put("age_ms",0).put("authenticated",false);assertFalse(EcosystemClient.fresh(s))
    }
}
