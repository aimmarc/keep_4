package com.example.myapplication.services
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapplication.R

class NotificationService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "foreground_service_channel")
            .setContentTitle("Your App Name")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)  // 使通知无法被用户手动移除
            .build()
        startForeground(1, notification)
        // 如果服务被杀死后不需要自动重启，返回 START_NOT_STICKY
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}