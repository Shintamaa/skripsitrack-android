package com.skripsitrack.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

data class PickedFile(val uri: Uri, val name: String, val size: Long, val mime: String)

val ALLOWED_MIMES = arrayOf(
    "image/jpeg",
    "image/png",
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
)
const val MAX_UPLOAD_BYTES = 5L * 1024 * 1024   // sama dengan aturan di backend (5 MB)

fun Context.queryFile(uri: Uri): PickedFile {
    var name = "berkas"
    var size = 0L
    contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            if (ni >= 0) name = c.getString(ni)
            if (si >= 0) size = c.getLong(si)
        }
    }
    val mime = contentResolver.getType(uri) ?: "application/octet-stream"
    return PickedFile(uri, name, size, mime)
}

/** Salin isi Uri ke file sementara di cache agar bisa dikirim sebagai multipart. */
fun Context.copyToCache(f: PickedFile): File {
    val safeName = f.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val out = File(cacheDir, safeName)
    contentResolver.openInputStream(f.uri)!!.use { input ->
        out.outputStream().use { output -> input.copyTo(output) }
    }
    return out
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    else -> "${bytes / 1024} KB"
}