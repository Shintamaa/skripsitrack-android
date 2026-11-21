package com.skripsitrack.app.data.remote

import com.skripsitrack.app.SkripsiApp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Alamat bawaan; bisa diubah pengguna lewat "Atur server" di layar Login
    const val DEFAULT_BASE_URL = "http://192.168.1.110:8000/api/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                SkripsiApp.session.getToken()?.let {
                    builder.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC   // untuk rilis, BODY terlalu berisik
            })
            .build()
    }

    private var cachedUrl: String? = null
    private var cachedApi: ApiService? = null

    // Dibangun ulang otomatis jika alamat server berubah
    val api: ApiService
        @Synchronized get() {
            val url = SkripsiApp.session.getBaseUrl()
            if (cachedApi == null || cachedUrl != url) {
                cachedApi = Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
                cachedUrl = url
            }
            return cachedApi!!
        }
}