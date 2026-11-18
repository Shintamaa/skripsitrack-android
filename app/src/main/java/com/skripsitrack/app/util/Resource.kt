package com.skripsitrack.app.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException
import java.io.IOException

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> T): Resource<T> {
    return try {
        Resource.Success(call())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        var msg = "Terjadi kesalahan (${e.code()})"
        try {
            val json = Gson().fromJson(body, JsonObject::class.java)
            // 422: ambil pesan error validasi pertama jika ada
            val errors = json.getAsJsonObject("errors")
            msg = if (errors != null && errors.size() > 0) {
                errors.entrySet().first().value.asJsonArray[0].asString
            } else {
                json.get("message")?.asString ?: msg
            }
        } catch (_: Exception) { }
        Resource.Error(msg, e.code())
    } catch (e: IOException) {
        Resource.Error("Gagal terhubung ke server. Periksa koneksi dan jaringan Wi-Fi Anda.")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Terjadi kesalahan tidak diketahui")
    }
}