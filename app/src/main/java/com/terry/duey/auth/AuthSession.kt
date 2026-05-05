package com.terry.duey.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthSession(context: Context) {
    private val preferences = context.getSharedPreferences("duey_auth", Context.MODE_PRIVATE)
    private val _isLoggedIn = MutableStateFlow(!preferences.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun save(accessToken: String, refreshToken: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
        _isLoggedIn.value = true
    }

    fun clear() {
        preferences.edit().clear().apply()
        _isLoggedIn.value = false
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
