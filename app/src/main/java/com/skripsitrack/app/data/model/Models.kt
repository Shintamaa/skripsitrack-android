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
    val status: String,
    val submissions_count: Int? = null,
    val submissions: List<Submission>? = null,
    val thesis: Thesis? = null
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

data class UserMini(
    val id: Int,
    val name: String,
    val nim_nidn: String? = null
)

data class Thesis(
    val id: Int,
    val student_id: Int,
    val supervisor_id: Int,
    val title: String,
    val description: String?,
    val status: String,
    val start_date: String?,
    val target_date: String?,
    val student: UserMini?,
    val supervisor: UserMini?,
    val stages_count: Int?,
    val done_stages_count: Int?,
    val stages: List<ThesisStage>?
)

// Field null tidak ikut terkirim (Gson membuang null), jadi satu class cukup untuk tambah dan ubah
data class ThesisRequest(
    val supervisor_id: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val start_date: String? = null,
    val target_date: String? = null,
    val status: String? = null
)

data class CommentUser(val id: Int, val name: String, val role: String?)

data class Comment(
    val id: Int,
    val submission_id: Int,
    val user_id: Int,
    val body: String,
    val created_at: String,
    val user: CommentUser?
)

data class Submission(
    val id: Int,
    val stage_id: Int,
    val version: Int,
    val file_path: String,
    val file_type: String,
    val file_url: String?,
    val note: String?,
    val status: String,
    val created_at: String,
    val comments: List<Comment>? = null
)

