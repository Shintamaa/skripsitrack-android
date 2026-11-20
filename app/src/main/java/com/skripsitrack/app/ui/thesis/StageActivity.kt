package com.skripsitrack.app.ui.thesis

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.model.Submission
import com.skripsitrack.app.data.model.ThesisStage
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

class StageActivity : AppCompatActivity() {
    private lateinit var b: ActivityStageBinding
    private var stageId = 0
    private var picked: PickedFile? = null
    private val isMahasiswa get() = SkripsiApp.session.getRole() == "mahasiswa"
    private val adapter = SubmissionAdapter { openFile(it) }

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onFilePicked(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStageBinding.inflate(layoutInflater)
        setContentView(b.root)

        stageId = intent.getIntExtra("stage_id", 0)
        b.toolbar.title = intent.getStringExtra("stage_name") ?: "Tahapan"
        b.toolbar.setNavigationOnClickListener { finish() }

        b.rvSubmissions.layoutManager = LinearLayoutManager(this)
        b.rvSubmissions.adapter = adapter

        b.swipe.setOnRefreshListener { load(full = false) }
        b.btnRetry.setOnClickListener { load(full = true) }
        b.btnPick.setOnClickListener { pickFile.launch(ALLOWED_MIMES) }
        b.btnUpload.setOnClickListener { upload() }

        load(full = true)
    }

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
    }

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