package com.kail.location.network

import android.content.Context
import com.kail.location.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Public, read-only JSON content. An empty base URL uses bundled files without network access. */
object OpenContentClient {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class NoticeInfo(
        val id: Long,
        val title: String,
        val type: Int,
        val content: String,
        val createTime: String
    )

    data class FaqItem(
        val id: Long,
        val title: String,
        val answerMd: String,
        val sort: Int
    )

    suspend fun getNoticeList(context: Context): Result<List<NoticeInfo>> = withContext(Dispatchers.IO) {
        repository(context).load("notices.json", ContentParser::notices)
    }

    suspend fun getFaqList(context: Context): Result<List<FaqItem>> = withContext(Dispatchers.IO) {
        repository(context).load("faqs.json", ContentParser::faqs)
    }

    private fun repository(context: Context): ContentRepository {
        val baseUrl = BuildConfig.OPEN_CONTENT_BASE_URL.trimEnd('/')
        // Separate caches when changing providers, including switching back to offline-only mode.
        val cacheDir = File(context.cacheDir, "open-content/${baseUrl.hashCode()}")
        return ContentRepository(
            fetchRemote = if (baseUrl.isBlank()) null else { name ->
                val request = Request.Builder().url("$baseUrl/$name")
                    .header("Accept", "application/json").build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    response.body?.string() ?: throw IOException("Empty content response")
                }
            },
            readCache = { name -> File(cacheDir, name).takeIf { it.isFile }?.readText() },
            writeCache = { name, json ->
                cacheDir.mkdirs()
                File(cacheDir, name).writeText(json)
            },
            readBundled = { name ->
                context.assets.open("open-api/$name").bufferedReader().use { it.readText() }
            }
        )
    }
}
