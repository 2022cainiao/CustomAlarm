package com.customalarm.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.customalarm.app.appContainer
import com.customalarm.app.domain.AlarmRingingController
import com.customalarm.app.ui.ringing.RingingActivity
import kotlinx.coroutines.launch

class AlarmPlaybackService : Service() {
    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentAlarmId: Long = -1L
    private var currentSnoozeMinutes: Int = 10
    private var canSnooze: Boolean = true
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val ringingStateStore: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START -> handleStart(intent)
                ACTION_DISMISS -> stopAlarm()
                ACTION_SNOOZE -> {
                    val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                    val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, currentSnoozeMinutes)
                    if (alarmId > 0L && canSnooze) {
                        appContainer.applicationScope.launch {
                            appContainer.alarmCoordinator.snoozeAlarm(alarmId, snoozeMinutes)
                        }
                    }
                    stopAlarm()
                }
                else -> restorePersistedAlarmIfNeeded()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Alarm playback command failed", exception)
            stopAlarm()
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return
        try {
            currentAlarmId = alarmId
            currentSnoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
            canSnooze = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
            val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
            val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)
            val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
            persistRingingState(
                alarmId = alarmId,
                label = label,
                ringtoneUri = ringtoneUri,
                vibrate = vibrate,
                snoozeEnabled = canSnooze,
                snoozeMinutes = currentSnoozeMinutes
            )

            startForeground(
                AlarmRingingController.NOTIFICATION_ID_BASE + alarmId.toInt(),
                appContainer.alarmRingingController.createNotification(
                    alarmId = alarmId,
                    label = label,
                    snoozeEnabled = canSnooze,
                    snoozeMinutes = currentSnoozeMinutes
                )
            )
            playRingtone(ringtoneUri)
            if (vibrate) {
                startVibration()
            }
            startActivity(
                RingingActivity.createIntent(
                    context = this,
                    alarmId = alarmId,
                    label = label,
                    snoozeEnabled = canSnooze,
                    snoozeMinutes = currentSnoozeMinutes
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to start alarm playback for $alarmId", exception)
            stopAlarm()
        }
    }

    private fun playRingtone(ringtoneUri: String?) {
        stopPlayback()
        requestAudioFocus()
        val primaryUri = ringtoneUri?.let(Uri::parse) ?: appContainer.alarmRingingController.defaultAlarmUri()
        val fallbackUri = appContainer.alarmRingingController.defaultAlarmUri()

        if (primaryUri != null && tryPlayWithMediaPlayer(primaryUri)) {
            return
        }
        if (fallbackUri != null && fallbackUri != primaryUri && tryPlayWithMediaPlayer(fallbackUri)) {
            return
        }

        val finalUri = fallbackUri ?: primaryUri ?: return
        ringtone = RingtoneManager.getRingtone(this, finalUri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                audioAttributes = alarmAudioAttributes()
                isLooping = true
            }
            play()
        }
    }

    private fun tryPlayWithMediaPlayer(uri: Uri): Boolean {
        return runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(alarmAudioAttributes())
                setWakeMode(this@AlarmPlaybackService, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(this@AlarmPlaybackService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            Log.w(TAG, "Failed to play ringtone via MediaPlayer: $uri", it)
        }.isSuccess
    }

    private fun alarmAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun requestAudioFocus() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(alarmAudioAttributes())
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(manager::abandonAudioFocusRequest)
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
        audioManager = null
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 300), 0)
        }
    }

    private fun stopAlarm() {
        clearPersistedRingingState()
        stopPlayback()
        sendBroadcast(Intent(AlarmRingingController.ACTION_FINISH_RINGING).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w(TAG, "Task removed while alarm service is active, keeping foreground playback alive")
        super.onTaskRemoved(rootIntent)
    }

    private fun stopPlayback() {
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        abandonAudioFocus()
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val PREFS_NAME = "alarm_playback_service"
        private const val ACTION_START = "com.customalarm.app.action.START_ALARM"
        private const val ACTION_DISMISS = "com.customalarm.app.action.DISMISS_ALARM"
        private const val ACTION_SNOOZE = "com.customalarm.app.action.SNOOZE_ALARM"
        private const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val EXTRA_LABEL = "extra_label"
        private const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        private const val EXTRA_VIBRATE = "extra_vibrate"
        private const val EXTRA_SNOOZE_ENABLED = "extra_snooze_enabled"
        private const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        private const val TAG = "AlarmPlaybackService"
        private const val KEY_ALARM_ID = "alarm_id"
        private const val KEY_LABEL = "label"
        private const val KEY_RINGTONE_URI = "ringtone_uri"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_SNOOZE_ENABLED = "snooze_enabled"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"

        fun createStartIntent(
            context: Context,
            alarmId: Long,
            label: String,
            ringtoneUri: String?,
            vibrate: Boolean,
            snoozeEnabled: Boolean,
            snoozeMinutes: Int
        ) = Intent(context, AlarmPlaybackService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_SNOOZE_ENABLED, snoozeEnabled)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }

        fun createDismissIntent(context: Context, alarmId: Long) =
            Intent(context, AlarmPlaybackService::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_ALARM_ID, alarmId)
            }

        fun createSnoozeIntent(context: Context, alarmId: Long, snoozeMinutes: Int) =
            Intent(context, AlarmPlaybackService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
    }

    private fun restorePersistedAlarmIfNeeded() {
        val alarmId = ringingStateStore.getLong(KEY_ALARM_ID, -1L)
        if (alarmId <= 0L) {
            stopSelf()
            return
        }
        handleStart(
            createStartIntent(
                context = this,
                alarmId = alarmId,
                label = ringingStateStore.getString(KEY_LABEL, "").orEmpty(),
                ringtoneUri = ringingStateStore.getString(KEY_RINGTONE_URI, null),
                vibrate = ringingStateStore.getBoolean(KEY_VIBRATE, true),
                snoozeEnabled = ringingStateStore.getBoolean(KEY_SNOOZE_ENABLED, true),
                snoozeMinutes = ringingStateStore.getInt(KEY_SNOOZE_MINUTES, 10)
            )
        )
    }

    private fun persistRingingState(
        alarmId: Long,
        label: String,
        ringtoneUri: String?,
        vibrate: Boolean,
        snoozeEnabled: Boolean,
        snoozeMinutes: Int
    ) {
        ringingStateStore.edit()
            .putLong(KEY_ALARM_ID, alarmId)
            .putString(KEY_LABEL, label)
            .putString(KEY_RINGTONE_URI, ringtoneUri)
            .putBoolean(KEY_VIBRATE, vibrate)
            .putBoolean(KEY_SNOOZE_ENABLED, snoozeEnabled)
            .putInt(KEY_SNOOZE_MINUTES, snoozeMinutes)
            .apply()
    }

    private fun clearPersistedRingingState() {
        ringingStateStore.edit().clear().apply()
    }
}
