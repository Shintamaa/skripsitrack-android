package com.skripsitrack.app.util

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.skripsitrack.app.R
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
    "selesai"  -> Triple("Selesai",  Color.parseColor("#1F6B3A"), Color.parseColor("#DDF3E4"))
    "ditunda"  -> Triple("Ditunda",  Color.parseColor("#6B4A00"), Color.parseColor("#FFEDC4"))
    else       -> Triple("Berjalan", Color.parseColor("#064A69"), Color.parseColor("#D6EEFF"))
}

fun stageStatusStyle(status: String): Triple<String, Int, Int> = when (status) {
    "proses"  -> Triple("Proses",      Color.parseColor("#064A69"), Color.parseColor("#D6EEFF"))
    "revisi"  -> Triple("Revisi",      Color.parseColor("#8A3A12"), Color.parseColor("#FBE1D4"))
    "selesai" -> Triple("Selesai",     Color.parseColor("#1F6B3A"), Color.parseColor("#DDF3E4"))
    else      -> Triple("Belum mulai", Color.parseColor("#4A6A7C"), Color.parseColor("#E4ECF3"))
}

fun submissionStatusStyle(status: String): Triple<String, Int, Int> = when (status) {
    "perlu_revisi" -> Triple("Perlu revisi",    Color.parseColor("#8A3A12"), Color.parseColor("#FBE1D4"))
    "disetujui"    -> Triple("Disetujui",       Color.parseColor("#1F6B3A"), Color.parseColor("#DDF3E4"))
    else           -> Triple("Menunggu review", Color.parseColor("#6B4A00"), Color.parseColor("#FFEDC4"))
}

/** Latar label status berbentuk pil (sudut membulat) dengan warna dinamis. */
fun View.setPillColor(color: Int) {
    background = ContextCompat.getDrawable(context, R.drawable.bg_pill)?.mutate()
    backgroundTintList = ColorStateList.valueOf(color)
}

/** Mengembalikan (ikon, warna ikon, warna latar ikon) untuk jenis aktivitas. */
fun activityActionStyle(action: String, description: String = ""): Triple<Int, Int, Int> = when {
    // Review dosen: "meminta revisi" -> pensil merah muda, selain itu -> centang krem
    action == "review" && description.contains("revisi", ignoreCase = true) ->
        Triple(R.drawable.ic_edit, Color.parseColor("#8A3A12"), Color.parseColor("#FBE1D4"))
    action == "review" ->
        Triple(R.drawable.ic_check, Color.parseColor("#7A4B00"), Color.parseColor("#FFEDC4"))
    else -> activityActionByKey(action)
}

private fun activityActionByKey(action: String): Triple<Int, Int, Int> = when (action) {
    "upload"       -> Triple(R.drawable.ic_upload,  Color.parseColor("#0A5F86"), Color.parseColor("#D6EEFF"))
    "komentar"     -> Triple(R.drawable.ic_comment, Color.parseColor("#0A5F86"), Color.parseColor("#E4ECF3"))
    "ubah_status"  -> Triple(R.drawable.ic_check,   Color.parseColor("#7A4B00"), Color.parseColor("#FFEDC4"))
    "hapus"        -> Triple(R.drawable.ic_trash,   Color.parseColor("#8A3A12"), Color.parseColor("#FBE1D4"))
    "buat_skripsi" -> Triple(R.drawable.ic_thesis,  Color.parseColor("#0A5F86"), Color.parseColor("#D6EEFF"))
    else           -> Triple(R.drawable.ic_history, Color.parseColor("#4A6A7C"), Color.parseColor("#E4ECF3"))
}

/** Inisial untuk avatar, mis. "Mahasiswa Contoh" -> "MC". */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
