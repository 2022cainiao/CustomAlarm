package com.customalarm.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.model.AlarmScheduleCandidate
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms WHERE type = 'NORMAL' ORDER BY hour, minute, id")
    fun observeNormalAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE routineGroupId = :groupId ORDER BY hour, minute, id")
    fun observeRoutineGroupAlarms(groupId: Long): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :alarmId LIMIT 1")
    suspend fun getAlarmById(alarmId: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE routineGroupId = :groupId ORDER BY hour, minute, id")
    suspend fun getRoutineGroupAlarms(groupId: Long): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteById(alarmId: Long)

    @Query("DELETE FROM alarms WHERE routineGroupId = :groupId")
    suspend fun deleteByRoutineGroupId(groupId: Long)

    @Transaction
    @Query(
        """
        SELECT alarms.*, routine_groups.enabled AS routineGroupEnabled
        FROM alarms
        LEFT JOIN routine_groups ON alarms.routineGroupId = routine_groups.id
        """
    )
    suspend fun getScheduleCandidates(): List<AlarmScheduleCandidate>
}

