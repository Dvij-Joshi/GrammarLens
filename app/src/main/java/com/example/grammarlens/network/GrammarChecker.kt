package com.example.grammarlens.network

import android.content.Context
import com.example.grammarlens.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GrammarChecker(private val context: Context) {

    private val gson = Gson()
    private var currentBaseUrl = ""
    private var apiService: GroqApiService? = null

    private fun getApiService(baseUrl: String): GroqApiService {
        if (apiService == null || currentBaseUrl != baseUrl) {
            val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val retrofit = Retrofit.Builder()
                .baseUrl(validUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiService = retrofit.create(GroqApiService::class.java)
            currentBaseUrl = validUrl
        }
        return apiService!!
    }

    suspend fun analyzeSentence(sentence: String): GrammarCheckResult? = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)
        val savedKey = sharedPrefs.getString("groq_api_key", "") ?: ""
        val apiKey = savedKey.ifEmpty { BuildConfig.GROQ_API_KEY }
        val apiUrl = sharedPrefs.getString("groq_api_url", "https://api.groq.com/openai/v1/") ?: "https://api.groq.com/openai/v1/"

        if (apiKey.isEmpty()) {
            return@withContext null
        }

        val prompt = """
            You are a strict, precise grammar checker.
            Analyze the following sentence for grammar, spelling, punctuation, and structure errors.
            Respond ONLY with a JSON object in this exact format, with no extra text:
            {
              "isGrammarCorrect": false,
              "mistakes": [
                {
                  "mistake": "exact part of the sentence that is wrong",
                  "category": "Tense / Spelling / Punctuation / Preposition / Article / Subject-Verb Agreement / Word Choice / Sentence Structure",
                  "suggestion": "how to fix it"
                }
              ],
              "correctedText": "The fully corrected sentence."
            }
            If there are absolutely no mistakes, return isGrammarCorrect as true, empty mistakes array, and the original text as correctedText.

            Sentence to check: "$sentence"
        """.trimIndent()

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You must output JSON only."),
                GroqMessage(role = "user", content = prompt)
            )
        )

        try {
            val service = getApiService(apiUrl)
            val response = service.checkGrammar(
                authHeader = "Bearer $apiKey",
                request = request
            )

            val content = response.choices.firstOrNull()?.message?.content ?: return@withContext null
            
            // Parse the JSON string from content into GrammarCheckResult
            gson.fromJson(content, GrammarCheckResult::class.java).copy(originalText = sentence)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun applyActionToSentence(sentence: String, actionType: String): String? = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences("grammarlens_prefs", Context.MODE_PRIVATE)
        val savedKey = sharedPrefs.getString("groq_api_key", "") ?: ""
        val apiKey = savedKey.ifEmpty { BuildConfig.GROQ_API_KEY }
        val apiUrl = sharedPrefs.getString("groq_api_url", "https://api.groq.com/openai/v1/") ?: "https://api.groq.com/openai/v1/"

        if (apiKey.isEmpty()) {
            return@withContext null
        }

        val prompt = when (actionType) {
            "Improve Vocabulary" -> "Rewrite the following sentence to use more advanced and professional vocabulary. Respond ONLY with the rewritten sentence, no extra text: \"$sentence\""
            "Make Formal" -> "Rewrite the following sentence to sound highly formal and polite. Respond ONLY with the rewritten sentence, no extra text: \"$sentence\""
            else -> return@withContext null
        }

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "system", content = "You are a writing assistant. You must reply only with the transformed sentence and nothing else."),
                GroqMessage(role = "user", content = prompt)
            ),
            responseFormat = ResponseFormat(type = "text") // We just want plain text back
        )

        try {
            val service = getApiService(apiUrl)
            val response = service.checkGrammar(
                authHeader = "Bearer $apiKey",
                request = request
            )

            response.choices.firstOrNull()?.message?.content?.trim()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
