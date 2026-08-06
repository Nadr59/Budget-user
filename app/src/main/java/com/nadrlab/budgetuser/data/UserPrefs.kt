
package com.nadrlab.budgetuser.data

import android.content.Context
import android.content.SharedPreferences

class UserPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bait_budget_user", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) = prefs.edit().putString("user_name", value).apply()

    val isSetupComplete: Boolean
        get() = userName.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
