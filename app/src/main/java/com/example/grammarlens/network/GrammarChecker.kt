package com.example.grammarlens.network

import com.example.grammarlens.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GrammarChecker {

    private val apiService: GroqApiService
    private val gson = Gson()

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(GroqApiService::class.java)
    }

    suspend fun analyzeSentence(sentence: String): GrammarCheckResult? = withContext(Dispatchers.IO) {
        if (BuildConfig.GROQ_API_KEY.isEmpty()) {
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
            val response = apiService.checkGrammar(
                authHeader = "Bearer ${BuildConfig.GROQ_API_KEY}",
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
}
