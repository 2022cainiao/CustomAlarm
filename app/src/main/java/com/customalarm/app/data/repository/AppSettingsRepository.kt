package com.customalarm.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(private val context: Context) {
    private val defaultSnoozeKey = intPreferencesKey("default_snooze_minutes")

    val defaultSnoozeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[defaultSnoozeKey] ?: 10
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[defaultSnoozeKey] = minutes
        }
    }
}

