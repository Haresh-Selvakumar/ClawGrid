package com.example.data.remote

object GatewayAddress {
    const val DEFAULT = "http://192.168.4.1"
    fun validate(value: String): String {
        val uri = java.net.URI(value.trim().trimEnd('/'))
        require(uri.scheme in listOf("http", "https") && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.rawPath.isNullOrEmpty()) { "Enter a local gateway origin, for example http://192.168.4.1" }
        val host = uri.host ?: error("Local gateway IPv4 address required")
        val parts = host.split('.').map { it.toIntOrNull() ?: -1 }
        require(parts.size == 4 && parts.all { it in 0..255 } && (parts[0] == 10 || (parts[0] == 172 && parts[1] in 16..31) || (parts[0] == 192 && parts[1] == 168)) && host != "10.0.2.2") { "Use the gateway's local Wi-Fi IPv4 address; localhost and emulator aliases do not reach the ESP from a phone" }
        require(uri.port == -1 || uri.port in 1..65535)
        return uri.toString()
    }
}
