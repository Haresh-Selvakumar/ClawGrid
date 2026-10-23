package com.example

import com.example.data.remote.BoardRejected
import com.example.data.remote.HttpBoardApi
import com.example.data.remote.HttpSpeechApi
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

/** Real loopback HTTP fixtures, never a gateway or board. */
class HttpBoardApiTest {
    @Test fun speechAudioIsRawOctetStreamAndUploadedOnce() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val pcm = ByteArray(3200) { (it % 256).toByte() }
        var received = byteArrayOf(); var contentType = ""; var token = ""; var uploads = 0
        server.createContext("/api/speech/audio") { exchange ->
            uploads++; received = exchange.requestBody.readBytes()
            contentType = exchange.requestHeaders.getFirst("Content-Type")
            token = exchange.requestHeaders.getFirst("X-ClawGrid-Token")
            val body = """{"queued":true,"job":71,"authorized":false}""".toByteArray()
            exchange.sendResponseHeaders(202, body.size.toLong()); exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            assertEquals(71, HttpSpeechApi("http://127.0.0.1:${server.address.port}", "voice-fixture-token").upload(pcm).getInt("job"))
            assertArrayEquals(pcm, received); assertEquals("application/octet-stream", contentType)
            assertEquals("voice-fixture-token", token); assertEquals(1, uploads)
        } finally { server.stop(0) }
    }

    @Test fun rejectedAudioIsNotRetried() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var uploads = 0
        server.createContext("/api/speech/audio") { exchange -> uploads++; exchange.requestBody.readBytes(); exchange.sendResponseHeaders(409, -1); exchange.close() }
        server.start()
        try {
            assertTrue(runCatching { HttpSpeechApi("http://127.0.0.1:${server.address.port}", "voice-fixture-token").upload(ByteArray(3200)) }.exceptionOrNull() is BoardRejected)
            assertEquals(1, uploads)
        } finally { server.stop(0) }
    }

    @Test fun textAndTokenSentOnceAnd202IsOnlyQueueAcceptance() = runBlocking {
        val count = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var received = ""
        var token = ""
        server.createContext("/api/confirm") { exchange ->
            count.incrementAndGet()
            received = exchange.requestBody.bufferedReader().readText()
            token = exchange.requestHeaders.getFirst("X-ClawGrid-Token")
            val bytes = """{"queued":true,"id":42}""".toByteArray()
            exchange.sendResponseHeaders(202, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val api = HttpBoardApi("http://127.0.0.1:${server.address.port}", "host-fixture-token")
            val body = "7 physical_bedroom bedroom light ON"
            assertTrue(api.request("/api/confirm", body).getBoolean("queued"))
            assertEquals(body, received); assertEquals("host-fixture-token", token); assertEquals(1, count.get())
        } finally { server.stop(0) }
    }

    @Test fun rejectionAndRedirectAreNeverRetriedOrFollowed() = runBlocking {
        for (status in listOf(409, 302)) {
            val count = AtomicInteger()
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/api/confirm") { exchange ->
                count.incrementAndGet()
                exchange.responseHeaders.add("Location", "/other")
                exchange.sendResponseHeaders(status, -1); exchange.close()
            }
            server.createContext("/other") { exchange -> count.incrementAndGet(); exchange.close() }
            server.start()
            try {
                val api = HttpBoardApi("http://127.0.0.1:${server.address.port}", "host-fixture-token")
                val error = runCatching { api.request("/api/confirm", "7 physical_bedroom bedroom light ON") }.exceptionOrNull()
                assertTrue(error is BoardRejected); assertEquals(status, (error as BoardRejected).status)
                assertEquals(1, count.get())
            } finally { server.stop(0) }
        }
    }
}
