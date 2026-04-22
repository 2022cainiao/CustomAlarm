package com.customalarm.app.ui.alarm

import android.content.Intent
import android.media.RingtoneManager
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.customalarm.app.R
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
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.updateRingtone(extractPickedRingtoneUri(result.data)?.toString())
    }
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
            title = {
                Text(
                    if (state.id == 0L) {
                        stringResource(R.string.screen_add_alarm)
                    } else {
                        stringResource(R.string.screen_edit_alarm)
                    }
                )
            },
            navigationIcon = { OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }
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
                    Text(
                        stringResource(R.string.label_alarm_placement),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.type == AlarmType.NORMAL,
                            onClick = { viewModel.updateType(AlarmType.NORMAL) },
                            label = { Text(stringResource(R.string.label_standard)) }
                        )
                        FilterChip(
                            selected = state.type == AlarmType.ROUTINE,
                            onClick = { viewModel.updateType(AlarmType.ROUTINE) },
                            label = { Text(stringResource(R.string.label_routine)) }
                        )
                    }
                    Text(
                        text = if (state.type == AlarmType.NORMAL) {
                            stringResource(R.string.hint_standard_alarm_management)
                        } else {
                            stringResource(
                                if (state.availableGroups.isEmpty()) {
                                    R.string.hint_create_routine_group_first
                                } else {
                                    R.string.hint_routine_alarm_management
                                }
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.type == AlarmType.ROUTINE && state.availableGroups.isEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (state.type == AlarmType.ROUTINE) {
                        Text(stringResource(R.string.label_routine_group))
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
                    Text(
                        stringResource(R.string.label_time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.hour,
                            onValueChange = viewModel::updateHour,
                            label = { Text(stringResource(R.string.label_hour)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.minute,
                            onValueChange = viewModel::updateMinute,
                            label = { Text(stringResource(R.string.label_minute)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = state.label,
                        onValueChange = viewModel::updateLabel,
                        label = { Text(stringResource(R.string.label_alarm_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.label_repeat),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1 to R.string.day_mon_short,
                            2 to R.string.day_tue_short,
                            3 to R.string.day_wed_short,
                            4 to R.string.day_thu_short,
                            5 to R.string.day_fri_short,
                            6 to R.string.day_sat_short,
                            7 to R.string.day_sun_short
                        ).forEach { (day, labelRes) ->
                            FilterChip(
                                selected = day in state.repeatDays,
                                enabled = !state.holidayAwareWorkdays,
                                onClick = { viewModel.toggleRepeatDay(day) },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_holiday_aware_workdays), modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.holidayAwareWorkdays,
                            onCheckedChange = viewModel::updateHolidayAwareWorkdays
                        )
                    }
                    Text(
                        text = stringResource(R.string.hint_holiday_aware_workdays),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.label_alert_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_vibrate), modifier = Modifier.weight(1f))
                        Switch(checked = state.vibrate, onCheckedChange = viewModel::updateVibrate)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_snooze), modifier = Modifier.weight(1f))
                        Switch(checked = state.snoozeEnabled, onCheckedChange = viewModel::updateSnoozeEnabled)
                    }
                    if (state.snoozeEnabled) {
                        OutlinedTextField(
                            value = state.snoozeMinutes,
                            onValueChange = viewModel::updateSnoozeMinutes,
                            label = { Text(stringResource(R.string.label_snooze_minutes)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_enable_after_saving), modifier = Modifier.weight(1f))
                        Switch(checked = state.enabled, onCheckedChange = viewModel::updateEnabled)
                    }
                    Text(
                        stringResource(
                            R.string.label_ringtone,
                            resolveRingtoneLabel(context, state.ringtoneUri)
                        )
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ringtonePickerLauncher.launch(
                                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                        putExtra(
                                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                            state.ringtoneUri?.let(Uri::parse)
                                        )
                                    }
                                )
                            }
                        ) {
                            Text(stringResource(R.string.action_choose_ringtone))
                        }
                        OutlinedButton(onClick = { ringtoneLauncher.launch(arrayOf("audio/*")) }) {
                            Text(stringResource(R.string.action_choose_audio_file))
                        }
                        OutlinedButton(onClick = { viewModel.updateRingtone(null) }) {
                            Text(stringResource(R.string.action_use_default))
                        }
                    }
                }
            }

            state.errorMessageRes?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isSaving) {
                        stringResource(R.string.status_saving)
                    } else {
                        stringResource(R.string.action_save_alarm)
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun resolveRingtoneLabel(context: android.content.Context, ringtoneUri: String?): String {
    if (ringtoneUri.isNullOrBlank()) {
        return context.getString(R.string.label_system_default)
    }

    val uri = Uri.parse(ringtoneUri)
    return RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        ?: uri.lastPathSegment
        ?: context.getString(R.string.label_system_default)
}

private fun extractPickedRingtoneUri(data: Intent?): Uri? {
    if (data == null) return null
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
}
