package com.customalarm.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.customalarm.app.R
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.util.formatAlarmTime
import com.customalarm.app.util.formatNextTrigger
import com.customalarm.app.util.formatRepeatDays

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    exactAlarmEnabled: Boolean,
    notificationsEnabled: Boolean,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onAddNormalAlarm: () -> Unit,
    onAddRoutineGroup: () -> Unit,
    onEditAlarm: (Long, Long?) -> Unit,
    onOpenRoutineGroup: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var moveAlarmId by remember { mutableStateOf<Long?>(null) }

    if (moveAlarmId != null) {
        MoveToRoutineGroupDialog(
            moveTargets = uiState.routineGroups,
            onDismiss = { moveAlarmId = null },
            onMoveToGroup = { targetGroupId ->
                moveAlarmId?.let { viewModel.moveAlarmToRoutineGroup(it, targetGroupId) }
                moveAlarmId = null
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.screen_home_title)) }) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = onAddRoutineGroup) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_routine_group))
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(onClick = onAddNormalAlarm) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_alarm))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PermissionCard(
                    exactAlarmEnabled = exactAlarmEnabled,
                    notificationsEnabled = notificationsEnabled,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
            }

            item {
                OverviewCard(
                    totalAlarmCount = uiState.totalAlarmCount,
                    enabledNormalCount = uiState.enabledNormalCount,
                    routineGroupCount = uiState.routineGroupCount,
                    enabledRoutineGroupCount = uiState.enabledRoutineGroupCount,
                    effectiveRoutineAlarmCount = uiState.effectiveRoutineAlarmCount,
                    nextTriggerAt = uiState.nextTriggerAt
                )
            }

            item { SectionHeader(title = stringResource(R.string.label_coming_up)) }

            if (uiState.upcomingAlarms.isEmpty()) {
                item { EmptyState(stringResource(R.string.empty_active_alarms)) }
            } else {
                items(uiState.upcomingAlarms, key = { it.alarmId }) { alarm ->
                    UpcomingAlarmCard(
                        alarm = alarm,
                        onOpen = { onEditAlarm(alarm.alarmId, alarm.routineGroupId) }
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.label_standard_alarms)) }

            if (uiState.normalAlarms.isEmpty()) {
                item { EmptyState(stringResource(R.string.empty_standard_alarms)) }
            } else {
                items(uiState.normalAlarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm.id, it) },
                        onEdit = { onEditAlarm(alarm.id, null) },
                        onMove = { moveAlarmId = alarm.id },
                        onDelete = { viewModel.deleteAlarm(alarm.id) }
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.label_routine_groups)) }

            if (uiState.routineGroups.isEmpty()) {
                item { EmptyState(stringResource(R.string.empty_routine_groups)) }
            } else {
                items(uiState.routineGroups, key = { it.id }) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRoutineGroup(group.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.label_active_of_total,
                                            group.activeCount,
                                            group.alarmCount,
                                            formatNextTrigger(context, group.nextTriggerAt)
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Switch(
                                    checked = group.enabled,
                                    onCheckedChange = { viewModel.toggleRoutineGroup(group.id, it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { onOpenRoutineGroup(group.id) }) {
                                    Text(stringResource(R.string.action_open_group))
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    totalAlarmCount: Int,
    enabledNormalCount: Int,
    routineGroupCount: Int,
    enabledRoutineGroupCount: Int,
    effectiveRoutineAlarmCount: Int,
    nextTriggerAt: Long?
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.label_overview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.label_next_active_alarm, formatNextTrigger(context, nextTriggerAt)),
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverviewMetric(
                    title = stringResource(R.string.label_all_alarms),
                    value = totalAlarmCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                OverviewMetric(
                    title = stringResource(R.string.label_standard_on),
                    value = enabledNormalCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverviewMetric(
                    title = stringResource(R.string.label_routine_groups_on),
                    value = "$enabledRoutineGroupCount / $routineGroupCount",
                    modifier = Modifier.weight(1f)
                )
                OverviewMetric(
                    title = stringResource(R.string.label_routine_alarms_active),
                    value = effectiveRoutineAlarmCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverviewMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PermissionCard(
    exactAlarmEnabled: Boolean,
    notificationsEnabled: Boolean,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.label_permissions), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.label_exact_alarms,
                    stringResource(if (exactAlarmEnabled) R.string.label_enabled else R.string.label_disabled)
                )
            )
            Text(
                stringResource(
                    R.string.label_notifications,
                    stringResource(if (notificationsEnabled) R.string.label_enabled else R.string.label_disabled)
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!exactAlarmEnabled) {
                    OutlinedButton(onClick = onRequestExactAlarmPermission) {
                        Text(stringResource(R.string.action_enable_exact_alarms))
                    }
                }
                if (!notificationsEnabled) {
                    OutlinedButton(onClick = onRequestNotificationPermission) {
                        Text(stringResource(R.string.action_enable_notifications))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyState(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun UpcomingAlarmCard(
    alarm: UpcomingAlarmSummary,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatAlarmTime(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(alarm.label.ifBlank { stringResource(R.string.empty_unnamed_alarm) })
                }
                OutlinedButton(onClick = onOpen) {
                    Text(stringResource(R.string.status_open))
                }
            }
            Text(
                if (alarm.isRoutineAlarm) {
                    stringResource(R.string.label_source_routine_alarm, alarm.routineGroupName.orEmpty())
                } else {
                    stringResource(R.string.label_source_standard_alarm)
                }
            )
            Text(formatRepeatDays(context, alarm.repeatDays, alarm.holidayAwareWorkdays))
            Text(stringResource(R.string.label_next_ring, formatNextTrigger(context, alarm.nextTriggerAt)))
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatAlarmTime(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(alarm.label.ifBlank { stringResource(R.string.empty_unnamed_alarm) })
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
            Text(formatRepeatDays(context, alarm.repeatDays, alarm.holidayAwareWorkdays))
            Text(stringResource(R.string.label_next_ring, formatNextTrigger(context, alarm.nextTriggerAt)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                OutlinedButton(onClick = onMove) { Text(stringResource(R.string.action_move)) }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
private fun MoveToRoutineGroupDialog(
    moveTargets: List<RoutineGroupSummary>,
    onDismiss: () -> Unit,
    onMoveToGroup: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_move_alarm)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.label_move_alarm_message))
                if (moveTargets.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_no_other_routine_groups),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    moveTargets.forEach { target ->
                        OutlinedButton(
                            onClick = { onMoveToGroup(target.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_move_to_group, target.name))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.status_cancel))
            }
        }
    )
}
