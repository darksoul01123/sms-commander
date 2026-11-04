package com.smstrigger.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "sms_commander_prefs"
        private const val KEY_TRIGGER_CODE = "trigger_code"
        private const val KEY_APP_ENABLED = "app_enabled"
        private const val KEY_MATCH_MODE = "match_mode" // "exact" or "contains"
        private const val KEY_EVENT_LOGS = "event_logs"
        private const val MAX_LOGS = 25

        const val MATCH_EXACT = "exact"
        const val MATCH_CONTAINS = "contains"
        const val DEFAULT_TRIGGER = "red"
    }

    var triggerCode: String
        get() = prefs.getString(KEY_TRIGGER_CODE, DEFAULT_TRIGGER)?.trim() ?: DEFAULT_TRIGGER
        set(value) = prefs.edit().putString(KEY_TRIGGER_CODE, value.trim()).apply()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_ENABLED, value).apply()

    var matchMode: String
        get() = prefs.getString(KEY_MATCH_MODE, MATCH_EXACT) ?: MATCH_EXACT
        set(value) = prefs.edit().putString(KEY_MATCH_MODE, value).apply()

    fun getLogs(): List<String> {
        val raw = prefs.getString(KEY_EVENT_LOGS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("###").filter { it.isNotBlank() }
    }

    fun addLog(sender: String, message: String, actionResult: String) {
        val time = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] From: $sender | Msg: \"$message\" | Status: $actionResult"
        val existing = getLogs().toMutableList()
        existing.add(0, entry)
        if (existing.size > MAX_LOGS) {
            existing.removeAt(existing.size - 1)
        }
        prefs.edit().putString(KEY_EVENT_LOGS, existing.joinToString("###")).apply()
    }

    fun clearLogs() {
        prefs.edit().remove(KEY_EVENT_LOGS).apply()
    }
}
