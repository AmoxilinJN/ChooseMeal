package com.choosemeal.app.data.update

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateCheckResult(
    val success: Boolean,
    val message: String,
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val changelog: String = "",
    val downloadUrl: String = "",
)

data class ApkDownloadResult(
    val success: Boolean,
    val message: String,
    val uri: Uri? = null,
    val version: String = "",
)

interface AppUpdateService {
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult

    // Progress: 0..100, -1 means total size unknown (indeterminate).
    suspend fun downloadApk(
        downloadUrl: String,
        latestVersion: String,
        onProgress: (Int) -> Unit,
    ): ApkDownloadResult
}

@Serializable
private data class GithubReleaseResponse(
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<GithubReleaseAsset> = emptyList(),
)

@Serializable
private data class GithubReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
)

class GithubAppUpdateService(
    private val context: Context,
) : AppUpdateService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val raw = httpGet(LATEST_RELEASE_API)
            val release = json.decodeFromString<GithubReleaseResponse>(raw)
            val latestVersion = release.tagName.ifBlank { release.name }.ifBlank { "unknown" }
            val apkAsset = selectApkAsset(release.assets)
                ?: return@runCatching UpdateCheckResult(
                    success = false,
                    message = "未找到可下载的 APK 资源，请先在 Release 中上传安装包。",
                )

            val hasUpdate = isVersionNewer(latestVersion, currentVersion)
            UpdateCheckResult(
                success = true,
                message = if (hasUpdate) "发现新版本 $latestVersion" else "当前已是最新版本",
                hasUpdate = hasUpdate,
                latestVersion = latestVersion,
                changelog = release.body.trim(),
                downloadUrl = apkAsset.browserDownloadUrl,
            )
        }.getOrElse {
            UpdateCheckResult(
                success = false,
                message = "检查更新失败: ${it.message ?: "网络异常"}",
            )
        }
    }

    override suspend fun downloadApk(
        downloadUrl: String,
        latestVersion: String,
        onProgress: (Int) -> Unit,
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "choosemeal-${sanitizeFilePart(latestVersion)}.apk"
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, fileName)
            downloadFile(downloadUrl, apkFile, onProgress)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            ApkDownloadResult(
                success = true,
                message = "下载完成",
                uri = uri,
                version = latestVersion,
            )
        }.getOrElse {
            ApkDownloadResult(
                success = false,
                message = "下载更新失败: ${it.message ?: "网络异常"}",
            )
        }
    }

    private fun selectApkAsset(assets: List<GithubReleaseAsset>): GithubReleaseAsset? {
        return assets
            .filter { it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank() }
            .sortedByDescending { asset ->
                var score = 0
                val name = asset.name.lowercase()
                if ("release" in name) score += 3
                if ("app" in name) score += 1
                if ("debug" in name) score -= 2
                score
            }
            .firstOrNull()
    }

    private fun sanitizeFilePart(value: String): String {
        return value.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "latest" }
    }

    private fun downloadFile(
        urlString: String,
        target: File,
        onProgress: (Int) -> Unit,
    ) {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.instanceFollowRedirects = true
        connection.connect()

        val code = connection.responseCode
        if (code !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }?.take(180)
            connection.disconnect()
            error("HTTP $code ${errorBody ?: ""}".trim())
        }

        val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
        var downloadedBytes = 0L
        var lastProgress = -1
        if (totalBytes <= 0) {
            onProgress(-1)
        } else {
            onProgress(0)
        }

        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read

                    if (totalBytes > 0) {
                        val progress = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                }
            }
        }

        onProgress(100)
        connection.disconnect()
    }

    private fun httpGet(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "ChooseMeal-Android")
        connection.instanceFollowRedirects = true
        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }?.take(180)
            connection.disconnect()
            error("HTTP $code ${errorBody ?: ""}".trim())
        }
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.also {
            connection.disconnect()
        }
    }

    private fun isVersionNewer(latestVersion: String, currentVersion: String): Boolean {
        val latest = parseVersionNumbers(latestVersion)
        val current = parseVersionNumbers(currentVersion)
        if (latest == null || current == null) {
            return latestVersion.trim() != currentVersion.trim()
        }

        val size = maxOf(latest.size, current.size)
        for (index in 0 until size) {
            val l = latest.getOrElse(index) { 0 }
            val c = current.getOrElse(index) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun parseVersionNumbers(version: String): List<Int>? {
        val normalized = version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
        if (normalized.isBlank()) return null
        val parts = normalized.split('.')
        if (parts.isEmpty()) return null
        val numbers = parts.map { part ->
            val digitPrefix = part.takeWhile { it.isDigit() }
            if (digitPrefix.isBlank()) return null
            digitPrefix.toIntOrNull() ?: return null
        }
        return numbers
    }

    companion object {
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/AmoxilinJN/ChooseMeal/releases/latest"
    }
}
