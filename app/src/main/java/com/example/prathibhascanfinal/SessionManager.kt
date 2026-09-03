package com.example.prathibhascanfinal

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_UNIQUE_ID = "unique_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_SPORT = "user_sport"
        private const val KEY_REGISTRATION_COMPLETE = "reg_complete"
    }

    fun saveSession(email: String, name: String, uniqueId: String, role: String, sport: String? = null) {
        prefs.edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_UNIQUE_ID, uniqueId)
            putString(KEY_USER_ROLE, role)
            putString(KEY_USER_SPORT, sport)
        }
    }

    fun setRegistrationComplete(complete: Boolean) {
        prefs.edit { putBoolean(KEY_REGISTRATION_COMPLETE, complete) }
    }

    fun isRegistrationComplete(): Boolean = prefs.getBoolean(KEY_REGISTRATION_COMPLETE, false)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getName(): String? = prefs.getString(KEY_USER_NAME, "User")
    fun getUniqueId(): String? = prefs.getString(KEY_UNIQUE_ID, "PR-ATH-0000")
    fun getRole(): String? = prefs.getString(KEY_USER_ROLE, "Athlete")
    fun getSport(): String? = prefs.getString(KEY_USER_SPORT, null)

    fun logout() {
        prefs.edit { clear() }
    }
}
