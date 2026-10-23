package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import okhttp3.Dns
import org.json.JSONObject

/** Bind only these local calls to Wi-Fi, including an AP without Internet. Never use cellular as fallback. */
class WifiGatewayApis(context: Context, private val base: String, private val token: String) : BoardApi, SpeechApi {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private var selected: Network? = null
    private var board: HttpBoardApi? = null
    private var speech: HttpSpeechApi? = null
    private fun selectWifi() {
        GatewayAddress.validate(base)
        val network = connectivity.allNetworks.firstOrNull {
            connectivity.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } ?: error("Join the ESP gateway Wi-Fi and choose Stay connected without Internet")
        if (network != selected) {
            selected = network
            val dns = object : Dns {
                override fun lookup(hostname: String) = network.getAllByName(hostname).toList()
            }
            board = HttpBoardApi(base, token, network.socketFactory, dns)
            speech = HttpSpeechApi(base, token, network.socketFactory, dns)
        }
    }
    override suspend fun request(path: String, body: String?): JSONObject { selectWifi(); return board!!.request(path, body) }
    override suspend fun status(): JSONObject { selectWifi(); return speech!!.status() }
    override suspend fun upload(pcm: ByteArray): JSONObject { selectWifi(); return speech!!.upload(pcm) }
}
