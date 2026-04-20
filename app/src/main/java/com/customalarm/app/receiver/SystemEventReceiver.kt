package com.customalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.customalarm.app.appContainer
import kotlinx.coroutines.launch

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = context.appContainer
        container.applicationScope.launch {
            container.alarmCoordinator.rebuildSchedules()
            pendingResult.finish()
        }
    }
}

