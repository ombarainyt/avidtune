package com.cgens67.avidtune.repository

import com.cgens67.avidtune.models.NewsItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor() {

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 15000
            endpoint {
                connectTimeout = 15000
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Volatile private var metadataCache: List<NewsItem>? = null

    suspend fun fetchNews(): List<NewsItem> {
        val response = client.get(METADATA_URL) {
            headers {
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
                append(HttpHeaders.Expires, "0")
            }
        }
        val text = response.bodyAsText()
        val items = json.decodeFromString<List<NewsItem>>(text)
        metadataCache = items
        return items
    }

    suspend fun fetchNewsContent(id: String): String {
        val response = client.get("$CONTENT_BASE_URL$id") {
            headers {
                append(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                append(HttpHeaders.Pragma, "no-cache")
                append(HttpHeaders.Expires, "0")
            }
        }
        return response.bodyAsText()
    }

    fun getCachedItem(id: String): NewsItem? = metadataCache?.find { it.id == id }

    private companion object {
        const val METADATA_URL =
            "https://raw.githubusercontent.com/cgens67/avidtune-news/main/metadata.json"
        const val CONTENT_BASE_URL =
            "https://raw.githubusercontent.com/cgens67/avidtune-news/main/content/"
    }
}