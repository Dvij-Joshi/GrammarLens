package com.example.grammarlens.network

import com.google.gson.annotations.SerializedName

data class GroqRequest(
    val model: String = "llama-3.3-70b-versatile",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.0,
    @SerializedName("response_format")
    val responseFormat: ResponseFormat = ResponseFormat()
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class GroqResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: GroqMessage
)

data class GrammarCheckResult(
    val originalText: String,
    val isGrammarCorrect: Boolean,
    val mistakes: List<MistakeDetail> = emptyList(),
    val correctedText: String
)

data class MistakeDetail(
    val mistake: String,
    val category: String,
    val suggestion: String
)
