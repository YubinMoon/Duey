package com.terry.duey.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker {
    suspend fun fetchUpdateInfo(releasesUrl: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(releasesUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            connection.inputStream.bufferedReader().use { reader ->
                parseLatestStageRelease(reader.readText())
            }
        }
    }

    fun parseLatestStageRelease(responseBody: String): UpdateInfo {
        val candidates = JSONArray(responseBody)
            .asObjects()
            .mapNotNull { release -> release.toStageUpdateInfo() }

        require(candidates.isNotEmpty()) { "No stage APK release found" }
        return candidates.maxBy { it.versionCode }
    }

    fun isUpdateAvailable(context: Context, updateInfo: UpdateInfo): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = PackageInfoCompat.getLongVersionCode(packageInfo)
        return updateInfo.versionCode.toLong() > currentVersion
    }

    private fun JSONObject.toStageUpdateInfo(): UpdateInfo? {
        if (!optBoolean("prerelease", false)) return null

        val versionName = parseStageVersionName(optString("tag_name", "")) ?: return null
        val apkUrl = optJSONArray("assets")?.findApkUrl() ?: return null

        return UpdateInfo(
            versionCode = semVerCode(versionName),
            versionName = versionName,
            apkUrl = apkUrl,
            releaseNotes = optString("body", ""),
            forceUpdate = false,
        )
    }

    private fun parseStageVersionName(tagName: String): String? {
        val match = Regex("""^v(\d+)\.(\d+)\.(\d+)-stage$""").matchEntire(tagName) ?: return null
        return match.groupValues.drop(1).joinToString(".")
    }

    private fun JSONArray.findApkUrl(): String? = asObjects()
        .firstOrNull { asset ->
            asset.optString("name", "").endsWith(".apk") &&
                asset.optString("browser_download_url", "").startsWith("https://")
        }
        ?.optString("browser_download_url")

    private fun JSONArray.asObjects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun semVerCode(versionName: String): Int {
        val parts = versionName.split(".").map { it.toInt() }
        require(parts.size == 3) { "Invalid SemVer versionName" }

        val major = parts[0]
        val minor = parts[1]
        val patch = parts[2]
        require(major >= 0 && minor in 0..999 && patch in 0..999) { "Invalid SemVer range" }

        return major * 1_000_000 + minor * 1_000 + patch
    }
}
