package io.github.aimmarc.keep4

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.github.aimmarc.keep4.service.Keep4Notification
import io.github.aimmarc.keep4.ui.Keep4App
import io.github.aimmarc.keep4.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<Keep4ViewModel>()
    private var startAfterPermission = false
    private var permissionRevision by mutableIntStateOf(0)

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionRevision++
        if (granted) {
            val shouldStart = startAfterPermission
            startAfterPermission = false
            if (shouldStart) startKeep4()
        } else {
            startAfterPermission = false
            viewModel.reportPermissionError("需要麦克风权限才能启用四扬声器")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionRevision++
        viewModel.refreshCapabilities(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            @Suppress("UNUSED_EXPRESSION")
            permissionRevision
            MyApplicationTheme {
                Keep4App(
                    viewModel = viewModel,
                    hasMicrophonePermission = hasPermission(Manifest.permission.RECORD_AUDIO),
                    hasNotificationPermission = hasNotificationPermission(),
                    onSetEnabled = ::setKeep4Enabled,
                    onRequestMicrophonePermission = ::requestMicrophonePermission,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onHideFromRecentsChanged = ::setExcludedFromRecents,
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onOpenShizuku = ::openShizuku,
                    onOpenUrl = ::openUrl,
                )
            }
        }
        if (intent?.action == ACTION_RECOVER_SERVICE) setKeep4Enabled(true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_RECOVER_SERVICE) setKeep4Enabled(true)
    }

    override fun onResume() {
        super.onResume()
        permissionRevision++
        viewModel.refreshCapabilities(this)
        lifecycleScope.launch { viewModel.reconcileService(this@MainActivity) }
    }

    private fun setKeep4Enabled(enabled: Boolean) {
        if (!enabled) {
            viewModel.setEnabled(this, false)
            return
        }
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            startAfterPermission = true
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        startKeep4()
    }

    private fun startKeep4() {
        viewModel.setEnabled(this, true)
        Keep4Notification.cancelRecovery(this)
        requestNotificationPermission()
    }

    private fun requestMicrophonePermission() {
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun setExcludedFromRecents(excluded: Boolean) {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.appTasks
            .firstOrNull {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.taskInfo.taskId == taskId
                } else {
                    @Suppress("DEPRECATION")
                    it.taskInfo.id == taskId
                }
            }
            ?.setExcludeFromRecents(excluded)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openShizuku() {
        val launchIntent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launchIntent != null) startActivity(launchIntent)
        else viewModel.setShizukuMessage("未检测到 Shizuku，请先安装并启动 Shizuku")
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    companion object {
        const val ACTION_RECOVER_SERVICE = "io.github.aimmarc.keep4.action.RECOVER_KEEP4"
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
