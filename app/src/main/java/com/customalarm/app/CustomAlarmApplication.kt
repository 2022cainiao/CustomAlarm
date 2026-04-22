package com.customalarm.app

import android.app.Application
import android.content.Context
import com.customalarm.app.data.repository.AppSettingsRepository

class CustomAlarmApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppSettingsRepository(this).applySavedAppLanguageBlocking()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CustomAlarmApplication).container
