package com.almunassiq.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiReplyClient {

    fun generateReply(
        apiKey: String,
        sender: String,
        incomingMessage: String,
        appPackage: String
    ): String {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return buildFallbackReply(sender, incomingMessage)
        }

        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$cleanKey"

        val prompt = buildString {
            append("أنت مساعد رد تلقائي ذكي ومختصر باللغة العربية.")
            if (sender.isNotBlank()) append(" المرسل: $sender.")
            if (appPackage.isNotBlank()) append(" التطبيق: $appPackage.")
            append(" اكتب رداً مناسباً ومباشراً ومختصراً على الرسالة التالية دون مقدمات طويلة: \"$incomingMessage\"")
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (code !in 200..299) {
                return buildFallbackReply(sender, incomingMessage)
            }

            extractReplyText(responseText).ifBlank { buildFallbackReply(sender, incomingMessage) }
        } catch (_: Exception) {
            buildFallbackReply(sender, incomingMessage)
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractReplyText(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            val firstCandidate = candidates.optJSONObject(0) ?: return ""
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            val firstPart = parts.optJSONObject(0) ?: return ""
            firstPart.optString("text", "").trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun buildFallbackReply(sender: String, incomingMessage: String): String {
        val who = if (sender.isNotBlank()) " يا $sender" else ""
        return "أهلاً$who، تم استلام رسالتك: \"$incomingMessage\" وسأرد عليك قريباً."
    }
}
