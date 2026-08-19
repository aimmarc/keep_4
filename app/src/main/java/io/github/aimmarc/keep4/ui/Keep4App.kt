package io.github.aimmarc.keep4.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.aimmarc.keep4.BuildConfig
import io.github.aimmarc.keep4.Keep4ViewModel
import io.github.aimmarc.keep4.R
import io.github.aimmarc.keep4.data.AppSettings
import io.github.aimmarc.keep4.service.ServiceMode
import io.github.aimmarc.keep4.service.ServiceSnapshot
import io.github.aimmarc.keep4.shizuku.ShizukuSnapshot
import io.github.aimmarc.keep4.shizuku.ShizukuStatus

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val SHIZUKU = "settings/shizuku"
    const val ABOUT = "settings/about"
}

@Composable
fun Keep4App(
    viewModel: Keep4ViewModel,
    hasMicrophonePermission: Boolean,
    hasNotificationPermission: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onHideFromRecentsChanged: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenShizuku: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val serviceSnapshot by viewModel.serviceSnapshot.collectAsStateWithLifecycle()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()
    val shizukuSnapshot by viewModel.shizukuSnapshot.collectAsStateWithLifecycle()

    LaunchedEffect(settings.hideFromRecents) {
        onHideFromRecentsChanged(settings.hideFromRecents)
    }

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                settings = settings,
                serviceSnapshot = serviceSnapshot,
                hasMicrophonePermission = hasMicrophonePermission,
                hasNotificationPermission = hasNotificationPermission,
                onSetEnabled = onSetEnabled,
                onRequestMicrophonePermission = onRequestMicrophonePermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                settings = settings,
                accessibilityEnabled = accessibilityEnabled,
                shizukuSnapshot = shizukuSnapshot,
                onSetStartOnBoot = viewModel::setStartOnBoot,
                onSetHideFromRecents = viewModel::setHideFromRecents,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenShizukuSettings = { navController.navigate(Routes.SHIZUKU) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }
        composable(Routes.SHIZUKU) {
            ShizukuScreen(
                navController = navController,
                snapshot = shizukuSnapshot,
                onOpenShizuku = onOpenShizuku,
                onRequestPermission = viewModel::requestShizukuPermission,
                onApplyOptimization = viewModel::applyShizukuOptimization,
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(
                navController = navController,
                onOpenUrl = onOpenUrl,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    settings: AppSettings,
    serviceSnapshot: ServiceSnapshot,
    hasMicrophonePermission: Boolean,
    hasNotificationPermission: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keep4", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StatusPanel(serviceSnapshot) }
            item {
                SettingRow(
                    icon = Icons.Outlined.VolumeUp,
                    title = "开启四扬声器",
                    supportingText = if (settings.enabled) "后台服务已启用" else "后台服务已停止",
                    trailing = {
                        Switch(checked = settings.enabled, onCheckedChange = null)
                    },
                    onClick = { onSetEnabled(!settings.enabled) },
                )
            }
            item {
                SectionTitle("权限")
                PermissionRow(
                    icon = Icons.Outlined.Mic,
                    title = "麦克风",
                    granted = hasMicrophonePermission,
                    onRequest = onRequestMicrophonePermission,
                )
                Divider()
                PermissionRow(
                    icon = Icons.Outlined.Notifications,
                    title = "常驻通知",
                    granted = hasNotificationPermission,
                    onRequest = onRequestNotificationPermission,
                )
            }
        }
    }
}

@Composable
private fun StatusPanel(snapshot: ServiceSnapshot) {
    val presentation = when (snapshot.mode) {
        ServiceMode.RECORDING -> StatusPresentation(
            "四扬声器已启用",
            "检测到媒体播放，录音路由正在工作",
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Outlined.VolumeUp,
        )
        ServiceMode.MONITORING -> StatusPresentation(
            "正在后台监听",
            snapshot.detail ?: "常驻通知已启用，等待媒体播放",
            MaterialTheme.colorScheme.secondaryContainer,
            Icons.Outlined.CheckCircle,
        )
        ServiceMode.ERROR -> StatusPresentation(
            "需要处理",
            snapshot.detail ?: "后台服务运行异常",
            MaterialTheme.colorScheme.errorContainer,
            Icons.Outlined.ErrorOutline,
        )
        ServiceMode.STOPPED -> StatusPresentation(
            "服务未运行",
            "打开总开关后开始监听",
            MaterialTheme.colorScheme.surfaceVariant,
            Icons.Outlined.PowerSettingsNew,
        )
    }
    StatusSurface(presentation)
}

@Composable
private fun StatusSurface(presentation: StatusPresentation) {
    Surface(color = presentation.color, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(presentation.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    presentation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(presentation.detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private data class StatusPresentation(
    val title: String,
    val detail: String,
    val color: Color,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    navController: NavHostController,
    settings: AppSettings,
    accessibilityEnabled: Boolean,
    shizukuSnapshot: ShizukuSnapshot,
    onSetStartOnBoot: (Boolean) -> Unit,
    onSetHideFromRecents: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenShizukuSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    SecondaryScaffold(title = "设置", onBack = { navController.popBackStack() }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item { SectionTitle("运行") }
            item {
                SettingRow(
                    icon = Icons.Outlined.PowerSettingsNew,
                    title = "开机恢复",
                    supportingText = "重启后自动恢复，受限时改为通知提醒",
                    trailing = { Switch(settings.startOnBoot, onCheckedChange = null) },
                    onClick = { onSetStartOnBoot(!settings.startOnBoot) },
                )
            }
            item { Divider(modifier = Modifier.padding(start = 72.dp)) }
            item {
                SettingRow(
                    icon = Icons.Outlined.VisibilityOff,
                    title = "隐藏最近任务",
                    supportingText = "隐藏界面任务，不停止后台服务",
                    trailing = { Switch(settings.hideFromRecents, onCheckedChange = null) },
                    onClick = { onSetHideFromRecents(!settings.hideFromRecents) },
                )
            }
            item { Spacer(Modifier.height(16.dp)); SectionTitle("增强能力") }
            item {
                NavigationRow(
                    icon = Icons.Outlined.AccessibilityNew,
                    title = "无障碍辅助",
                    supportingText = if (accessibilityEnabled) "已开启" else "未开启",
                    onClick = onOpenAccessibilitySettings,
                )
            }
            item { Divider(modifier = Modifier.padding(start = 72.dp)) }
            item {
                NavigationRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = "Shizuku",
                    supportingText = when (shizukuSnapshot.status) {
                        ShizukuStatus.READY -> "已授权"
                        ShizukuStatus.PERMISSION_REQUIRED -> "等待授权"
                        ShizukuStatus.UNAVAILABLE -> "未运行"
                    },
                    onClick = onOpenShizukuSettings,
                )
            }
            item { Spacer(Modifier.height(16.dp)); SectionTitle("项目") }
            item {
                NavigationRow(
                    icon = Icons.Outlined.Info,
                    title = "关于与声明",
                    supportingText = "版本、隐私、免责声明与开源许可",
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(
    navController: NavHostController,
    onOpenUrl: (String) -> Unit,
) {
    SecondaryScaffold(title = "关于与声明", onBack = { navController.popBackStack() }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = MaterialTheme.shapes.large,
                        color = Color(0xFF105D43),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text("Keep4", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "版本 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "面向联想小新 Pad Pro 12.7 一代第三方 ROM 的开源四扬声器工具。",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { SectionTitle("开源项目") }
            item {
                NavigationRow(
                    icon = Icons.Outlined.Code,
                    title = "源代码与贡献",
                    supportingText = "MIT License · aimmarc and contributors",
                    onClick = { onOpenUrl(PROJECT_URL) },
                )
            }
            item { Spacer(Modifier.height(16.dp)); SectionTitle("政策与声明") }
            item {
                NavigationRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "隐私政策",
                    supportingText = "离线运行，不上传数据",
                    onClick = { onOpenUrl(PRIVACY_URL) },
                )
            }
            item { Divider(modifier = Modifier.padding(start = 72.dp)) }
            item {
                NavigationRow(
                    icon = Icons.Outlined.Gavel,
                    title = "免责声明",
                    supportingText = "设备兼容性、使用风险与责任边界",
                    onClick = { onOpenUrl(DISCLAIMER_URL) },
                )
            }
            item { Divider(modifier = Modifier.padding(start = 72.dp)) }
            item {
                NavigationRow(
                    icon = Icons.Outlined.Code,
                    title = "第三方开源许可",
                    supportingText = "AndroidX、Compose、Shizuku 等",
                    onClick = { onOpenUrl(THIRD_PARTY_URL) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShizukuScreen(
    navController: NavHostController,
    snapshot: ShizukuSnapshot,
    onOpenShizuku: () -> Unit,
    onRequestPermission: () -> Unit,
    onApplyOptimization: () -> Unit,
) {
    SecondaryScaffold(title = "Shizuku", onBack = { navController.popBackStack() }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StatusSurface(
                when (snapshot.status) {
                    ShizukuStatus.READY -> StatusPresentation(
                        "Shizuku 已授权",
                        "可以应用后台运行优化",
                        MaterialTheme.colorScheme.primaryContainer,
                        Icons.Outlined.CheckCircle,
                    )
                    ShizukuStatus.PERMISSION_REQUIRED -> StatusPresentation(
                        "等待授权",
                        "需要在 Shizuku 中确认授权",
                        MaterialTheme.colorScheme.secondaryContainer,
                        Icons.Outlined.AdminPanelSettings,
                    )
                    ShizukuStatus.UNAVAILABLE -> StatusPresentation(
                        "Shizuku 未运行",
                        "启动 Shizuku 后返回此页面",
                        MaterialTheme.colorScheme.errorContainer,
                        Icons.Outlined.ErrorOutline,
                    )
                },
            )
            Text(
                "授权后可申请系统待机白名单和后台运行 AppOps。不同 ROM 可能只支持其中一部分。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (snapshot.status) {
                ShizukuStatus.UNAVAILABLE -> Button(onClick = onOpenShizuku) { Text("打开 Shizuku") }
                ShizukuStatus.PERMISSION_REQUIRED -> Button(onClick = onRequestPermission) { Text("请求授权") }
                ShizukuStatus.READY -> Button(
                    onClick = onApplyOptimization,
                    enabled = !snapshot.working,
                ) { Text(if (snapshot.working) "正在应用" else "应用后台优化") }
            }
            snapshot.result?.let { result ->
                Text(result, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(supportingText) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    onClick: () -> Unit,
) {
    SettingRow(
        icon = icon,
        title = title,
        supportingText = supportingText,
        trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(if (granted) "已授权" else "需要授权") },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            if (granted) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "已授权",
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                FilledTonalButton(onClick = onRequest) { Text("授权") }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private const val PROJECT_URL = "https://github.com/aimmarc/keep_4"
private const val PRIVACY_URL = "$PROJECT_URL/blob/main/PRIVACY.md"
private const val DISCLAIMER_URL = "$PROJECT_URL/blob/main/DISCLAIMER.md"
private const val THIRD_PARTY_URL = "$PROJECT_URL/blob/main/THIRD_PARTY_NOTICES.md"
