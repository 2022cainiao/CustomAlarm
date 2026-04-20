package com.customalarm.app.ui

object NavRoutes {
    const val HOME = "home"
    const val ALARM_EDITOR = "alarm_editor"
    const val ROUTINE_DETAIL = "routine_detail"
    const val ROUTINE_EDITOR = "routine_editor"

    fun alarmEditor(alarmId: Long? = null, routineGroupId: Long? = null): String {
        val alarmPart = alarmId ?: -1L
        val groupPart = routineGroupId ?: -1L
        return "$ALARM_EDITOR/$alarmPart/$groupPart"
    }

    fun routineDetail(groupId: Long): String = "$ROUTINE_DETAIL/$groupId"

    fun routineEditor(groupId: Long? = null): String = "$ROUTINE_EDITOR/${groupId ?: -1L}"
}

