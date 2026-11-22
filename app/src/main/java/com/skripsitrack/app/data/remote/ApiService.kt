package com.skripsitrack.app.data.remote

import com.skripsitrack.app.data.model.*
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.skripsitrack.app.data.model.ThesisStage
import com.skripsitrack.app.data.model.Submission
import com.skripsitrack.app.data.model.Comment
import com.skripsitrack.app.data.model.CommentRequest
import com.skripsitrack.app.data.model.ReviewRequest
import com.skripsitrack.app.data.model.BulkDeleteRequest
import com.skripsitrack.app.data.model.BulkDeleteResult

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

    @GET("stages/{id}")
    suspend fun stageDetail(@Path("id") id: Int): ApiResponse<ThesisStage>

    @Multipart
    @POST("stages/{id}/submissions")
    suspend fun uploadSubmission(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part,
        @Part("note") note: RequestBody?
    ): ApiResponse<Submission>

    @POST("submissions/{id}/comments")
    suspend fun addComment(@Path("id") id: Int, @Body body: CommentRequest): ApiResponse<Comment>

    @PATCH("submissions/{id}/status")
    suspend fun reviewSubmission(@Path("id") id: Int, @Body body: ReviewRequest): ApiResponse<Submission>

    @DELETE("submissions/{id}")
    suspend fun deleteSubmission(@Path("id") id: Int): ApiResponse<Unit>

    @POST("submissions/bulk-delete")
    suspend fun bulkDeleteSubmissions(@Body body: BulkDeleteRequest): ApiResponse<BulkDeleteResult>

    @GET("activities")
    suspend fun activities(
        @Query("page") page: Int,
        @Query("action") action: String?
    ): ApiResponse<Page<ActivityLog>>

    @POST("register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<LoginData>
}