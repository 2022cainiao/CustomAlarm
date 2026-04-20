package com.customalarm.app.data.db

import androidx.room.TypeConverter
import com.customalarm.app.data.model.AlarmType

class Converters {
    @TypeConverter
    fun fromAlarmType(value: AlarmType): String = value.name

    @TypeConverter
    fun toAlarmType(value: String): AlarmType = AlarmType.valueOf(value)

    @TypeConverter
    fun fromRepeatDays(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toRepeatDays(value: String): List<Int> {
        if (value.isBlank()) return emptyList()
        return value.split(",").mapNotNull { it.toIntOrNull() }
    }
}

