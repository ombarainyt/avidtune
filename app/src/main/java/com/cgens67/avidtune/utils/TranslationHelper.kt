package com.cgens67.avidtune.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber
import java.util.Locale

object TranslationHelper {
    private val client = HttpClient(CIO)

    suspend fun translate(text: String, targetLang: String = Locale.getDefault().toLanguageTag()): String? = runCatching {
        if (text.isBlank()) return null
        
        val lines = text.split("\n")
        val chunks = mutableListOf<String>()
        var currentChunk = java.lang.StringBuilder()
        
        for (line in lines) {
            // Keep chunks safely within the API's limit
            if (currentChunk.length + line.length > 1500) {
                chunks.add(currentChunk.toString())
                currentChunk = java.lang.StringBuilder()
            }
            currentChunk.append(line).append("\n")
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        val resultBuilder = java.lang.StringBuilder()
        for (chunk in chunks) {
            val response = client.get("https://translate.googleapis.com/translate_a/single") {
                parameter("client", "gtx")
                parameter("sl", "auto")
                parameter("tl", targetLang)
                parameter("dt", "t")
                parameter("q", chunk)
            }.bodyAsText()

            val jsonArray = Json.parseToJsonElement(response).jsonArray
            val translatedSegments = jsonArray[0].jsonArray
            for (segment in translatedSegments) {
                resultBuilder.append(segment.jsonArray[0].jsonPrimitive.content)
            }
        }
        resultBuilder.toString().trimEnd()
    }.onFailure { Timber.e(it, "Failed to translate text") }.getOrNull()

    suspend fun romanize(text: String): String? = runCatching {
        if (text.isBlank()) return null
        
        val lines = text.split("\n")
        val chunks = mutableListOf<String>()
        var currentChunk = java.lang.StringBuilder()
        
        for (line in lines) {
            if (currentChunk.length + line.length > 1500) {
                chunks.add(currentChunk.toString())
                currentChunk = java.lang.StringBuilder()
            }
            currentChunk.append(line).append("\n")
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        val resultBuilder = java.lang.StringBuilder()
        for (chunk in chunks) {
            val response = client.get("https://translate.googleapis.com/translate_a/single") {
                parameter("client", "gtx")
                parameter("sl", "auto")
                parameter("tl", "en")
                parameter("dt", "rm")
                parameter("q", chunk)
            }.bodyAsText()

            val jsonArray = Json.parseToJsonElement(response).jsonArray
            val segments = jsonArray.getOrNull(0)?.jsonArray
            if (segments != null) {
                for (segment in segments) {
                    if (segment is kotlinx.serialization.json.JsonArray) {
                        val romaji = segment.getOrNull(2)?.jsonPrimitive?.contentOrNull ?: segment.getOrNull(3)?.jsonPrimitive?.contentOrNull
                        if (romaji != null) {
                            resultBuilder.append(romaji)
                        }
                    }
                }
            }
        }
        
        val res = resultBuilder.toString().trimEnd()
        if (res.isBlank()) text else res
    }.onFailure { Timber.e(it, "Failed to romanize text") }.getOrNull()

    suspend fun detectLanguage(text: String): String? = runCatching {
        if (text.isBlank()) return null
        val sample = text.take(200)
        val response = client.get("https://translate.googleapis.com/translate_a/single") {
            parameter("client", "gtx")
            parameter("sl", "auto")
            parameter("tl", "en")
            parameter("dt", "t")
            parameter("q", sample)
        }.bodyAsText()

        val jsonArray = Json.parseToJsonElement(response).jsonArray
        jsonArray[2].jsonPrimitive.content
    }.onFailure { Timber.e(it, "Failed to detect language") }.getOrNull()
}
