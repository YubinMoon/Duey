package com.terry.duey.sync

import com.terry.duey.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class SyncPayload(
    val categories: List<RemoteCategory>,
    val todos: List<RemoteTodo>,
    val recurringTemplates: List<RemoteRecurringTemplate>,
)

data class RemoteCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val deletedAt: String,
)

data class RemoteTodo(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val startDate: String,
    val endDate: String,
    val completed: Boolean,
    val recurringTemplateId: String,
    val recurringOccurrenceDate: String,
    val deletedAt: String,
)

data class RemoteRecurringTemplate(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val repeatStartDate: String,
    val repeatEndDate: String,
    val repeatType: String,
    val weeklyDays: String,
    val monthlyDay: Int,
    val periodLengthDays: Int,
    val lastGeneratedUntil: String,
    val deletedAt: String,
)

class SyncApiClient {
    fun bootstrap(accessToken: String): Result<SyncPayload> = runCatching {
        val connection =
            (URL("${BuildConfig.SERVER_BASE_URL.trimEnd('/')}/api/sync/v1/bootstrap").openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }
        val responseText = if (connection.responseCode in 200..299) {
            BufferedReader(connection.inputStream.reader()).use { it.readText() }
        } else {
            val errorText = connection.errorStream?.reader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException(errorText.ifBlank { "동기화에 실패했습니다." })
        }
        JSONObject(responseText).toSyncPayload()
    }

    private fun JSONObject.toSyncPayload(): SyncPayload = SyncPayload(
        categories = optJSONArray("categories").orEmpty().mapObjects { json ->
            RemoteCategory(
                id = json.optString("id"),
                name = json.optString("name"),
                sortOrder = json.optInt("sortOrder"),
                deletedAt = json.optString("deletedAt"),
            )
        },
        todos = optJSONArray("todos").orEmpty().mapObjects { json ->
            RemoteTodo(
                id = json.optString("id"),
                title = json.optString("title"),
                description = json.optString("description"),
                categoryId = json.optString("categoryId"),
                startDate = json.optString("startDate"),
                endDate = json.optString("endDate"),
                completed = json.optBoolean("completed"),
                recurringTemplateId = json.optString("recurringTemplateId"),
                recurringOccurrenceDate = json.optString("recurringOccurrenceDate"),
                deletedAt = json.optString("deletedAt"),
            )
        },
        recurringTemplates = optJSONArray("recurringTemplates").orEmpty().mapObjects { json ->
            RemoteRecurringTemplate(
                id = json.optString("id"),
                title = json.optString("title"),
                description = json.optString("description"),
                categoryId = json.optString("categoryId"),
                repeatStartDate = json.optString("repeatStartDate"),
                repeatEndDate = json.optString("repeatEndDate"),
                repeatType = json.optString("repeatType"),
                weeklyDays = json.optString("weeklyDays"),
                monthlyDay = json.optInt("monthlyDay", 1),
                periodLengthDays = json.optInt("periodLengthDays", 1),
                lastGeneratedUntil = json.optString("lastGeneratedUntil"),
                deletedAt = json.optString("deletedAt"),
            )
        },
    )

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = (0 until length()).map { index -> transform(getJSONObject(index)) }
}
