package com.customalarm.app.data.repository

import com.customalarm.app.data.db.RoutineGroupDao
import com.customalarm.app.data.model.RoutineGroupEntity
import com.customalarm.app.data.model.RoutineGroupWithAlarms
import kotlinx.coroutines.flow.Flow

class RoutineGroupRepository(
    private val routineGroupDao: RoutineGroupDao
) {
    fun observeRoutineGroupsWithAlarms(): Flow<List<RoutineGroupWithAlarms>> =
        routineGroupDao.observeRoutineGroupsWithAlarms()

    fun observeRoutineGroup(groupId: Long): Flow<RoutineGroupWithAlarms?> =
        routineGroupDao.observeRoutineGroup(groupId)

    suspend fun getRoutineGroup(groupId: Long): RoutineGroupEntity? =
        routineGroupDao.getRoutineGroupById(groupId)

    suspend fun saveRoutineGroup(group: RoutineGroupEntity): Long {
        return if (group.id == 0L) {
            val sortOrder = routineGroupDao.getMaxSortOrder() + 1
            routineGroupDao.insert(group.copy(sortOrder = sortOrder))
        } else {
            routineGroupDao.update(group)
            group.id
        }
    }

    suspend fun setRoutineGroupEnabled(groupId: Long, enabled: Boolean) {
        val current = routineGroupDao.getRoutineGroupById(groupId) ?: return
        routineGroupDao.update(current.copy(enabled = enabled))
    }

    suspend fun deleteRoutineGroup(groupId: Long) {
        routineGroupDao.deleteById(groupId)
    }
}

