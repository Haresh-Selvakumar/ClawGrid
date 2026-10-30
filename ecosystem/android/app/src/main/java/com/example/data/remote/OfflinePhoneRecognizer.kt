package com.example.data.remote

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import com.sun.jna.NativeLibrary
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/** Native Vosk inference only. No SpeechRecognizer, network, PCM upload or transcript persistence. */
class OfflinePhoneRecognizer(context: Context) {
    private val app = context.applicationContext
    private val mutex = Mutex()
    private var model: Model? = null
    private var closed = false
    private var vocabulary: Map<String, Int> = emptyMap()

    suspend fun prepare() = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(!closed)
            if (model == null) {
                LibVosk.vosk_set_log_level(-1)
                val path = installModel()
                check(File(path, "graph/HCLr.fst").isFile && File(path, "graph/Gr.fst").isFile) {
                    "Bundled model does not support the bounded dynamic phrase grammar"
                }
                currentCoroutineContext().ensureActive()
                val loaded = Model(path.absolutePath)
                try {
                    // Documented public C API, also exported by the pinned Android0.3.75 AAR.
                    val findWord = NativeLibrary.getInstance("vosk").getFunction("vosk_model_find_word")
                    vocabulary = PhoneVoicePolicy.requiredVocabulary.associateWith { word ->
                        findWord.invokeInt(arrayOf(loaded.pointer, word)).also { check(it > 0) { "Displayed command word absent from bundled model" } }
                    }
                    currentCoroutineContext().ensureActive(); model = loaded
                } catch (e: Throwable) { loaded.close(); throw e }
            }
            vocabulary.toMap()
        }
    }

    suspend fun recognize(pcm: ByteArray, selectedRoom: String?): PhoneVoiceDecision = withContext(Dispatchers.IO) {
        GatewayPcm.validate(pcm)
        mutex.withLock {
            check(!closed)
            val loaded = model ?: error("Local model unavailable")
            val deadline = SystemClock.elapsedRealtime() + 8000
            // Same acoustic model; bounded vocabulary with explicit unknown and
            // negative utterances. The exact whole-text parser remains mandatory.
            Recognizer(loaded, 16000f, PhoneVoicePolicy.decoderGrammar).use { decoder ->
                decoder.setWords(true)
                val results = mutableListOf<String>()
                val chunk = ByteArray(4000)
                try {
                    var offset = 0
                    while (offset < pcm.size) {
                        currentCoroutineContext().ensureActive()
                        check(SystemClock.elapsedRealtime() < deadline) { "Local recognition timed out" }
                        val count = minOf(chunk.size, pcm.size - offset)
                        pcm.copyInto(chunk, 0, offset, offset + count)
                        if (decoder.acceptWaveForm(chunk, count)) results += decoder.result
                        offset += count
                    }
                    results += decoder.finalResult
                    currentCoroutineContext().ensureActive()
                    check(SystemClock.elapsedRealtime() < deadline) { "Local recognition timed out" }
                    PhoneVoicePolicy.decide(results, selectedRoom)
                } finally { chunk.fill(0) }
            }
        }
    }

    fun close() {
        // Avoid closing native shared data while an in-flight call is using it, or blocking the UI.
        CoroutineScope(Dispatchers.IO).launch { mutex.withLock { closed = true; model?.close(); model = null } }
    }

    private suspend fun installModel(): File {
        val manifest = app.assets.open("offline-voice/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
        val archiveHash = manifest.getString("archive_sha256")
        require(archiveHash.matches(Regex("[a-f0-9]{64}")))
        val root = File(app.noBackupFilesDir, "offline-voice").apply { mkdirs() }
        val destination = File(root, archiveHash)
        val files = manifest.getJSONArray("files")
        val expected = (0 until files.length()).associate { i ->
            val entry = files.getJSONObject(i)
            entry.getString("path") to entry
        }
        require(expected.size in 1..100 && manifest.getLong("extracted_bytes") < 100_000_000)
        suspend fun valid(): Boolean {
            if (!destination.isDirectory) return false
            for ((path, metadata) in expected) {
                currentCoroutineContext().ensureActive()
                val file = safeFile(destination, path)
                if (!file.isFile || file.length() != metadata.getLong("bytes") || digest(file) != metadata.getString("sha256")) return false
            }
            return true
        }
        if (valid()) return destination
        val staging = File(root, "install-staging")
        val archive = File(root, "install.zip")
        // Both paths are fixed private app children; no shared storage or user-supplied paths.
        staging.deleteRecursively(); staging.mkdirs()
        try {
            app.assets.open("offline-voice/model.zip").use { input -> archive.outputStream().use { output ->
                val buffer = ByteArray(65536)
                var total = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer); if (n < 0) break
                    total += n; require(total <= manifest.getLong("archive_bytes"))
                    output.write(buffer, 0, n)
                }
                require(total == manifest.getLong("archive_bytes"))
            } }
            require(digest(archive) == archiveHash)
            val seen = mutableSetOf<String>()
            ZipFile(archive).use { zip ->
                for (entry in zip.entries()) {
                    currentCoroutineContext().ensureActive()
                    if (entry.isDirectory) continue
                    val prefix = manifest.getString("model") + "/"
                    require(entry.name.startsWith(prefix))
                    val path = entry.name.removePrefix(prefix)
                    val metadata = expected[path] ?: error("Unexpected model entry")
                    require(seen.add(path) && entry.size == metadata.getLong("bytes"))
                    val output = safeFile(staging, path)
                    output.parentFile!!.mkdirs()
                    zip.getInputStream(entry).use { input -> output.outputStream().use { stream ->
                        val buffer = ByteArray(65536); var total = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer); if (n < 0) break
                            total += n; require(total <= entry.size); stream.write(buffer, 0, n)
                        }
                        require(total == entry.size)
                    } }
                    require(digest(output) == metadata.getString("sha256"))
                }
            }
            require(seen == expected.keys)
            destination.deleteRecursively()
            check(staging.renameTo(destination)) { "Cannot install bundled model" }
            return destination
        } finally { archive.delete(); staging.deleteRecursively() }
    }

    private fun safeFile(root: File, relative: String): File {
        require(relative.isNotBlank() && !relative.contains('\\') && relative.split('/').none { it == ".." || it.isEmpty() })
        return File(root, relative).canonicalFile.also { require(it.path.startsWith(root.canonicalPath + File.separator)) }
    }
    private suspend fun digest(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
