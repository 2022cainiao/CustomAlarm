package com.customalarm.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.customalarm.app.R
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.util.formatAlarmTime
import com.customalarm.app.util.formatNextTrigger
import com.customalarm.app.util.formatRepeatDays

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutineDetailScreen(
    viewModel: RoutineDetailViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEditGroup: () -> Unit,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var moveAlarmId by remember { mutableStateOf<Long?>(null) }

    if (!state.exists) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.empty_routine_group_not_found))
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        }
        return
    }

    if (moveAlarmId != null) {
        MoveAlarmDialog(
            moveTargets = state.moveTargets,
            onDismiss = { moveAlarmId = null },
            onMoveToStandard = {
                moveAlarmId?.let(viewModel::moveAlarmToStandard)
                moveAlarmId = null
            },
            onMoveToGroup = { targetGroupId ->
                moveAlarmId?.let { viewModel.moveAlarmToGroup(it, targetGroupId) }
                moveAlarmId = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.groupName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }
            )
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.label_routine_master_switch),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val nextTrigger = state.alarms
                                    .filter { it.enabled && state.enabled }
                                    .mapNotNull { it.nextTriggerAt }
                                    .minOrNull()
                                Text(
                                    text = stringResource(
                                        R.string.label_next_ring,
                                        formatNextTrigger(context, nextTrigger)
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = state.enabled, onCheckedChange = viewModel::toggleGroup)
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = onEditGroup) {
                                Text(stringResource(R.string.action_edit_group))
                            }
                            OutlinedButton(onClick = { viewModel.deleteGroup(onDeleted) }) {
                                Text(stringResource(R.string.action_delete_group))
                            }
                            Button(onClick = onAddAlarm) {
                                Text(stringResource(R.string.action_add_group_alarm))
                            }
                        }
                    }
                }
            }

            if (state.alarms.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.empty_routine_group_alarms), modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(state.alarms, key = { it.id }) { alarm ->
                    RoutineAlarmCard(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm.id, it) },
                        onEdit = { onEditAlarm(alarm.id) },
                        onMove = { moveAlarmId = alarm.id },
                        onDelete = { viewModel.deleteAlarm(alarm.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineAlarmCard(
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = alarm.label.ifBlank { stringResource(R.string.empty_unnamed_alarm) },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
            Text(
                text = formatRepeatDays(context, alarm.repeatDays, alarm.holidayAwareWorkdays),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.label_next_ring, formatNextTrigger(context, alarm.nextTriggerAt)),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                OutlinedButton(onClick = onMove) { Text(stringResource(R.string.action_move)) }
                OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
            }
        }
    }
}

@Composable
private fun MoveAlarmDialog(
    moveTargets: List<RoutineMoveTarget>,
    onDismiss: () -> Unit,
    onMoveToStandard: () -> Unit,
    onMoveToGroup: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_move_alarm)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.label_move_alarm_message))
                OutlinedButton(onClick = onMoveToStandard, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_move_to_standard))
                }
                if (moveTargets.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_no_other_routine_groups),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    moveTargets.forEach { target ->
                        OutlinedButton(
                            onClick = { onMoveToGroup(target.groupId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_move_to_group, target.groupName))
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
