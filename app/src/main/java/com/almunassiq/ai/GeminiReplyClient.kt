package com.almunassiq.ai

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

object GeminiReplyClient {

    fun generateReply(
        apiKey: String,
        incomingMessage: String,
        fallbackReply: String
    ): String {
        val cleanKey = apiKey.trim()
        val cleanMessage = incomingMessage.trim()
        val cleanFallback = fallbackReply.trim().ifEmpty {
            "تم استلام رسالتك، سنرد عليك قريباً."
        }

        if (cleanKey.isEmpty() || cleanMessage.isEmpty()) {
            return cleanFallback
        }

        return try {
            val endpoint = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$cleanKey"
            )
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val promptText =
                "أنت مساعد رد تلقائي ذكي باللغة العربية. اكتب رداً قصيراً ومباشراً ومناسباً للرسالة التالية، بدون مقدمات طويلة:\n\"$cleanMessage\""

            val partObj = JSONObject().put("text", promptText)
            val contentObj = JSONObject().put("parts", JSONArray().put(partObj))
            val payload = JSONObject().put("contents", JSONArray().put(contentObj))

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return cleanFallback
            }

            val responseText = BufferedReader(
                InputStreamReader(connection.inputStream, Charsets.UTF_8)
            ).use { it.readText() }

            connection.disconnect()

            val parsed = extractTextFromResponse(responseText)?.trim()
            if (parsed.isNullOrEmpty()) cleanFallback else parsed
        } catch (_: Exception) {
            cleanFallback
        }
    }

    private fun extractTextFromResponse(jsonStr: String): String? {
        return try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            parts.optJSONObject(0)?.optString("text")
        } catch (_: Exception) {
            null
        }
    }
}
