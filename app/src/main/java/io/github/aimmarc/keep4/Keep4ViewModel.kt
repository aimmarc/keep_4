package io.github.aimmarc.keep4

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.aimmarc.keep4.accessibility.Keep4AccessibilityService
import io.github.aimmarc.keep4.data.AppSettings
import io.github.aimmarc.keep4.data.SettingsRepository
import io.github.aimmarc.keep4.service.Keep4ForegroundService
import io.github.aimmarc.keep4.service.ServiceMode
import io.github.aimmarc.keep4.service.ServiceRuntime
import io.github.aimmarc.keep4.shizuku.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Keep4ViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val shizukuManager = ShizukuManager(application)

    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )
    val serviceSnapshot = ServiceRuntime.snapshot
    val shizukuSnapshot = shizukuManager.snapshot

    private val mutableAccessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled = mutableAccessibilityEnabled.asStateFlow()

    fun setEnabled(context: Context, enabled: Boolean) {
        if (enabled) {
            runCatching { Keep4ForegroundService.start(context) }
                .onFailure { reportPermissionError(it.message ?: "无法启动后台服务") }
        } else {
            Keep4ForegroundService.stop(context)
        }
        viewModelScope.launch {
            settingsRepository.setEnabled(enabled)
        }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setStartOnBoot(enabled) }
    }

    fun setHideFromRecents(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHideFromRecents(enabled) }
    }

    fun refreshCapabilities(context: Context) {
        mutableAccessibilityEnabled.value = isAccessibilityServiceEnabled(context)
        shizukuManager.refresh()
    }

    suspend fun reconcileService(context: Context) {
        val currentSettings = settingsRepository.settings.first()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (currentSettings.enabled && hasPermission && serviceSnapshot.value.mode == ServiceMode.STOPPED) {
            runCatching { Keep4ForegroundService.start(context) }
                .onFailure { reportPermissionError(it.message ?: "无法恢复后台服务") }
        }
    }

    fun requestShizukuPermission() = shizukuManager.requestPermission()

    fun applyShizukuOptimization() {
        viewModelScope.launch { shizukuManager.applyBackgroundOptimization() }
    }

    fun setShizukuMessage(message: String) {
        shizukuManager.setMessage(message)
    }

    fun reportPermissionError(message: String) {
        ServiceRuntime.update(ServiceMode.ERROR, message)
    }

    override fun onCleared() {
        shizukuManager.close()
        super.onCleared()
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(context, Keep4AccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { service ->
                val info = service.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == expected
            }
    }
}
