package com.example.grammarlens.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApiService {
    @POST("chat/completions")
    suspend fun checkGrammar(
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): GroqResponse
}
