package io.github.aimmarc.keep4.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import io.github.aimmarc.keep4.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Keep4ForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var audioManager: AudioManager
    private lateinit var audioEngine: AudioRouteEngine
    private lateinit var settingsRepository: SettingsRepository
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioEngine = AudioRouteEngine(this)
        settingsRepository = SettingsRepository(this)
        Keep4Notification.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                settingsRepository.setEnabled(false)
                stopServiceCleanly()
            }
            return START_NOT_STICKY
        }

        if (!promoteToForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent == null) {
            scope.launch {
                if (settingsRepository.settings.first().enabled) startMonitoring()
                else stopServiceCleanly()
            }
        } else {
            startMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        audioEngine.close()
        ServiceRuntime.update(ServiceMode.STOPPED)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground(): Boolean {
        val snapshot = ServiceSnapshot(ServiceMode.MONITORING)
        return try {
            val notification = Keep4Notification.serviceNotification(this, snapshot)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    Keep4Notification.SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                )
            } else {
                startForeground(Keep4Notification.SERVICE_NOTIFICATION_ID, notification)
            }
            ServiceRuntime.update(ServiceMode.MONITORING)
            Keep4Notification.cancelRecovery(this)
            true
        } catch (error: Exception) {
            ServiceRuntime.update(ServiceMode.ERROR, error.message ?: "无法启动前台服务")
            Keep4Notification.showRecovery(this, "点击打开 Keep4 完成后台服务恢复")
            false
        }
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            var lastSnapshot: ServiceSnapshot? = null
            while (isActive) {
                val snapshot = audioEngine.synchronize(audioManager.isMusicActive)
                if (snapshot != lastSnapshot) {
                    ServiceRuntime.update(snapshot.mode, snapshot.detail)
                    val notification = Keep4Notification.serviceNotification(this@Keep4ForegroundService, snapshot)
                    getSystemService(android.app.NotificationManager::class.java)
                        .notify(Keep4Notification.SERVICE_NOTIFICATION_ID, notification)
                    lastSnapshot = snapshot
                }
                delay(if (snapshot.mode == ServiceMode.ERROR) ERROR_RETRY_INTERVAL_MS else POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopServiceCleanly() {
        monitorJob?.cancel()
        monitorJob = null
        audioEngine.close()
        ServiceRuntime.update(ServiceMode.STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "io.github.aimmarc.keep4.action.START_KEEP4"
        const val ACTION_STOP = "io.github.aimmarc.keep4.action.STOP_KEEP4"
        private const val POLL_INTERVAL_MS = 1_000L
        private const val ERROR_RETRY_INTERVAL_MS = 5_000L

        fun start(context: Context) {
            val intent = Intent(context, Keep4ForegroundService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, Keep4ForegroundService::class.java))
            ServiceRuntime.update(ServiceMode.STOPPED)
        }
    }
}
