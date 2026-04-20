package com.customalarm.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_groups")
data class RoutineGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

