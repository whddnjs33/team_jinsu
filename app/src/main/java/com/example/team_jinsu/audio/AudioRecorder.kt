package com.example.team_jinsu.audio

import android.media.MediaRecorder
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.team_jinsu.R
import kotlinx.coroutines.CoroutineScope
import java.io.File
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.team_jinsu.network.ApiService

class MainActivity : AppCompatActivity() {

    private var recorder: MediaRecorder? = null
    private lateinit var outputFile: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // layout 설정

        // 파일 저장 경로 지정
        outputFile = "${externalCacheDir?.absolutePath}/recorded_audio.3gp" // 확장자 수정

        val recordButton = findViewById<Button>(R.id.record_button)

        recordButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    sendAudioToServer() // 이 함수 구현 필요
                    true
                }

                else -> false
            }
        }
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // WAV 미지원. 추후 변환 필요
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    private fun sendAudioToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(outputFile)
                val requestFile = RequestBody.create("audio/3gp".toMediaTypeOrNull(), file)
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // Retrofit 객체 생성
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8000/") // 에뮬레이터에서 로컬 서버 접근용
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