package com.skripsitrack.app.data.model

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val nim_nidn: String?
)

data class LoginData(
    val user: User,
    val token: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ThesisStage(
    val id: Int,
    val thesis_id: Int,
    val name: String,
    val sort_order: Int,
    val target_date: String?,
    val completed_at: String?,
    val status: String
)

data class ActivityUser(
    val id: Int,
    val name: String
)

data class ActivityLog(
    val id: Int,
    val action: String,
    val description: String,
    val created_at: String,
    val user: ActivityUser?
)

data class DashboardData(
    val total_skripsi: Int,
    val progress_persen: Int,
    val tahapan_selesai: Int,
    val total_tahapan: Int,
    val menunggu_review: Int,
    val perlu_revisi: Int,
    val deadline_terdekat: ThesisStage?,
    val aktivitas_terbaru: List<ActivityLog>
)