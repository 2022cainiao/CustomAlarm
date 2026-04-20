package com.customalarm.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.model.RoutineGroupEntity

@Database(
    entities = [AlarmEntity::class, RoutineGroupEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun routineGroupDao(): RoutineGroupDao
}

