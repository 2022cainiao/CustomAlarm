package com.customalarm.app.data.repository

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ZH_CN("zh-CN"),
    EN("en");

    companion object {
        fun fromStoredTag(raw: String?): AppLanguage {
            return entries.firstOrNull { it.tag == raw } ?: SYSTEM
        }
    }
}

class AppSettingsRepository(private val context: Context) {
    private val defaultSnoozeKey = intPreferencesKey("default_snooze_minutes")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val holidaySyncUrlKey = stringPreferencesKey("holiday_sync_url")

    val defaultSnoozeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[defaultSnoozeKey] ?: 10
    }

    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        AppLanguage.fromStoredTag(preferences[appLanguageKey])
    }

    val holidaySyncUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[holidaySyncUrlKey].orEmpty()
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[defaultSnoozeKey] = minutes
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            if (language == AppLanguage.SYSTEM) {
                preferences.remove(appLanguageKey)
            } else {
                preferences[appLanguageKey] = language.tag.orEmpty()
            }
        }
        applyAppLanguage(language)
    }

    suspend fun setHolidaySyncUrl(url: String) {
        val normalizedUrl = normalizeHolidaySyncUrl(url)
        context.dataStore.edit { preferences ->
            if (normalizedUrl.isBlank()) {
                preferences.remove(holidaySyncUrlKey)
            } else {
                preferences[holidaySyncUrlKey] = normalizedUrl
            }
        }
    }

    fun applySavedAppLanguageBlocking() {
        applyAppLanguage(runBlocking { appLanguage.first() })
    }

    companion object {
        fun normalizeHolidaySyncUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) {
                return ""
            }
            return if (
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "http://$trimmed"
            }
        }

        fun applyAppLanguage(language: AppLanguage) {
            val locales = if (language == AppLanguage.SYSTEM) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.tag.orEmpty())
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
