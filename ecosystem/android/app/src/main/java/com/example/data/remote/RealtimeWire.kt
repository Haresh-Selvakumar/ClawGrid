package com.example.data.remote

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Same authenticated wire format as Python AESGCM and browser WebCrypto. */
class RealtimeWire(token: String, private val home: String, private val gateway: String) {
    private val key = SecretKeySpec(MessageDigest.getInstance("SHA-256")
        .digest(("ClawGridRealtimeV1:" + token).toByteArray(Charsets.UTF_8)), "AES")
    private val aad = "ClawGridRealtimeV1|$home|$gateway".toByteArray(Charsets.UTF_8)

    fun encrypt(value: JSONObject): JSONObject {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(aad)
        val data = value.toString().toByteArray(Charsets.UTF_8)
        require(data.size <= 32768)
        return JSONObject().put("v", 1).put("home_id", home).put("gateway_id", gateway)
            .put("iv", iv.toByteString().base64())
            .put("ct", cipher.doFinal(data).toByteString().base64())
    }

    fun decrypt(value: JSONObject): JSONObject {
        require(value.optInt("v") == 1 && value.optString("home_id") == home && value.optString("gateway_id") == gateway)
        val iv = requireNotNull(value.getString("iv").decodeBase64()).toByteArray()
        val data = requireNotNull(value.getString("ct").decodeBase64()).toByteArray()
        require(iv.size == 12 && data.size in 16..32784)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(aad)
        return JSONObject(String(cipher.doFinal(data), Charsets.UTF_8))
    }
}
