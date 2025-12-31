package com.example.speechsync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TranslationResponse(
    val recognizedText: String,
    val translatedText: String,
    val audioUrl: String
)

class TranslationService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(110, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .callTimeout(210, TimeUnit.SECONDS)
        .build()

    // Place your backend server here:
    private val baseUrl = "URL HERE"

    suspend fun translate(
        audioFile: File,
        inputLang: String,
        targetLang: String
    ): Result<TranslationResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/*".toMediaType())
                )
                .addFormDataPart("input_lang", inputLang)
                .addFormDataPart("target_lang", targetLang)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/translate")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("Server error: ${response.code} - $errorBody")
            }

            val json = JSONObject(response.body?.string() ?: "")
            val audioUrl = "$baseUrl${json.getString("audio_url")}"

            Result.success(
                TranslationResponse(
                    recognizedText = json.optString("recognized_text", ""),
                    translatedText = json.getString("translated_text"),
                    audioUrl = audioUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}



