package com.customalarm.app

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.customalarm.app.data.db.AppDatabase
import com.customalarm.app.data.repository.AlarmRepository
import com.customalarm.app.data.repository.AppSettingsRepository
import com.customalarm.app.data.repository.RoutineGroupRepository
import com.customalarm.app.domain.AlarmCoordinator
import com.customalarm.app.domain.HolidayCalendar
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
    ).addMigrations(MIGRATION_1_2)
        .build()

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val holidayCalendar = HolidayCalendar()
    val nextTriggerCalculator = NextTriggerCalculator(holidayCalendar = holidayCalendar)
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

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE alarms ADD COLUMN holidayAwareWorkdays INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
