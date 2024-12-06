package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false
    private lateinit var scheduler: ScheduledExecutorService
    private lateinit var task: Runnable
    private var processStatus = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 设置全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.settings_activity);

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

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }
        if (isAudioPlaying()) {
            startRecording()
        }

        processInit()
    }

    // 初始化任务
    fun processInit() {
        Log.d("app", "onCreate: 创建")
        scheduler = Executors.newScheduledThreadPool(1)
        task = Runnable {
            Log.d("app", "轮询执行")
            try {
                if (processStatus.equals("1")) {
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

    /**
     * 停止任务
     */
//    fun processStop() {
//        scheduler.shutdown()
//        if (isRecording) {
//            stopRecording();
//        }
//    }

    fun isAudioPlaying(): Boolean {
        val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMusicActive
    }

    override fun onDestroy() {
        super.onDestroy();
        stopRecording();
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
        mediaRecorder.apply {
            stop()
            release()
            isRecording = false;
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
                startRecording();
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
//        // 检查是否有应用程序可以处理这个Intent
//        if (intent.resolveActivity(packageManager) != null) {
//            startActivity(chooser)
//        } else {
//            // 处理没有浏览器的情况
//            // 例如，显示一个Toast或者弹出一个对话框通知用户
//            Toast.makeText(this@MainActivity, "没有可执行此操作的APP", Toast.LENGTH_LONG).show()
//        }
    }

    inner class WebAppInterface(private val activity: MainActivity) {

        @JavascriptInterface
        fun setAppBackground(status: String) {
            // 在这里可以调用 Kotlin 方法，比如显示 Toast
            // 设置标志，隐藏最近任务中的应用
            // 设置Activity在最近任务中隐藏
            Toast.makeText(this@MainActivity, "默认隐藏，切换功能还未实现", Toast.LENGTH_LONG).show()
//            if (status.equals("0")) {
//                // 关闭
//                val intent = Intent(this@MainActivity, activity::class.java)
//                startActivity(intent)
//            }
//            if (status.equals("1")) {
//                // 开启
//                val intent = Intent(this@MainActivity, activity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//                startActivity(intent)
//            }
        }

        @JavascriptInterface
        fun setLoudspeaker(status: String) {
            // 在这里可以调用 Kotlin 方法，比如显示 Toast
            // 设置标志，隐藏最近任务中的应用
            // 设置Activity在最近任务中隐藏
            Log.d("app", "进入bridge")

            if (status.equals("0")) {
                // 关闭
                // this@MainActivity.processStop()
                this@MainActivity.processStatus = "0"
                if (this@MainActivity.isRecording) {
                    this@MainActivity.stopRecording()
                }
                Toast.makeText(this@MainActivity, "已关闭四扬", Toast.LENGTH_LONG).show()
            }
            if (status.equals("1")) {
                // 开启
                // this@MainActivity.processStart()
                this@MainActivity.processStatus = "1"
                Toast.makeText(this@MainActivity, "已开启四扬", Toast.LENGTH_LONG).show()
            }
        }

        @JavascriptInterface
        fun openWebUrl(url: String) {
            this@MainActivity.openWebPage(url)
        }
    }
}
