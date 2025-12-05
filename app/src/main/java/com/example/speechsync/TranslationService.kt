package com.example.speechsync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class TranslationResponse(
    val recognizedText: String,
    val translatedText: String,
    val audioUrl: String
)

class TranslationService {
    private val client = OkHttpClient()
    private val baseUrl = "https://legends-sherman-licenses-bare.trycloudflare.com"

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
                throw IOException("Server error: ${response.code}")
            }

            val json = JSONObject(response.body?.string() ?: "")
            val audioUrl = "$baseUrl${json.getString("audio_url")}"

            Result.success(
                TranslationResponse(
                    recognizedText = json.getString("recognized_text"),
                    translatedText = json.getString("translated_text"),
                    audioUrl = audioUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}