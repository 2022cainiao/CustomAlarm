package com.customalarm.app

import android.content.Context
import androidx.room.Room
import com.customalarm.app.data.db.AppDatabase
import com.customalarm.app.data.repository.AlarmRepository
import com.customalarm.app.data.repository.AppSettingsRepository
import com.customalarm.app.data.repository.RoutineGroupRepository
import com.customalarm.app.domain.AlarmCoordinator
import com.customalarm.app.domain.AlarmRingingController
import com.customalarm.app.domain.AlarmScheduler
import com.customalarm.app.domain.NextTriggerCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val database = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "custom_alarm.db"
    ).build()

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val nextTriggerCalculator = NextTriggerCalculator()
    val alarmRepository = AlarmRepository(database.alarmDao(), nextTriggerCalculator)
    val routineGroupRepository = RoutineGroupRepository(database.routineGroupDao())
    val appSettingsRepository = AppSettingsRepository(applicationContext)
    val alarmScheduler = AlarmScheduler(applicationContext, alarmRepository)
    val alarmRingingController = AlarmRingingController(applicationContext)
    val alarmCoordinator = AlarmCoordinator(
        alarmRepository = alarmRepository,
        routineGroupRepository = routineGroupRepository,
        alarmScheduler = alarmScheduler
    )
}

