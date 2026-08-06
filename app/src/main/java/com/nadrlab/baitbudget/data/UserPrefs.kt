package com.nadrlab.baitbudget.data

import android.content.Context
import android.content.SharedPreferences

class UserPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bait_budget_prefs", Context.MODE_PRIVATE)

    var isAdmin: Boolean
        get() = prefs.getBoolean("is_admin", false)
        set(value) = prefs.edit().putBoolean("is_admin", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) = prefs.edit().putString("user_name", value).apply()

    var adminPassword: String
        get() = prefs.getString("admin_password", "1234") ?: "1234"
        set(value) = prefs.edit().putString("admin_password", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("is_admin", false)
            .putString("user_name", "")
            .apply()
    }

    fun checkAdminPassword(password: String): Boolean {
        return password == adminPassword
    }

    fun changeAdminPassword(oldPassword: String, newPassword: String): Boolean {
        return if (oldPassword == adminPassword) {
            adminPassword = newPassword
            true
        } else false
    }
}
