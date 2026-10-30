package com.example

import com.example.data.remote.PhoneVoiceDiagnostic
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.CRC32

class PhoneVoiceDiagnosticTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun retainedBytesSurviveCallerErasureAndMatchManifest() {
        val directory = File(temporary.root, "voice")
        val pcm = ByteArray(3200) { ((it * 31) and 255).toByte() }
        val expected = pcm.copyOf()
        val manifest = PhoneVoiceDiagnostic.save(directory, pcm, "first buffer after 250ms", true)!!
        pcm.fill(0)
        assertArrayEquals(expected, File(directory, "last.pcm").readBytes())
        val meta = JSONObject(manifest.readText())
        assertEquals(1600, meta.getInt("samples"))
        assertEquals(CRC32().apply { update(expected) }.value, meta.getLong("pcm_crc32"))
        assertEquals(64, meta.getString("pcm_sha256").length)
        assertEquals("PCM_S16LE", meta.getString("format"))
    }

    @Test fun replacesOneBoundedClipInsteadOfAccumulatingAudio() {
        val directory = File(temporary.root, "voice")
        PhoneVoiceDiagnostic.save(directory, ByteArray(128000) { 1 }, "cap", true)
        PhoneVoiceDiagnostic.save(directory, ByteArray(3200) { 2 }, "short", true)
        assertEquals(setOf("last.pcm", "last.json"), directory.list()!!.toSet())
        assertEquals(3200L, File(directory, "last.pcm").length())
        assertEquals(1600, JSONObject(File(directory, "last.json").readText()).getInt("samples"))
    }

    @Test fun disabledRetentionCreatesNoDirectory() {
        val directory = File(temporary.root, "voice")
        assertNull(PhoneVoiceDiagnostic.save(directory, ByteArray(3200), "", false))
        assertFalse(directory.exists())
    }

    @Test fun invalidAudioDoesNotReplacePreviousEvidence() {
        val directory = File(temporary.root, "voice")
        val expected = ByteArray(3200) { 9 }
        PhoneVoiceDiagnostic.save(directory, expected, "valid", true)
        assertTrue(runCatching { PhoneVoiceDiagnostic.save(directory, ByteArray(128002), "invalid", true) }.isFailure)
        assertArrayEquals(expected, File(directory, "last.pcm").readBytes())
    }
}
