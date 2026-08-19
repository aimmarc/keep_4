package io.github.aimmarc.keep4.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.aimmarc.keep4.MainActivity
import io.github.aimmarc.keep4.R

object Keep4Notification {
    const val SERVICE_NOTIFICATION_ID = 4001
    private const val RECOVERY_NOTIFICATION_ID = 4002
    const val SERVICE_CHANNEL_ID = "keep4_service"
    private const val RECOVERY_CHANNEL_ID = "keep4_recovery"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Keep4 后台服务",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "显示四扬声器监听和录音路由状态"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                RECOVERY_CHANNEL_ID,
                "Keep4 恢复提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "设备重启后提醒恢复 Keep4" },
        )
    }

    fun serviceNotification(context: Context, snapshot: ServiceSnapshot): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, Keep4ForegroundService::class.java)
                .setAction(Keep4ForegroundService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val content = when (snapshot.mode) {
            ServiceMode.RECORDING -> "四扬声器路由已启用"
            ServiceMode.ERROR -> snapshot.detail ?: "服务运行异常"
            else -> "正在等待媒体播放"
        }
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Keep4 正在运行")
            .setContentText(content)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun showRecovery(context: Context, message: String = "点击恢复四扬声器后台服务") {
        createChannels(context)
        if (!canPostNotifications(context)) return
        val openIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_RECOVER_SERVICE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationManagerCompat.from(context).notify(
            RECOVERY_NOTIFICATION_ID,
            NotificationCompat.Builder(context, RECOVERY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("恢复 Keep4")
                .setContentText(message)
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun cancelRecovery(context: Context) {
        NotificationManagerCompat.from(context).cancel(RECOVERY_NOTIFICATION_ID)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
