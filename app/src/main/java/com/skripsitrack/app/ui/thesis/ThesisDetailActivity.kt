package com.skripsitrack.app.ui.thesis

import com.skripsitrack.app.util.setPillColor
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.skripsitrack.app.data.model.Thesis
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.ActivityThesisDetailBinding
import com.skripsitrack.app.util.*
import kotlinx.coroutines.launch

class ThesisDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityThesisDetailBinding
    private var thesisId = 0

    private val adapter = StageAdapter { stage ->
        stageLauncher.launch(
            Intent(this, StageActivity::class.java)
                .putExtra("stage_id", stage.id)
                .putExtra("stage_name", stage.name)
        )
    }

    // Jika ada perubahan di layar tahapan (unggah, dll.), muat ulang dan beri tahu daftar di belakang
    private val stageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                load(full = false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityThesisDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        thesisId = intent.getIntExtra("thesis_id", 0)
        b.toolbar.setNavigationOnClickListener { finish() }

        b.rvStages.layoutManager = LinearLayoutManager(this)
        b.rvStages.adapter = adapter
        b.swipe.setOnRefreshListener { load(full = false) }
        b.btnRetry.setOnClickListener { load(full = true) }

        load(full = true)
    }

    private fun load(full: Boolean) {
        lifecycleScope.launch {
            b.layoutError.isVisible = false
            if (full) {
                b.progressLoading.isVisible = true
                b.swipe.isVisible = false
            }

            val r = safeApiCall { RetrofitClient.api.thesisDetail(thesisId) }

            b.progressLoading.isVisible = false
            b.swipe.isRefreshing = false

            when (r) {
                is Resource.Success -> {
                    val t = r.data.data
                    if (t == null) showError("Data skripsi tidak ditemukan.") else {
                        b.swipe.isVisible = true
                        bind(t)
                    }
                }
                is Resource.Error -> {
                    if (r.code == 401) redirectToLogin("Sesi berakhir, silakan login kembali")
                    else if (full) showError(r.message)
                    else Toast.makeText(this@ThesisDetailActivity, r.message, Toast.LENGTH_LONG).show()
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

    private fun bind(t: Thesis) {
        b.tvTitle.text = t.title

        val (label, textColor, bgColor) = thesisStatusStyle(t.status)
        b.tvStatus.text = label
        b.tvStatus.setTextColor(textColor)
        b.tvStatus.setPillColor(bgColor)

        b.tvPeople.text = "Mahasiswa: ${t.student?.name ?: "-"} (${t.student?.nim_nidn ?: "-"})\n" +
                "Pembimbing: ${t.supervisor?.name ?: "-"}"
        b.tvDates.text = "${DateUtil.formatDate(t.start_date)}  →  ${DateUtil.formatDate(t.target_date)}"

        b.tvDesc.isVisible = !t.description.isNullOrBlank()
        b.tvDesc.text = t.description

        val stages = t.stages.orEmpty()
        val done = stages.count { it.status == "selesai" }
        val percent = if (stages.isNotEmpty()) done * 100 / stages.size else 0
        b.lpi.setProgressCompat(percent, true)
        b.tvProgress.text = "$done/${stages.size} tahapan selesai ($percent%)"

        adapter.submit(stages)
        b.tvEmpty.isVisible = stages.isEmpty()
    }
}