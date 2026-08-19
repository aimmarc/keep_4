package io.github.aimmarc.keep4.accessibility

import android.accessibilityservice.AccessibilityService
import android.Manifest
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import io.github.aimmarc.keep4.data.SettingsRepository
import io.github.aimmarc.keep4.service.Keep4ForegroundService
import io.github.aimmarc.keep4.service.Keep4Notification
import io.github.aimmarc.keep4.service.ServiceRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Keep4AccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()
        ServiceRuntime.setAccessibilityConnected(true)
        scope.launch {
            val enabled = SettingsRepository(this@Keep4AccessibilityService).settings.first().enabled
            val hasPermission = ContextCompat.checkSelfPermission(
                this@Keep4AccessibilityService,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (enabled && hasPermission) {
                runCatching { Keep4ForegroundService.start(this@Keep4AccessibilityService) }
                    .onFailure { Keep4Notification.showRecovery(this@Keep4AccessibilityService) }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        ServiceRuntime.setAccessibilityConnected(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ServiceRuntime.setAccessibilityConnected(false)
        scope.cancel()
        super.onDestroy()
    }
}
