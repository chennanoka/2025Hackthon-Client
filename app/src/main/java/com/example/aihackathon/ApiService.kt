package com.example.aihackathon

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


data class MessageRequest(
    val request: String
)

data class MessageResponse(
    val route: String,
    val message: String,
    val type: String,
)

interface ApiService {
    @POST("ai-server")
    fun postRequest(@Body request: MessageRequest): Call<MessageResponse>
}
