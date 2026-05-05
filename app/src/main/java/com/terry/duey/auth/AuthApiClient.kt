package com.terry.duey.auth

import com.terry.duey.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

class AuthApiClient {
    fun loginWithGoogle(idToken: String): Result<AuthTokens> = runCatching {
        val connection =
            (URL("${BuildConfig.SERVER_BASE_URL.trimEnd('/')}/api/auth/google").openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }
        val body = JSONObject().put("idToken", idToken).toString()
        OutputStreamWriter(connection.outputStream).use { it.write(body) }
        val responseText = if (connection.responseCode in 200..299) {
            BufferedReader(connection.inputStream.reader()).use { it.readText() }
        } else {
            val errorText = connection.errorStream?.reader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException(errorText.ifBlank { "로그인에 실패했습니다." })
        }
        val json = JSONObject(responseText)
        AuthTokens(
            accessToken = json.getString("accessToken"),
            refreshToken = json.getString("refreshToken"),
        )
    }
}
