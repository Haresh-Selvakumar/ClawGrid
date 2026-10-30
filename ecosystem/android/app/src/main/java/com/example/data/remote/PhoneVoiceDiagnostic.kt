package com.example.data.remote

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32

/** One bounded clip in app-private cache, only when the caller is a debug build. No network export. */
object PhoneVoiceDiagnostic {
    fun save(directory: File, pcm: ByteArray, captureInfo: String, enabled: Boolean): File? {
        if (!enabled) return null
        GatewayPcm.validate(pcm)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create private voice diagnostic directory" }
        val crc = CRC32().apply { update(pcm) }.value
        val sha = MessageDigest.getInstance("SHA-256").digest(pcm).joinToString("") { "%02x".format(it.toInt() and 255) }
        val metadata = JSONObject().put("source", "PHONE_CAPTURE_DIAGNOSTIC")
            .put("sample_rate_hz", 16000).put("channels", 1).put("format", "PCM_S16LE")
            .put("samples", pcm.size / 2).put("bytes", pcm.size).put("pcm_crc32", crc)
            .put("pcm_sha256", sha).put("capture_info", captureInfo)
        File(directory, "last.pcm").writeBytes(pcm)
        return File(directory, "last.json").also { it.writeText(metadata.toString(2), Charsets.UTF_8) }
    }
}
