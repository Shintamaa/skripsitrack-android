package com.skripsitrack.app.ui.thesis

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.skripsitrack.app.data.model.Thesis
import com.skripsitrack.app.data.model.ThesisRequest
import com.skripsitrack.app.data.model.UserMini
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.ActivityThesisFormBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.Resource
import com.skripsitrack.app.util.redirectToLogin
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ThesisFormActivity : AppCompatActivity() {
    private lateinit var b: ActivityThesisFormBinding

    private var editing: Thesis? = null
    private var supervisors: List<UserMini> = emptyList()
    private var supervisorId: Int? = null
    private var startDate: String? = null
    private var targetDate: String? = null
    private val statuses = listOf("berjalan", "selesai", "ditunda")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityThesisFormBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }

        editing = intent.getStringExtra("thesis")?.let { Gson().fromJson(it, Thesis::class.java) }
        val t = editing

        if (t != null) {
            b.toolbar.title = "Ubah Skripsi"
            b.etTitle.setText(t.title)
            b.etDesc.setText(t.description ?: "")
            startDate = t.start_date?.take(10)
            targetDate = t.target_date?.take(10)
            b.etStart.setText(DateUtil.formatDate(startDate))
            b.etTarget.setText(DateUtil.formatDate(targetDate))

            b.tilSupervisor.visibility = View.GONE          // pembimbing tidak diubah saat edit
            b.tilStatus.visibility = View.VISIBLE
            b.actStatus.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses))
            b.actStatus.setText(t.status, false)
        } else {
            b.toolbar.title = "Tambah Skripsi"
            loadSupervisors()
        }

        b.etStart.setOnClickListener { pickDate { startDate = it; b.etStart.setText(DateUtil.formatDate(it)) } }
        b.etTarget.setOnClickListener { pickDate { targetDate = it; b.etTarget.setText(DateUtil.formatDate(it)) } }
        b.btnSave.setOnClickListener { save() }
    }

    private fun pickDate(onPicked: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Pilih tanggal").build()
        picker.addOnPositiveButtonClickListener { millis ->
            // MaterialDatePicker mengembalikan tengah malam UTC
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            onPicked(f.format(Date(millis)))
        }
        picker.show(supportFragmentManager, "date")
    }

    private fun loadSupervisors() {
        lifecycleScope.launch {
            setLoading(true)
            val r = safeApiCall { RetrofitClient.api.supervisors() }
            setLoading(false)
            when (r) {
                is Resource.Success -> {
                    supervisors = r.data.data.orEmpty()
                    if (supervisors.isEmpty()) {
                        Toast.makeText(this@ThesisFormActivity,
                            "Belum ada dosen pembimbing terdaftar", Toast.LENGTH_LONG).show()
                    }
                    b.actSupervisor.setAdapter(ArrayAdapter(this@ThesisFormActivity,
                        android.R.layout.simple_dropdown_item_1line, supervisors.map { it.name }))
                    b.actSupervisor.setOnItemClickListener { _, _, pos, _ ->
                        supervisorId = supervisors[pos].id
                        b.tilSupervisor.error = null
                    }
                }
                is Resource.Error -> {
                    if (r.code == 401) redirectToLogin("Sesi berakhir, silakan login kembali")
                    else Toast.makeText(this@ThesisFormActivity, r.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                else -> {}
            }
        }
    }

    private fun save() {
        val title = b.etTitle.text.toString().trim()
        val desc = b.etDesc.text.toString().trim().ifEmpty { null }

        listOf(b.tilTitle, b.tilSupervisor, b.tilStart, b.tilTarget).forEach { it.error = null }

        val sd = startDate
        val td = targetDate
        when {
            title.length < 5 -> { b.tilTitle.error = "Judul minimal 5 karakter"; return }
            editing == null && supervisorId == null -> { b.tilSupervisor.error = "Pilih dosen pembimbing"; return }
            sd == null -> { b.tilStart.error = "Pilih tanggal mulai"; return }
            td == null -> { b.tilTarget.error = "Pilih target selesai"; return }
            td < sd -> { b.tilTarget.error = "Target tidak boleh sebelum tanggal mulai"; return }
        }

        val current = editing
        val body = ThesisRequest(
            supervisor_id = if (current == null) supervisorId else null,
            title = title,
            description = desc,
            start_date = sd,
            target_date = td,
            status = if (current != null) b.actStatus.text.toString() else null
        )

        lifecycleScope.launch {
            setLoading(true)
            val r = safeApiCall {
                if (current == null) RetrofitClient.api.createThesis(body)
                else RetrofitClient.api.updateThesis(current.id, body)
            }
            setLoading(false)

            when (r) {
                is Resource.Success -> {
                    Toast.makeText(this@ThesisFormActivity,
                        r.data.message ?: "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is Resource.Error -> {
                    if (r.code == 401) redirectToLogin("Sesi berakhir, silakan login kembali")
                    else Toast.makeText(this@ThesisFormActivity, r.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnSave.isEnabled = !loading
    }
}