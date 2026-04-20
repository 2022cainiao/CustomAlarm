package com.customalarm.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.customalarm.app.data.model.RoutineGroupEntity
import com.customalarm.app.data.model.RoutineGroupWithAlarms
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineGroupDao {
    @Transaction
    @Query("SELECT * FROM routine_groups ORDER BY sortOrder, createdAt, id")
    fun observeRoutineGroupsWithAlarms(): Flow<List<RoutineGroupWithAlarms>>

    @Transaction
    @Query("SELECT * FROM routine_groups WHERE id = :groupId LIMIT 1")
    fun observeRoutineGroup(groupId: Long): Flow<RoutineGroupWithAlarms?>

    @Query("SELECT * FROM routine_groups WHERE id = :groupId LIMIT 1")
    suspend fun getRoutineGroupById(groupId: Long): RoutineGroupEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM routine_groups")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: RoutineGroupEntity): Long

    @Update
    suspend fun update(group: RoutineGroupEntity)

    @Query("DELETE FROM routine_groups WHERE id = :groupId")
    suspend fun deleteById(groupId: Long)
}

