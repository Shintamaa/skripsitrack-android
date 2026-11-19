package com.skripsitrack.app.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtil {
    private val localeId = Locale("id", "ID")

    fun formatDateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val src = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
            val date = src.parse(iso.take(19))!!
            SimpleDateFormat("dd MMM yyyy, HH:mm", localeId).format(date)
        } catch (e: Exception) { iso }
    }

    fun formatDate(d: String?): String {
        if (d.isNullOrBlank()) return "-"
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d.take(10))!!
            SimpleDateFormat("dd MMM yyyy", localeId).format(date)
        } catch (e: Exception) { d }
    }

    /** Selisih hari dari hari ini; negatif berarti sudah lewat. */
    fun daysUntil(d: String?): Long? {
        if (d.isNullOrBlank()) return null
        return try {
            val target = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d.take(10))!!
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            TimeUnit.MILLISECONDS.toDays(target.time - today.time)
        } catch (e: Exception) { null }
    }

    fun actionLabel(action: String): String = when (action) {
        "upload" -> "Unggah"
        "komentar" -> "Komentar"
        "ubah_status" -> "Status"
        "buat_skripsi" -> "Skripsi"
        else -> action
    }
}