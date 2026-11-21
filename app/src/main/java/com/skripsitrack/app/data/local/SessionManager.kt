package com.skripsitrack.app.data.local

import android.content.Context
import com.skripsitrack.app.data.model.User
import com.skripsitrack.app.data.remote.RetrofitClient

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("skripsitrack_session", Context.MODE_PRIVATE)

    private val settings = context.getSharedPreferences(
        "skripsitrack_settings",
        Context.MODE_PRIVATE
    )

    fun getBaseUrl(): String =
        settings.getString("base_url", null) ?: RetrofitClient.DEFAULT_BASE_URL

    fun setBaseUrl(url: String) =
        settings.edit().putString("base_url", url).apply()

    fun saveSession(token: String, user: User) {
        prefs.edit()
            .putString("token", token)
            .putInt("user_id", user.id)
            .putString("name", user.name)
            .putString("email", user.email)
            .putString("role", user.role)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun getName(): String = prefs.getString("name", "") ?: ""
    fun getRole(): String = prefs.getString("role", "") ?: ""
    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()
    fun clear() = prefs.edit().clear().apply()
    fun getEmail(): String = prefs.getString("email", "") ?: ""
}