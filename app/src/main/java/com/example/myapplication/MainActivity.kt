package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapp.utils.NotificationHelper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private lateinit var scheduler: ScheduledExecutorService
    private lateinit var task: Runnable
    private var processStatus = "0"
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 设置全屏
        setContentView(R.layout.settings_activity);

        // 启用 JavaScript
        webviewInit();

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }

        processInit()
        setStatusBar()
    }

    // 设置statusBar图标颜色为深色
    fun setStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    // 初始化webview
    fun webviewInit() {
        val webView: WebView = findViewById(R.id.webView)
        // 启用 JavaScript
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        // 加载本地 HTML 文件
        webView.loadUrl("file:///android_asset/keep4_views/index.html")
        // 设置 WebViewClient 以确保链接在 WebView 中打开
        webView.webViewClient = WebViewClient()
        // 设置 WebChromeClient 以支持更多的 Web 功能
        webView.webChromeClient = WebChromeClient()
        // 添加 JavaScript 接口
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidFunction")
    }

    // 初始化通知
    fun notifyInit() {
        notificationHelper = NotificationHelper(this)

        // 检查并请求通知权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            // 权限已授予，显示通知
            showNotify()
        }
    }

    // 展示通知
    fun showNotify() {
        notificationHelper.showNotification("Keep4", "应用后台保活服务")
    }

    // 初始化任务
    fun processInit() {
        Log.d("app", "onCreate: 创建")
        scheduler = Executors.newScheduledThreadPool(1)
        task = Runnable {
            Log.d("app", "轮询执行")
            try {
                if (processStatus == "1") {
                    if (!isRecording && isAudioPlaying()) {
                        // 有音频在播放且未录音，开始录音
                        startRecording()
                        Log.d("app", "开始录音")
                        runOnUiThread {
                            Toast.makeText(this, "开始录音", Toast.LENGTH_LONG).show()
                        }
                    }
                    if (isRecording && !isAudioPlaying()) {
                        // 没有音频在播放且在录音中，停止录音
                        stopRecording()
                        Log.d("app", "停止录音")
                        runOnUiThread {
                            Toast.makeText(this, "停止录音", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (err: Exception) {
                err.printStackTrace()
            }
        }

        processStart();
    }

    /**
     * 开启任务
     */
    fun processStart() {
        // 间隔1s检查一下系统音频播放
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS)
    }

    fun isAudioPlaying(): Boolean {
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMusicActive
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isRecording) {
                stopRecording()
            }
            scheduler.shutdown()
        } catch (e: Exception) {
            Log.d("app", "onDestroy error")
        }
    }

    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(getExternalFilesDir(null)?.absolutePath + "/recording.3gp")

            try {
                prepare()
                start()
                isRecording = true;
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder.apply {
                stop()
                release()
                isRecording = false;
            }
        } catch (e: Exception) {
            Log.d("app", "stop recording error", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 权限被授予
            if (!isRecording && isAudioPlaying()) {
                startRecording()
            }
            notifyInit()
        }
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 权限被授予
                showNotify()
            } else {
                // 权限被拒绝
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWebPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (err: Exception) {
            // todo
        }
    }

    inner class WebAppInterface(private val activity: MainActivity) {

        @JavascriptInterface
        fun setAppBackground(status: String) {
            // 在这里可以调用 Kotlin 方法，比如显示 Toast
            // 设置标志，隐藏最近任务中的应用
            // 设置Activity在最近任务中隐藏
            Toast.makeText(this@MainActivity, "默认隐藏，切换功能还未实现", Toast.LENGTH_LONG).show()
        }

        @JavascriptInterface
        fun setLoudspeaker(status: String) {
            // 在这里可以调用 Kotlin 方法，比如显示 Toast
            // 设置标志，隐藏最近任务中的应用
            // 设置Activity在最近任务中隐藏
            Log.d("app", "进入bridge")
            try {
                if (status == "0") {
                    // 关闭
                    // this@MainActivity.processStop()
                    this@MainActivity.processStatus = "0"
                    if (this@MainActivity.isRecording) {
                        this@MainActivity.stopRecording()
                    }
                    Toast.makeText(this@MainActivity, "已关闭四扬", Toast.LENGTH_LONG).show()
                }
                if (status == "1") {
                    // 开启
                    // this@MainActivity.processStart()
                    this@MainActivity.processStatus = "1"
                    Toast.makeText(this@MainActivity, "已开启四扬", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "出错啦~~", Toast.LENGTH_LONG).show()
                Log.d("app", "setLoudspeaker 错误", e)
            }
        }

        @JavascriptInterface
        fun openWebUrl(url: String) {
            this@MainActivity.openWebPage(url)
        }
    }
}
