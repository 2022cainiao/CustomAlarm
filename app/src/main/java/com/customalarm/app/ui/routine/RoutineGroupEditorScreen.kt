package com.customalarm.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.customalarm.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineGroupEditorScreen(
    viewModel: RoutineGroupEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state = viewModel.uiState

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
                        stringResource(R.string.screen_add_routine_group)
                    } else {
                        stringResource(R.string.screen_edit_routine_group)
                    }
                )
            },
            navigationIcon = { OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.label_routine_group_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_enable_after_saving), modifier = Modifier.weight(1f))
                Switch(checked = state.enabled, onCheckedChange = viewModel::updateEnabled)
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
                        stringResource(R.string.action_save_routine_group)
                    }
                )
            }
        }
    }
}
