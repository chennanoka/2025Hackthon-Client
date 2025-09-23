package com.example.aihackathon // RetrofitInstance.kt
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {


    private const val BASE_URL = "http://10.0.0.243:8080/" // Example API


    var client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Example: 30 seconds connect timeout
        .readTimeout(60, TimeUnit.SECONDS) // Example: 60 seconds read timeout
        .writeTimeout(30, TimeUnit.SECONDS) // Example: 30 seconds write timeout
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}