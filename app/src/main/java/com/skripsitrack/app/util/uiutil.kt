package com.skripsitrack.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.ui.auth.LoginActivity

fun Context.redirectToLogin(message: String? = null) {
    SkripsiApp.session.clear()
    message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
    startActivity(
        Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
}

/** Mengembalikan (label, warna teks, warna latar) */
fun thesisStatusStyle(status: String): Triple<String, Int, Int> = when (status) {
    "selesai"  -> Triple("Selesai",  Color.parseColor("#2E7D32"), Color.parseColor("#E8F5E9"))
    "ditunda"  -> Triple("Ditunda",  Color.parseColor("#E65100"), Color.parseColor("#FFF3E0"))
    else       -> Triple("Berjalan", Color.parseColor("#1565C0"), Color.parseColor("#E3F2FD"))
}