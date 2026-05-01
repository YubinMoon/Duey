package com.terry.duey.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ApkDownloader {
    suspend fun download(context: Context, apkUrl: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(apkUrl.startsWith("https://")) { "Only HTTPS downloads are allowed" }
            val outputDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            val outputFile = File(outputDir, "duey-stage-update.apk")
            if (outputFile.exists()) outputFile.delete()

            val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                requestMethod = "GET"
            }
            connection.inputStream.use { input ->
                outputFile.outputStream().use { output -> input.copyTo(output) }
            }
            outputFile
        }
    }
}
