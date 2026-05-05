package com.terry.duey.ai

import com.terry.duey.BuildConfig
import com.terry.duey.model.AppDate
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ParsedScheduleDraft(
    val title: String,
    val description: String,
    val category: String,
    val startDate: AppDate,
    val endDate: AppDate,
)

class ScheduleVoiceParser {
    fun parseAudio(audioBytes: ByteArray, mimeType: String, accessToken: String?): Result<ParsedScheduleDraft> {
        if (accessToken.isNullOrBlank()) {
            return Result.failure(IllegalStateException("로그인이 필요합니다."))
        }
        if (audioBytes.isEmpty()) {
            return Result.failure(IllegalStateException("음성 데이터가 비어 있습니다."))
        }

        return runCatching {
            val boundary = "DueyBoundary${UUID.randomUUID()}"
            val connection =
                (URL("${BuildConfig.SERVER_BASE_URL.trimEnd('/')}/api/ai/schedule/voice").openConnection() as HttpURLConnection)
                    .apply {
                        requestMethod = "POST"
                        setRequestProperty("Authorization", "Bearer $accessToken")
                        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                        doOutput = true
                        connectTimeout = 10_000
                        readTimeout = 20_000
                    }

            connection.outputStream.use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.write("--$boundary\r\n")
                    writer.write("Content-Disposition: form-data; name=\"mimeType\"\r\n\r\n")
                    writer.write(mimeType)
                    writer.write("\r\n")
                    writer.write("--$boundary\r\n")
                    writer.write("Content-Disposition: form-data; name=\"audio\"; filename=\"voice.m4a\"\r\n")
                    writer.write("Content-Type: $mimeType\r\n\r\n")
                    writer.flush()
                    output.write(audioBytes)
                    output.flush()
                    writer.write("\r\n--$boundary--\r\n")
                }
            }

            val responseText = if (connection.responseCode in 200..299) {
                BufferedReader(connection.inputStream.reader()).use { it.readText() }
            } else {
                val errorText = connection.errorStream?.reader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(errorText.ifBlank { "음성 일정 변환에 실패했습니다." })
            }
            val json = JSONObject(responseText)
            val title = json.getString("title").trim()
            require(title.isNotBlank()) { "제목을 추출하지 못했습니다." }

            val today = AppDate.today()
            val start = parseDateOrToday(json.optString("startDate"), today)
            val endCandidate = parseDateOrToday(json.optString("endDate"), start)
            val end = if (endCandidate < start) start else endCandidate

            ParsedScheduleDraft(
                title = title,
                description = json.optString("description", "").trim(),
                category = json.optString("category", "").trim(),
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
