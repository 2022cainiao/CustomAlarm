package com.customalarm.app.ui.alarm

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.customalarm.app.data.model.AlarmType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditorScreen(
    viewModel: AlarmEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        viewModel.updateRingtone(uri?.toString())
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (state.id == 0L) "Add alarm" else "Edit alarm") },
            navigationIcon = { OutlinedButton(onClick = onBack) { Text("Back") } }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.type == AlarmType.NORMAL,
                            onClick = { viewModel.updateType(AlarmType.NORMAL) },
                            label = { Text("Standard") }
                        )
                        FilterChip(
                            selected = state.type == AlarmType.ROUTINE,
                            onClick = { viewModel.updateType(AlarmType.ROUTINE) },
                            label = { Text("Routine") }
                        )
                    }
                    if (state.type == AlarmType.ROUTINE) {
                        Text("Routine group")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.availableGroups.forEach { group ->
                                FilterChip(
                                    selected = state.routineGroupId == group.id,
                                    onClick = { viewModel.updateRoutineGroupId(group.id) },
                                    label = { Text(group.name) }
                                )
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.hour,
                            onValueChange = viewModel::updateHour,
                            label = { Text("Hour") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.minute,
                            onValueChange = viewModel::updateMinute,
                            label = { Text("Minute") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = state.label,
                        onValueChange = viewModel::updateLabel,
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Repeat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1 to "Mon",
                            2 to "Tue",
                            3 to "Wed",
                            4 to "Thu",
                            5 to "Fri",
                            6 to "Sat",
                            7 to "Sun"
                        ).forEach { (day, label) ->
                            FilterChip(
                                selected = day in state.repeatDays,
                                onClick = { viewModel.toggleRepeatDay(day) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Alert settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate", modifier = Modifier.weight(1f))
                        Switch(checked = state.vibrate, onCheckedChange = viewModel::updateVibrate)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Snooze", modifier = Modifier.weight(1f))
                        Switch(checked = state.snoozeEnabled, onCheckedChange = viewModel::updateSnoozeEnabled)
                    }
                    if (state.snoozeEnabled) {
                        OutlinedTextField(
                            value = state.snoozeMinutes,
                            onValueChange = viewModel::updateSnoozeMinutes,
                            label = { Text("Snooze minutes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable after saving", modifier = Modifier.weight(1f))
                        Switch(checked = state.enabled, onCheckedChange = viewModel::updateEnabled)
                    }
                    Text("Ringtone: ${state.ringtoneUri ?: "System default"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { ringtoneLauncher.launch(arrayOf("audio/*")) }) {
                            Text("Choose")
                        }
                        OutlinedButton(onClick = { viewModel.updateRingtone(null) }) {
                            Text("Use default")
                        }
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Saving..." else "Save alarm")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
