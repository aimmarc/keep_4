package io.github.aimmarc.keep4.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.github.aimmarc.keep4.data.SettingsRepository
import io.github.aimmarc.keep4.service.Keep4ForegroundService
import io.github.aimmarc.keep4.service.Keep4Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settings.first()
                val hasMicrophonePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                when (
                    BootStartPolicy.decide(
                        enabled = settings.enabled,
                        startOnBoot = settings.startOnBoot,
                        hasMicrophonePermission = hasMicrophonePermission,
                        sdkInt = android.os.Build.VERSION.SDK_INT,
                    )
                ) {
                    BootAction.IGNORE -> Unit
                    BootAction.SHOW_RECOVERY_NOTIFICATION -> {
                        val message = if (hasMicrophonePermission) {
                            "点击恢复四扬声器后台服务"
                        } else {
                            "需要麦克风权限才能恢复 Keep4"
                        }
                        Keep4Notification.showRecovery(context, message)
                    }
                    BootAction.START_SERVICE -> {
                        runCatching { Keep4ForegroundService.start(context) }
                            .onFailure { Keep4Notification.showRecovery(context) }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
