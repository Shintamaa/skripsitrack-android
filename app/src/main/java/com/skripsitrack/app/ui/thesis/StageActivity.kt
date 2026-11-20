package com.skripsitrack.app.ui.thesis

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.skripsitrack.app.R
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.model.*
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.ActivityStageBinding
import com.skripsitrack.app.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class StageActivity : AppCompatActivity(), SubmissionAdapter.Callbacks {
    private lateinit var b: ActivityStageBinding
    private lateinit var adapter: SubmissionAdapter
    private var stageId = 0
    private var picked: PickedFile? = null
    private var canSelectAny = false
    private val isMahasiswa get() = SkripsiApp.session.getRole() == "mahasiswa"

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onFilePicked(it) }
        }

    // Tombol Back keluar dari mode pilih terlebih dahulu
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() { adapter.setSelectionMode(false) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStageBinding.inflate(layoutInflater)
        setContentView(b.root)

        stageId = intent.getIntExtra("stage_id", 0)
        b.toolbar.title = intent.getStringExtra("stage_name") ?: "Tahapan"
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback(this, backCallback)

        adapter = SubmissionAdapter(isDosen = !isMahasiswa, cb = this)
        b.rvSubmissions.layoutManager = LinearLayoutManager(this)
        b.rvSubmissions.adapter = adapter

        b.swipe.setOnRefreshListener { load(full = false) }
        b.btnRetry.setOnClickListener { load(full = true) }
        b.btnPick.setOnClickListener { pickFile.launch(ALLOWED_MIMES) }
        b.btnUpload.setOnClickListener { upload() }

        b.tvSelect.setOnClickListener { adapter.setSelectionMode(true) }
        b.tvSelectAll.setOnClickListener { adapter.selectAll() }
        b.tvCancelSelect.setOnClickListener { adapter.setSelectionMode(false) }
        b.btnDeleteSelected.setOnClickListener { confirmBulkDelete() }

        load(full = true)
    }

    // ---------- Muat data ----------

    private fun load(full: Boolean) {
        lifecycleScope.launch {
            b.layoutError.isVisible = false
            if (full) {
                b.progressLoading.isVisible = true
                b.swipe.isVisible = false
            }

            val r = safeApiCall { RetrofitClient.api.stageDetail(stageId) }

            b.progressLoading.isVisible = false
            b.swipe.isRefreshing = false

            when (r) {
                is Resource.Success -> {
                    val s = r.data.data
                    if (s == null) showError("Data tahapan tidak ditemukan.") else {
                        b.swipe.isVisible = true
                        bind(s)
                    }
                }
                is Resource.Error -> {
                    if (r.code == 401) redirectToLogin("Sesi berakhir, silakan login kembali")
                    else if (full) showError(r.message)
                    else toast(r.message)
                }
                else -> {}
            }
        }
    }

    private fun showError(msg: String) {
        b.swipe.isVisible = false
        b.layoutError.isVisible = true
        b.tvError.text = msg
    }

    private fun bind(s: ThesisStage) {
        b.toolbar.title = s.name
        b.tvThesisTitle.text = "Skripsi: ${s.thesis?.title ?: "-"}"

        val (label, textColor, bgColor) = stageStatusStyle(s.status)
        b.tvStatus.text = label
        b.tvStatus.setTextColor(textColor)
        b.tvStatus.setBackgroundColor(bgColor)

        val info = DateUtil.deadlineInfo(s.status, s.target_date)
        b.tvTarget.text = "Target: ${DateUtil.formatDate(s.target_date)}" +
                (info?.let { "  ($it)" } ?: "")

        val done = s.status == "selesai"
        b.cardUpload.isVisible = isMahasiswa && !done
        b.tvDoneNote.isVisible = done

        val subs = s.submissions.orEmpty()
        adapter.submit(subs)
        b.tvEmpty.isVisible = subs.isEmpty()
        b.tvEmpty.text = if (isMahasiswa)
            "Belum ada unggahan. Unggah draf pertama Anda pada kartu di atas."
        else
            "Mahasiswa belum mengunggah apa pun untuk tahapan ini."

        canSelectAny = isMahasiswa && subs.any { it.status != "disetujui" }
        if (!canSelectAny && adapter.selectionMode) adapter.setSelectionMode(false)
        else updateSelectionUi(adapter.selectedIds().size)
    }

    // ---------- Aksi (komentar, review, hapus) ----------

    override fun onOpen(s: Submission) = openFile(s)

    override fun onSelectionChanged(count: Int) = updateSelectionUi(count)

    override fun onComment(s: Submission) {
        showInputDialog("Komentar untuk v${s.version}", "Tulis komentar", "Kirim") { text ->
            runAction("Komentar terkirim") {
                RetrofitClient.api.addComment(s.id, CommentRequest(text))
            }
        }
    }

    override fun onApprove(s: Submission) {
        AlertDialog.Builder(this)
            .setTitle("Setujui versi ${s.version}?")
            .setMessage("Tahapan akan ditandai selesai dan mahasiswa tidak dapat mengunggah lagi.")
            .setPositiveButton("Setujui") { _, _ ->
                runAction(null) { RetrofitClient.api.reviewSubmission(s.id, ReviewRequest("disetujui")) }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onRevise(s: Submission) {
        showInputDialog("Minta revisi v${s.version}", "Catatan revisi untuk mahasiswa", "Kirim") { text ->
            runAction(null) {
                RetrofitClient.api.reviewSubmission(s.id, ReviewRequest("perlu_revisi", text))
            }
        }
    }

    override fun onDelete(s: Submission) {
        AlertDialog.Builder(this)
            .setTitle("Hapus versi ${s.version}?")
            .setMessage("Berkas dan komentarnya akan ikut terhapus. Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                runAction("Unggahan dihapus") { RetrofitClient.api.deleteSubmission(s.id) }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmBulkDelete() {
        val ids = adapter.selectedIds()
        if (ids.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Hapus ${ids.size} unggahan?")
            .setMessage("Berkas dan komentar pada unggahan terpilih akan ikut terhapus. Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                runAction(null) { RetrofitClient.api.bulkDeleteSubmissions(BulkDeleteRequest(ids)) }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /** Jalankan satu aksi ke API, tampilkan hasilnya, lalu muat ulang. */
    private fun <T> runAction(successMsg: String?, call: suspend () -> ApiResponse<T>) {
        lifecycleScope.launch {
            b.swipe.isRefreshing = true
            val r = safeApiCall { call() }
            when (r) {
                is Resource.Success -> {
                    toast(successMsg ?: r.data.message ?: "Berhasil")
                    setResult(Activity.RESULT_OK)
                    adapter.setSelectionMode(false)
                    load(full = false)
                }
                is Resource.Error -> {
                    b.swipe.isRefreshing = false
                    if (r.code == 401) redirectToLogin("Sesi berakhir, silakan login kembali")
                    else toast(r.message)
                }
                else -> {}
            }
        }
    }

    private fun showInputDialog(
        title: String, hint: String, positive: String, onSubmit: (String) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_input, null)
        val til = view.findViewById<TextInputLayout>(R.id.tilInput)
        val et = view.findViewById<TextInputEditText>(R.id.etInput)
        til.hint = hint

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(positive, null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            // Listener dipasang manual agar dialog tidak tertutup saat input kosong
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val text = et.text.toString().trim()
                if (text.isEmpty()) { til.error = "Wajib diisi"; return@setOnClickListener }
                dialog.dismiss()
                onSubmit(text)
            }
        }
        dialog.show()
    }

    private fun updateSelectionUi(count: Int) {
        val on = adapter.selectionMode
        b.tvSelect.isVisible = canSelectAny && !on
        b.tvSelectAll.isVisible = on
        b.tvCancelSelect.isVisible = on
        b.layoutSelection.isVisible = on
        b.tvSelectedCount.text = "$count dipilih"
        b.btnDeleteSelected.isEnabled = count > 0
        backCallback.isEnabled = on
    }

    // ---------- Pilih dan unggah berkas ----------

    private fun onFilePicked(uri: Uri) {
        val f = queryFile(uri)

        if (f.mime !in ALLOWED_MIMES) {
            toast("Format tidak didukung. Gunakan JPG, PNG, PDF, atau DOC/DOCX.")
            return
        }
        if (f.size > MAX_UPLOAD_BYTES) {
            toast("Ukuran berkas ${formatSize(f.size)}. Maksimal 5 MB.")
            return
        }

        picked = f
        b.tvFileName.isVisible = true
        b.tvFileName.text = "${f.name}  (${formatSize(f.size)})"
        b.btnUpload.isEnabled = true

        if (f.mime.startsWith("image/")) {
            b.ivPickPreview.isVisible = true
            Glide.with(this).load(uri).centerCrop().into(b.ivPickPreview)
        } else {
            b.ivPickPreview.isVisible = false
        }
    }

    private fun upload() {
        val f = picked ?: return
        val note = b.etNote.text.toString().trim()

        lifecycleScope.launch {
            setUploading(true)

            val file = withContext(Dispatchers.IO) { copyToCache(f) }
            try {
                val part = MultipartBody.Part.createFormData(
                    "file", file.name, file.asRequestBody(f.mime.toMediaTypeOrNull())
                )
                val noteBody = note.takeIf { it.isNotEmpty() }
                    ?.toRequestBody("text/plain".toMediaTypeOrNull())

                val r = safeApiCall { RetrofitClient.api.uploadSubmission(stageId, part, noteBody) }

                when (r) {
                    is Resource.Success -> {
                        val v = r.data.data?.version
                        toast(if (v != null) "Berkas diunggah sebagai versi $v" else "Berkas diunggah")
                        resetPicker()
                        setResult(Activity.RESULT_OK)
                        load(full = false)
                    }
                    is Resource.Error -> {
                        if (r.code == 401) redirectToLogin("Sesi berakhir, silakan login kembali")
                        else toast(r.message)
                    }
                    else -> {}
                }
            } finally {
                file.delete()
                setUploading(false)
            }
        }
    }

    private fun resetPicker() {
        picked = null
        b.tvFileName.isVisible = false
        b.ivPickPreview.isVisible = false
        b.etNote.setText("")
        b.btnUpload.isEnabled = false
    }

    private fun setUploading(uploading: Boolean) {
        b.progressUpload.visibility = if (uploading) View.VISIBLE else View.GONE
        b.btnPick.isEnabled = !uploading
        b.btnUpload.isEnabled = !uploading && picked != null
    }

    private fun openFile(s: Submission) {
        val url = s.file_url ?: return toast("Tautan berkas tidak tersedia.")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("Tidak ada aplikasi untuk membuka berkas ini.")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}