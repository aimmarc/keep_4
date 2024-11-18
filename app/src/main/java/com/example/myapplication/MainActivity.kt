package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.layout.Layout
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        setContent {
//            MyApplicationTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding),
//                    )
//                }
//            }
//        }
        setContentView(R.layout.settings_activity);
        setHasOptionsMenu(true)
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
        Log.d("app", "onCreate: 创建")
        val scheduler = Executors.newScheduledThreadPool(1)
        val task = Runnable {
            Log.d("app", "轮询执行")
            try {
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
            } catch (err: Exception) {
                err.printStackTrace()
            }
        }

        // 间隔1s检查一下系统音频播放
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS)
    }

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
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}

