package com.terry.duey.ai

import com.terry.duey.BuildConfig
import com.terry.duey.model.AppDate
import java.util.Base64
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.json.JSONObject

data class ParsedScheduleDraft(
    val title: String,
    val description: String,
    val category: String,
    val startDate: AppDate,
    val endDate: AppDate,
)

class ScheduleVoiceParser {
    fun parseAudio(audioBytes: ByteArray, mimeType: String): Result<ParsedScheduleDraft> {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다."))
        }

        return runCatching {
            val prompt = "첨부된 한국어 음성에서 일정을 추출하세요."
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)
            val schema = JSONObject(
                mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "title" to mapOf("type" to "string"),
                        "description" to mapOf("type" to "string"),
                        "category" to mapOf("type" to "string"),
                        "start_date" to mapOf("type" to "string", "format" to "date"),
                        "end_date" to mapOf("type" to "string", "format" to "date"),
                    ),
                    "required" to listOf("title", "start_date", "end_date"),
                    "additionalProperties" to false,
                ),
            )

            val body = JSONObject()
                .put(
                    "contents",
                    listOf(
                        mapOf(
                            "parts" to listOf(
                                mapOf("text" to prompt),
                                mapOf("inline_data" to mapOf("mime_type" to mimeType, "data" to base64Audio)),
                            ),
                        ),
                    ),
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("responseMimeType", "application/json")
                        .put("responseJsonSchema", schema),
                )
                .toString()

            val connection =
                (URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }

            OutputStreamWriter(connection.outputStream).use { it.write(body) }
            val responseText = BufferedReader(connection.inputStream.reader()).use { it.readText() }
            val response = JSONObject(responseText)
            val text = response
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            val json = JSONObject(text)
            val title = json.getString("title").trim()
            require(title.isNotBlank()) { "제목을 추출하지 못했습니다." }

            val today = AppDate.today()
            val start = parseDateOrToday(json.optString("start_date"), today)
            val endCandidate = parseDateOrToday(json.optString("end_date"), start)
            val end = if (endCandidate < start) start else endCandidate

            ParsedScheduleDraft(
                title = title,
                description = json.optString("description", "").trim(),
                category = json.optString("category", "기본").trim().ifBlank { "기본" },
                startDate = start,
                endDate = end,
            )
        }
    }

    private fun parseDateOrToday(raw: String, fallback: AppDate): AppDate = runCatching {
        val parsed = LocalDate.parse(raw.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        AppDate(parsed.year, parsed.monthValue, parsed.dayOfMonth)
    }.getOrDefault(fallback)
}
