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

    @GET("dashboard")
    suspend fun dashboard(): ApiResponse<DashboardData>

    @GET("supervisors")
    suspend fun supervisors(): ApiResponse<List<UserMini>>

    @GET("theses")
    suspend fun theses(
        @Query("q") q: String?,
        @Query("status") status: String?
    ): ApiResponse<List<Thesis>>

    @GET("theses/{id}")
    suspend fun thesisDetail(@Path("id") id: Int): ApiResponse<Thesis>

    @POST("theses")
    suspend fun createThesis(@Body body: ThesisRequest): ApiResponse<Thesis>

    @PUT("theses/{id}")
    suspend fun updateThesis(@Path("id") id: Int, @Body body: ThesisRequest): ApiResponse<Thesis>

    @DELETE("theses/{id}")
    suspend fun deleteThesis(@Path("id") id: Int): ApiResponse<Unit>
}