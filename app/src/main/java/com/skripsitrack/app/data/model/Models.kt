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