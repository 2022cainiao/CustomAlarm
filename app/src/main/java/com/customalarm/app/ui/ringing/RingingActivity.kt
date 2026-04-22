package com.customalarm.app.ui.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.customalarm.app.R
import com.customalarm.app.domain.AlarmRingingController
import com.customalarm.app.service.AlarmPlaybackService
import com.customalarm.app.ui.theme.CustomAlarmTheme
import com.customalarm.app.util.formatRingingClock

class RingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            CustomAlarmTheme {
                RingingRoute(
                    alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L),
                    label = intent.getStringExtra(EXTRA_LABEL).orEmpty(),
                    snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true),
                    snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10),
                    onDismiss = {
                        startService(AlarmPlaybackService.createDismissIntent(this, it))
                        finish()
                    },
                    onSnooze = { alarmId, minutes ->
                        startService(AlarmPlaybackService.createSnoozeIntent(this, alarmId, minutes))
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val EXTRA_LABEL = "extra_label"
        private const val EXTRA_SNOOZE_ENABLED = "extra_snooze_enabled"
        private const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

        fun createIntent(
            context: Context,
            alarmId: Long,
            label: String,
            snoozeEnabled: Boolean,
            snoozeMinutes: Int
        ) = Intent(context, RingingActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_SNOOZE_ENABLED, snoozeEnabled)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
    }
}

@Composable
private fun RingingRoute(
    alarmId: Long,
    label: String,
    snoozeEnabled: Boolean,
    snoozeMinutes: Int,
    onDismiss: (Long) -> Unit,
    onSnooze: (Long, Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                activity?.finish()
            }
        }
        val filter = IntentFilter(AlarmRingingController.ACTION_FINISH_RINGING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatRingingClock(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            label.ifBlank { stringResource(R.string.status_alarm_ringing) },
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onDismiss(alarmId) }) {
            Text(stringResource(R.string.action_dismiss))
        }
        if (snoozeEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onSnooze(alarmId, snoozeMinutes) }) {
                Text(stringResource(R.string.action_snooze_minutes, snoozeMinutes))
            }
        }
    }
}
