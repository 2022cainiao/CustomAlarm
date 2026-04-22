package com.customalarm.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(state.groupName) },
            navigationIcon = { OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
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
                                Text(stringResource(R.string.label_next_ring, formatNextTrigger(context, nextTrigger)))
                            }
                            Switch(checked = state.enabled, onCheckedChange = viewModel::toggleGroup)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onEditGroup) { Text(stringResource(R.string.action_edit_group)) }
                            OutlinedButton(onClick = { viewModel.deleteGroup(onDeleted) }) {
                                Text(stringResource(R.string.action_delete_group))
                            }
                            Button(onClick = onAddAlarm) { Text(stringResource(R.string.action_add_group_alarm)) }
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
                        onMoveToStandard = { viewModel.moveAlarmToStandard(alarm.id) },
                        onDelete = { viewModel.deleteAlarm(alarm.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineAlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onMoveToStandard: () -> Unit,
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(alarm.label.ifBlank { stringResource(R.string.empty_unnamed_alarm) })
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
            Text(formatRepeatDays(context, alarm.repeatDays))
            Text(stringResource(R.string.label_next_ring, formatNextTrigger(context, alarm.nextTriggerAt)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                OutlinedButton(onClick = onMoveToStandard) { Text(stringResource(R.string.action_move_to_standard)) }
                OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
            }
        }
    }
}
