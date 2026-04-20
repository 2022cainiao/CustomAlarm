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
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Custom Alarm") }) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = onAddRoutineGroup) {
                    Icon(Icons.Filled.Add, contentDescription = "Add routine group")
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(onClick = onAddNormalAlarm) {
                    Icon(Icons.Filled.Add, contentDescription = "Add alarm")
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

            item { SectionHeader(title = "Standard alarms") }

            if (uiState.normalAlarms.isEmpty()) {
                item { EmptyState("No standard alarms yet") }
            } else {
                items(uiState.normalAlarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm.id, it) },
                        onEdit = { onEditAlarm(alarm.id, null) },
                        onDelete = { viewModel.deleteAlarm(alarm.id) }
                    )
                }
            }

            item { SectionHeader(title = "Routine groups") }

            if (uiState.routineGroups.isEmpty()) {
                item { EmptyState("No routine groups yet") }
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
                                        text = "${group.activeCount} active alarms · ${formatNextTrigger(group.nextTriggerAt)}",
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
                                    Text("Open group")
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
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            Text("Exact alarms: ${if (exactAlarmEnabled) "Enabled" else "Disabled"}")
            Text("Notifications: ${if (notificationsEnabled) "Enabled" else "Disabled"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!exactAlarmEnabled) {
                    OutlinedButton(onClick = onRequestExactAlarmPermission) {
                        Text("Enable exact alarms")
                    }
                }
                if (!notificationsEnabled) {
                    OutlinedButton(onClick = onRequestNotificationPermission) {
                        Text("Enable notifications")
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
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    Text(alarm.label.ifBlank { "Unnamed alarm" })
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
            Text(formatRepeatDays(alarm.repeatDays))
            Text("Next ring: ${formatNextTrigger(alarm.nextTriggerAt)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("Delete")
                }
            }
        }
    }
}
