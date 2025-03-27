package com.example.team_jinsu.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/upload-audio/")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part
    ): Response<AiResultResponse>
}

data class AiResultResponse(val result: String)