package com.skripsitrack.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.ui.auth.LoginActivity
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun Context.redirectToLogin(message: String? = null) {
    SkripsiApp.session.clear()
    message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
    startActivity(
        Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
}

fun normalizeBaseUrl(input: String): String? {
    var u = input.trim()
    if (u.isEmpty()) return null
    if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
    u = u.trimEnd('/')
    if (!u.endsWith("/api")) u += "/api"
    u += "/"
    return if (u.toHttpUrlOrNull() != null) u else null
}

/** Mengembalikan (label, warna teks, warna latar) */
fun thesisStatusStyle(status: String): Triple<String, Int, Int> = when (status) {
    "selesai"  -> Triple("Selesai",  Color.parseColor("#2E7D32"), Color.parseColor("#E8F5E9"))
    "ditunda"  -> Triple("Ditunda",  Color.parseColor("#E65100"), Color.parseColor("#FFF3E0"))
    else       -> Triple("Berjalan", Color.parseColor("#1565C0"), Color.parseColor("#E3F2FD"))
}

fun stageStatusStyle(status: String): Triple<String, Int, Int> = when (status) {
    "proses"  -> Triple("Proses",      Color.parseColor("#1565C0"), Color.parseColor("#E3F2FD"))
    "revisi"  -> Triple("Revisi",      Color.parseColor("#E65100"), Color.parseColor("#FFF3E0"))
    "selesai" -> Triple("Selesai",     Color.parseColor("#2E7D32"), Color.parseColor("#E8F5E9"))
    else      -> Triple("Belum mulai", Color.parseColor("#607D8B"), Color.parseColor("#ECEFF1"))
}

fun submissionStatusStyle(status: String): Triple<String, Int, Int> = when (status) {
    "perlu_revisi" -> Triple("Perlu revisi",    Color.parseColor("#E65100"), Color.parseColor("#FFF3E0"))
    "disetujui"    -> Triple("Disetujui",       Color.parseColor("#2E7D32"), Color.parseColor("#E8F5E9"))
    else           -> Triple("Menunggu review", Color.parseColor("#455A64"), Color.parseColor("#ECEFF1"))
}