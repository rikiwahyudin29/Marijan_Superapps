package com.rtekmidev.marijancbt.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val BASE_URL = "https://smkriyadhuljannahjalancagak.sch.id/"

    var authToken: String = ""

    // Interceptor untuk menambahkan X-API-KEY dan Accept JSON
    private val apiKeyInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("X-API-KEY", "SuperSecretSiakad2026")
            .header("Accept", "application/json")
            .apply {
                if (authToken.isNotEmpty()) {
                    header("Authorization", "Bearer $authToken")
                }
            }
            .build()
        chain.proceed(request)
    }

    // 🔥 INI OBAT TIMEOUT-NYA BOS: Kita paksa Android nunggu sampai 60 detik
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Masukkan settingan sabar ke sini
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}