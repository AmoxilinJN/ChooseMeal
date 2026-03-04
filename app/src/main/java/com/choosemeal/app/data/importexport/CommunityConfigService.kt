package com.choosemeal.app.data.importexport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class CommunityIndexV1(
    val version: Int = 1,
    val updatedAt: String = "",
    val configs: List<CommunityConfigEntry> = emptyList(),
)

@Serializable
data class CommunityConfigEntry(
    val id: String,
    val schoolName: String,
    val city: String,
    val region: String? = null,
    val file: String,
    val author: String,
    val updatedAt: String,
    val tags: List<String> = emptyList(),
)

data class CommunityIndexResult(
    val success: Boolean,
    val message: String,
    val updatedAt: String = "",
    val entries: List<CommunityConfigEntry> = emptyList(),
)

data class CommunityDownloadResult(
    val success: Boolean,
    val message: String,
    val rawJson: String = "",
)

interface CommunityConfigService {
    suspend fun fetchIndex(): CommunityIndexResult
    suspend fun downloadConfig(entry: CommunityConfigEntry): CommunityDownloadResult
    fun issueTemplateUrl(): String
    fun repositoryUrl(): String
}

class GithubCommunityConfigService : CommunityConfigService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchIndex(): CommunityIndexResult = withContext(Dispatchers.IO) {
        runCatching {
            val raw = httpGet(INDEX_URL)
            val index = json.decodeFromString<CommunityIndexV1>(raw)
            if (index.version != 1) {
                return@runCatching CommunityIndexResult(
                    success = false,
                    message = "社区配置索引版本不兼容（version=${index.version}）",
                )
            }
            val entries = index.configs.filter {
                it.id.isNotBlank() &&
                    it.schoolName.isNotBlank() &&
                    it.city.isNotBlank() &&
                    it.file.isNotBlank()
            }.sortedWith(compareBy<CommunityConfigEntry> { it.schoolName }.thenBy { it.city })

            CommunityIndexResult(
                success = true,
                message = "ok",
                updatedAt = index.updatedAt,
                entries = entries,
            )
        }.getOrElse {
            CommunityIndexResult(
                success = false,
                message = "加载社区配置失败: ${it.message ?: "网络异常"}",
            )
        }
    }

    override suspend fun downloadConfig(entry: CommunityConfigEntry): CommunityDownloadResult = withContext(Dispatchers.IO) {
        runCatching {
            val url = resolveConfigUrl(entry.file)
            val raw = httpGet(url)
            CommunityDownloadResult(
                success = true,
                message = "ok",
                rawJson = raw,
            )
        }.getOrElse {
            CommunityDownloadResult(
                success = false,
                message = "下载配置失败: ${it.message ?: "网络异常"}",
            )
        }
    }

    override fun issueTemplateUrl(): String = ISSUE_TEMPLATE_URL

    override fun repositoryUrl(): String = REPOSITORY_URL

    private fun resolveConfigUrl(file: String): String {
        return if (file.startsWith("http://") || file.startsWith("https://")) {
            file
        } else {
            "${RAW_BASE_URL}${file.trimStart('/')}"
        }
    }

    private fun httpGet(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/json")
        connection.instanceFollowRedirects = true
        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }?.take(180)
            connection.disconnect()
            error("HTTP $code ${errorBody ?: ""}".trim())
        }
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }.also {
            connection.disconnect()
        }
    }

    companion object {
        private const val INDEX_URL =
            "https://raw.githubusercontent.com/AmoxilinJN/ChooseMeal/main/community/index.json"
        private const val RAW_BASE_URL =
            "https://raw.githubusercontent.com/AmoxilinJN/ChooseMeal/main/"
        private const val REPOSITORY_URL = "https://github.com/AmoxilinJN/ChooseMeal"
        private const val ISSUE_TEMPLATE_URL =
            "https://github.com/AmoxilinJN/ChooseMeal/issues/new?template=config_share.yml"
    }
}
