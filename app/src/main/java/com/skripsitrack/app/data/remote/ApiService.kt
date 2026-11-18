package com.skripsitrack.app.data.remote

import com.skripsitrack.app.data.model.*
import retrofit2.http.*

interface ApiService {
    @POST("login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginData>

    @POST("logout")
    suspend fun logout(): ApiResponse<Unit>

    @GET("me")
    suspend fun me(): ApiResponse<User>
}