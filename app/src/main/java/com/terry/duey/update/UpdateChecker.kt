package com.terry.duey.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker {
    suspend fun fetchUpdateInfo(url: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            connection.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                val versionCode = json.optInt("versionCode", -1)
                val versionName = json.optString("versionName", "")
                val apkUrl = json.optString("apkUrl", "")
                val releaseNotes = json.optString("releaseNotes", "")
                val forceUpdate = json.optBoolean("forceUpdate", false)

                require(versionCode > 0) { "Invalid versionCode" }
                require(apkUrl.startsWith("https://")) { "Only HTTPS apkUrl is allowed" }

                UpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    apkUrl = apkUrl,
                    releaseNotes = releaseNotes,
                    forceUpdate = forceUpdate,
                )
            }
        }
    }

    fun isUpdateAvailable(context: Context, updateInfo: UpdateInfo): Boolean {
        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        return updateInfo.versionCode.toLong() > currentVersion
    }
}
