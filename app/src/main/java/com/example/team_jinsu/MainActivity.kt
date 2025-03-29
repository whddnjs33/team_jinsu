package com.example.team_jinsu

import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.team_jinsu.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class MainActivity : AppCompatActivity() {

    private var recorder: MediaRecorder? = null
    private lateinit var outputFile: String
    private var isRecording = false

    private fun checkPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val requestCode = 200

        if (permissions.any {
                checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }) {
            requestPermissions(permissions, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // activity_main.xml 사용

        //권한 요청
        checkPermissions()

        val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (musicDir == null) {
            Toast.makeText(this, "저장 경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        outputFile = "${musicDir.absolutePath}/recorded_audio.mp4" // 확장자도 변경 추천

        val recordButton = findViewById<Button>(R.id.record_button)

        recordButton.setOnClickListener {
            if (!isRecording) {
                startRecording()
                isRecording = true
                recordButton.text = "녹음 중지"
            } else {
                stopRecording()
                sendAudioToServer()
                isRecording = false
                recordButton.text = "녹음 시작"
            }
        }
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음 중지 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            recorder = null
        }
    }

    private fun sendAudioToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(outputFile)
                val requestFile = RequestBody.create("audio/3gp".toMediaTypeOrNull(), file)
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val retrofit = Retrofit.Builder()
                    .baseUrl("http://192.168.0.26:8000/") // <- 이건 본인 PC IP로 바꿔줘!
                    .client(OkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(ApiService::class.java)
                val response = apiService.uploadAudio(body)

                runOnUiThread {
                    if (response.isSuccessful) {
                        val resultText = response.body()?.result ?: "결과 없음"
                        Toast.makeText(this@MainActivity, "AI 결과: $resultText", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "서버 오류: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}