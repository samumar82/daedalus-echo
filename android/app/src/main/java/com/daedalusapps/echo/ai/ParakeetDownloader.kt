package com.daedalusapps.echo.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Downloads and safely expands the official int8 Parakeet archive into private app storage. */
class ParakeetDownloader(private val context: Context) {
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.MINUTES).build()

    suspend fun download() = withContext(Dispatchers.IO) {
        val finalDir = parakeetModelDir(context)
        if (isParakeetReady(context)) { _state.value = DownloadState.Done; return@withContext }
        val stagingDir = File(finalDir.parentFile, "${finalDir.name}.installing")
        stagingDir.deleteRecursively(); stagingDir.mkdirs()
        val archive = File(stagingDir.parentFile, "${PARAKEET_MODEL_ID}.tar.bz2.part")
        try {
            val request = Request.Builder().url(PARAKEET_ARCHIVE_URL)
                .addHeader("User-Agent", "DaedalusEcho/ItalianOffline").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code} while downloading Parakeet")
                val body = response.body ?: error("Empty Parakeet download")
                val total = body.contentLength().takeIf { it > 0 } ?: PARAKEET_TOTAL_BYTES
                var downloaded = 0L
                FileOutputStream(archive).use { out -> body.byteStream().use { input ->
                    val buffer = ByteArray(65_536)
                    while (true) {
                        val read = input.read(buffer); if (read < 0) break
                        out.write(buffer, 0, read); downloaded += read
                        _state.value = DownloadState.Downloading(
                            ((downloaded * 100) / total).toInt().coerceIn(0, 100),
                            downloaded, total)
                    }
                } }
            }
            _state.value = DownloadState.Installing("Installing Parakeet model…")
            extractModelArchive(archive, stagingDir)
            val extracted = File(stagingDir, PARAKEET_MODEL_ID)
            val required = listOf(PARAKEET_ENCODER_FILE, PARAKEET_DECODER_FILE, PARAKEET_JOINER_FILE, PARAKEET_TOKENS_FILE)
            require(extracted.isDirectory && required.all { File(extracted, it).isFile }) { "Incomplete Parakeet archive" }
            finalDir.deleteRecursively()
            if (!extracted.renameTo(finalDir)) extracted.copyRecursively(finalDir, overwrite = true)
            _state.value = DownloadState.Done
        } catch (e: Exception) {
            Log.e("ParakeetDL", "Model download failed", e)
            _state.value = DownloadState.Failed(e.message ?: "Unknown error")
        } finally { archive.delete(); stagingDir.deleteRecursively() }
    }

    private fun extractModelArchive(archive: File, stagingDir: File) {
        val root = stagingDir.canonicalFile
        val modelPrefix = "${PARAKEET_MODEL_ID}/"
        val required = setOf(
            PARAKEET_ENCODER_FILE,
            PARAKEET_DECODER_FILE,
            PARAKEET_JOINER_FILE,
            PARAKEET_TOKENS_FILE
        )
        val extracted = File(stagingDir, PARAKEET_MODEL_ID)
        extracted.mkdirs()
        TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream())).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                val output = File(stagingDir, entry.name).canonicalFile
                require(output.path.startsWith(root.path + File.separator)) { "Unsafe archive path: ${entry.name}" }
                val filename = entry.name.removePrefix(modelPrefix)
                if (!entry.isDirectory && entry.name.startsWith(modelPrefix) && filename in required) {
                    output.parentFile?.mkdirs()
                    output.outputStream().use { out ->
                        val buffer = ByteArray(65_536)
                        var read: Int
                        while (tar.read(buffer).also { read = it } >= 0) out.write(buffer, 0, read)
                    }
                    // Only the runtime files are needed. Skipping archive extras prevents
                    // first-run setup from waiting on unrelated archive content.
                    if (required.all { File(extracted, it).isFile }) break
                }
            }
        }
        require(required.all { File(extracted, it).isFile }) { "Incomplete Parakeet archive" }
    }
}
