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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.customalarm.app.R
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.repository.AppLanguage
import com.customalarm.app.util.formatAlarmTime
import com.customalarm.app.util.formatInstantOrDash
import com.customalarm.app.util.formatLocalDate
import com.customalarm.app.util.formatNextTrigger
import com.customalarm.app.util.formatRepeatDays

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    exactAlarmEnabled: Boolean,
    notificationsEnabled: Boolean,
    batteryOptimizationIgnored: Boolean,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    onSyncHolidayCalendar: (String) -> Unit,
    onAddNormalAlarm: () -> Unit,
    onAddRoutineGroup: () -> Unit,
    onEditAlarm: (Long, Long?) -> Unit,
    onOpenRoutineGroup: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var moveAlarmId by remember { mutableStateOf<Long?>(null) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var holidayServerInput by rememberSaveable { mutableStateOf(uiState.holidayServerUrl) }
    var holidaySettingsExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.holidayServerUrl) {
        if (holidayServerInput != uiState.holidayServerUrl) {
            holidayServerInput = uiState.holidayServerUrl
        }
    }

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    LanguageMenuButton(
                        selectedLanguage = uiState.appLanguage,
                        expanded = languageMenuExpanded,
                        onExpandChange = { languageMenuExpanded = it },
                        onSelectLanguage = { language ->
                            viewModel.setAppLanguage(language)
                            languageMenuExpanded = false
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onAddNormalAlarm,
                    text = { Text(stringResource(R.string.action_add_alarm)) },
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add_alarm)
                        )
                    }
                )
                ExtendedFloatingActionButton(
                    onClick = onAddRoutineGroup,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = { Text(stringResource(R.string.action_add_routine_group)) },
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add_routine_group)
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            item {
                PermissionCard(
                    exactAlarmEnabled = exactAlarmEnabled,
                    notificationsEnabled = notificationsEnabled,
                    batteryOptimizationIgnored = batteryOptimizationIgnored,
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestIgnoreBatteryOptimizations = onRequestIgnoreBatteryOptimizations
                )
            }

            item {
                HolidayCalendarCard(
                    holidayCalendar = uiState.holidayCalendar,
                    expanded = holidaySettingsExpanded,
                    serverUrl = holidayServerInput,
                    onExpandChange = { holidaySettingsExpanded = it },
                    onServerUrlChange = { holidayServerInput = it },
                    onSaveServerUrl = { viewModel.saveHolidayServerUrl(holidayServerInput) },
                    onSyncHolidayCalendar = { onSyncHolidayCalendar(holidayServerInput) }
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
                    RoutineGroupCard(
                        group = group,
                        onOpen = { onOpenRoutineGroup(group.id) },
                        onToggle = { viewModel.toggleRoutineGroup(group.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageMenuButton(
    selectedLanguage: AppLanguage,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            IconButton(
                onClick = { onExpandChange(!expanded) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = stringResource(R.string.action_change_language)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) }
            ) {
                AppLanguage.entries.forEach { language ->
                    val label = languageLabel(language)
                    val finalText = if (language == selectedLanguage) {
                        stringResource(R.string.label_selected_language, label)
                    } else {
                        label
                    }
                    DropdownMenuItem(
                        text = { Text(finalText) },
                        onClick = { onSelectLanguage(language) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HolidayCalendarCard(
    holidayCalendar: HolidayCalendarUiState,
    expanded: Boolean,
    serverUrl: String,
    onExpandChange: (Boolean) -> Unit,
    onServerUrlChange: (String) -> Unit,
    onSaveServerUrl: () -> Unit,
    onSyncHolidayCalendar: () -> Unit
) {
    val warningActive = holidayCalendar.shouldWarnSourceUnavailable
    val serverConfigured = serverUrl.isNotBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (warningActive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_holiday_calendar),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(
                            R.string.label_holiday_coverage_short,
                            formatLocalDate(holidayCalendar.coverageEnd)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onExpandChange(!expanded) }) {
                    Text(
                        stringResource(
                            if (expanded) {
                                R.string.action_hide_holiday_tools
                            } else {
                                R.string.action_show_holiday_tools
                            }
                        )
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.label_holiday_source,
                    holidayCalendar.sourceName
                        ?: holidayCalendar.sourceUrl
                        ?: if (serverConfigured) {
                            serverUrl
                        } else {
                            stringResource(R.string.label_not_configured)
                        }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (expanded) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_holiday_server_address)) },
                    placeholder = { Text(stringResource(R.string.hint_holiday_server_address)) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onSaveServerUrl) {
                        Text(stringResource(R.string.action_save_server_address))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onSyncHolidayCalendar,
                        enabled = serverConfigured && !holidayCalendar.isSyncing
                    ) {
                        Text(
                            stringResource(
                                if (holidayCalendar.isSyncing) {
                                    R.string.status_syncing
                                } else {
                                    R.string.action_sync_holiday_calendar
                                }
                            )
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.label_holiday_last_sync,
                        formatInstantOrDash(holidayCalendar.syncedAt)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (holidayCalendar.shouldWarnSourceUnavailable) {
                Text(
                    text = stringResource(R.string.warning_holiday_calendar_expired),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (serverConfigured && !holidayCalendar.lastErrorMessage.isNullOrBlank() && holidayCalendar.isExpired.not()) {
                Text(
                    text = stringResource(R.string.label_holiday_sync_failed_using_local),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
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
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_next_active_alarm, formatNextTrigger(context, nextTriggerAt)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        )
    ) {
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
    batteryOptimizationIgnored: Boolean,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestIgnoreBatteryOptimizations: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.label_permissions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
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
            Text(
                stringResource(
                    R.string.label_battery_optimization,
                    stringResource(if (batteryOptimizationIgnored) R.string.label_disabled else R.string.label_enabled)
                )
            )
            if (!batteryOptimizationIgnored) {
                Text(
                    stringResource(R.string.hint_battery_optimization),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!exactAlarmEnabled) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestExactAlarmPermission
                    ) {
                        Text(stringResource(R.string.action_enable_exact_alarms))
                    }
                }
                if (!notificationsEnabled) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestNotificationPermission
                    ) {
                        Text(stringResource(R.string.action_enable_notifications))
                    }
                }
                if (!batteryOptimizationIgnored) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestIgnoreBatteryOptimizations
                    ) {
                        Text(stringResource(R.string.action_disable_battery_optimization))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isRoutineAlarm) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatAlarmTime(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        alarm.label.ifBlank { stringResource(R.string.empty_unnamed_alarm) },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                StatusBadge(
                    text = if (alarm.isRoutineAlarm) {
                        stringResource(R.string.label_source_routine_alarm, alarm.routineGroupName.orEmpty())
                    } else {
                        stringResource(R.string.label_source_standard_alarm)
                    },
                    highlighted = alarm.isRoutineAlarm
                )
            }
            Text(
                formatRepeatDays(context, alarm.repeatDays, alarm.holidayAwareWorkdays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.label_next_ring, formatNextTrigger(context, alarm.nextTriggerAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onOpen) {
                    Text(stringResource(R.string.status_open))
                }
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatAlarmTime(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        alarm.label.ifBlank { stringResource(R.string.empty_unnamed_alarm) },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(
                        text = stringResource(
                            if (alarm.enabled) R.string.label_enabled else R.string.label_disabled
                        ),
                        highlighted = alarm.enabled
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Switch(checked = alarm.enabled, onCheckedChange = onToggle)
                }
            }
            Text(
                formatRepeatDays(context, alarm.repeatDays, alarm.holidayAwareWorkdays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.label_next_ring, formatNextTrigger(context, alarm.nextTriggerAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onEdit
                ) { Text(stringResource(R.string.action_edit)) }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onMove
                ) { Text(stringResource(R.string.action_move)) }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDelete
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
private fun RoutineGroupCard(
    group: RoutineGroupSummary,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (group.enabled) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = group.enabled,
                    onCheckedChange = onToggle
                )
            }
            StatusBadge(
                text = stringResource(
                    if (group.enabled) R.string.label_enabled else R.string.label_disabled
                ),
                highlighted = group.enabled
            )
            Text(
                text = stringResource(
                    R.string.label_active_of_total,
                    group.activeCount,
                    group.alarmCount,
                    formatNextTrigger(context, group.nextTriggerAt)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpen
            ) {
                Text(stringResource(R.string.action_open_group))
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
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

@Composable
private fun StatusBadge(
    text: String,
    highlighted: Boolean
) {
    Surface(
        color = if (highlighted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.option_language_system)
        AppLanguage.ZH_CN -> stringResource(R.string.option_language_zh_cn)
        AppLanguage.EN -> stringResource(R.string.option_language_en)
    }
}
