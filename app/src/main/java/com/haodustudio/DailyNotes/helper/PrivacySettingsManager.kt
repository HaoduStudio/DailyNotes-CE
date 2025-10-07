package com.haodustudio.DailyNotes.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.haodustudio.DailyNotes.BaseApplication

object PrivacySettingsManager {
    private const val KEY_CRASH_REPORTING = "privacy_crash_reporting_enabled"
    private const val KEY_LOCATION = "privacy_location_enabled"
    private const val KEY_CLOUD_RESOURCES = "privacy_cloud_resources_enabled"

    private val prefs: SharedPreferences by lazy {
        BaseApplication.instance.getSharedPreferences(
            BaseApplication.APP_SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    }

    fun isCrashReportingEnabled(): Boolean = prefs.getBoolean(KEY_CRASH_REPORTING, true)

    fun updateCrashReportingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CRASH_REPORTING, enabled) }
        BaseApplication.instance.updateSentryState(enabled)
    }

    fun isLocationEnabled(): Boolean = prefs.getBoolean(KEY_LOCATION, true)

    fun updateLocationEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_LOCATION, enabled)
            if (!enabled && prefs.getBoolean("app_background_is_weather", false)) {
                putBoolean("app_background_is_weather", false)
            }
        }
    }

    fun isCloudResourcesEnabled(): Boolean = prefs.getBoolean(KEY_CLOUD_RESOURCES, true)

    fun updateCloudResourcesEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CLOUD_RESOURCES, enabled) }
    }
}
