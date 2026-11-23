package com.skripsitrack.app.ui.thesis

import com.skripsitrack.app.util.applyStatusBarInset
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ThesisFormActivity : AppCompatActivity() {

    private lateinit var b: ActivityThesisFormBinding

    private var editing: Thesis? = null
    private var supervisors: List<UserMini> = emptyList()
    private var supervisorId: Int? = null
    private var startDate: String? = null
    private var targetDate: String? = null
    private var supervisorsLoading = false
    private var saveInProgress = false

    private val statusValues = listOf(
        "Berjalan" to "berjalan",
        "Selesai" to "selesai",
        "Ditunda" to "ditunda"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityThesisFormBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.applyStatusBarInset()

        b.toolbar.setNavigationOnClickListener { finish() }

        editing = intent.getStringExtra("thesis")
            ?.let { Gson().fromJson(it, Thesis::class.java) }

        val thesis = editing
        if (thesis != null) {
            b.toolbar.title = "Ubah Skripsi"
            b.etTitle.setText(thesis.title)
            b.etDesc.setText(thesis.description.orEmpty())

            startDate = thesis.start_date?.take(10)
            targetDate = thesis.target_date?.take(10)
            b.etStart.setText(DateUtil.formatDate(startDate))
            b.etTarget.setText(DateUtil.formatDate(targetDate))

            // Pembimbing tidak dapat diubah saat mengedit skripsi yang sudah dibuat.
            b.tilSupervisor.visibility = View.GONE
            b.tilStatus.isVisible = true
            b.actStatus.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    statusValues.map { it.first }
                )
            )
            b.actStatus.setText(statusLabel(thesis.status), false)
        } else {
            b.toolbar.title = "Tambah Skripsi"
            loadSupervisors()
            b.actSupervisor.setOnClickListener {
                if (supervisors.isEmpty() && !supervisorsLoading) loadSupervisors()
            }
        }

        b.etStart.setOnClickListener { showStartDatePicker() }
        b.tilStart.setEndIconOnClickListener { showStartDatePicker() }

        b.etTarget.setOnClickListener { showTargetDatePicker() }
        b.tilTarget.setEndIconOnClickListener { showTargetDatePicker() }

        b.btnSave.setOnClickListener { save() }
    }

    private fun showStartDatePicker() {
        pickDate(startDate) { selected ->
            startDate = selected
            b.etStart.setText(DateUtil.formatDate(selected))
            b.tilStart.error = null

            if (targetDate != null && targetDate!! < selected) {
                targetDate = null
                b.etTarget.setText("")
                b.tilTarget.error = "Pilih ulang target yang tidak sebelum tanggal mulai"
            }
        }
    }

    private fun showTargetDatePicker() {
        pickDate(targetDate) { selected ->
            val start = startDate
            if (start != null && selected < start) {
                b.tilTarget.error = "Target tidak boleh sebelum tanggal mulai"
                return@pickDate
            }
            targetDate = selected
            b.etTarget.setText(DateUtil.formatDate(selected))
            b.tilTarget.error = null
        }
    }

    private fun pickDate(currentDate: String?, onPicked: (String) -> Unit) {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Pilih tanggal")

        currentDate?.let { dateToUtcMillis(it) }?.let { builder.setSelection(it) }

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { millis ->
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            onPicked(formatter.format(Date(millis)))
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun dateToUtcMillis(value: String): Long? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(value.take(10))?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun loadSupervisors() {
        if (supervisorsLoading) return
        supervisorsLoading = true

        lifecycleScope.launch {
            setLoading(true)
            val result = safeApiCall { RetrofitClient.api.supervisors() }
            supervisorsLoading = false
            setLoading(false)

            when (result) {
                is Resource.Success -> {
                    supervisors = result.data.data.orEmpty()
                    if (supervisors.isEmpty()) {
                        b.tilSupervisor.error = "Belum ada dosen pembimbing terdaftar."
                        Toast.makeText(
                            this@ThesisFormActivity,
                            "Belum ada dosen pembimbing terdaftar.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    b.tilSupervisor.error = null
                    b.actSupervisor.setAdapter(
                        ArrayAdapter(
                            this@ThesisFormActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            supervisors.map { it.name }
                        )
                    )
                    b.actSupervisor.setOnItemClickListener { _, _, position, _ ->
                        supervisorId = supervisors[position].id
                        b.tilSupervisor.error = null
                    }
                }

                is Resource.Error -> {
                    if (result.code == 401) {
                        redirectToLogin("Sesi berakhir, silakan login kembali")
                    } else {
                        b.tilSupervisor.error = "Gagal memuat dosen. Ketuk kolom untuk mencoba lagi."
                        Toast.makeText(
                            this@ThesisFormActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                else -> Unit
            }
        }
    }

    private fun save() {
        if (saveInProgress) return

        val title = b.etTitle.text?.toString()?.trim().orEmpty()
        val description = b.etDesc.text?.toString()?.trim().orEmpty().ifEmpty { null }

        listOf(b.tilTitle, b.tilSupervisor, b.tilStart, b.tilTarget)
            .forEach { it.error = null }

        val start = startDate
        val target = targetDate

        when {
            title.length < 5 -> {
                b.tilTitle.error = "Judul minimal 5 karakter"
                b.etTitle.requestFocus()
                return
            }
            editing == null && supervisorId == null -> {
                b.tilSupervisor.error = "Pilih dosen pembimbing"
                b.actSupervisor.requestFocus()
                return
            }
            start == null -> {
                b.tilStart.error = "Pilih tanggal mulai"
                return
            }
            target == null -> {
                b.tilTarget.error = "Pilih target selesai"
                return
            }
            target < start -> {
                b.tilTarget.error = "Target tidak boleh sebelum tanggal mulai"
                return
            }
        }

        val current = editing
        val selectedStatus = statusValues.firstOrNull {
            it.first == b.actStatus.text.toString()
        }?.second

        val request = ThesisRequest(
            supervisor_id = if (current == null) supervisorId else null,
            title = title,
            description = description,
            start_date = start,
            target_date = target,
            status = if (current != null) selectedStatus ?: current.status else null
        )

        saveInProgress = true
        lifecycleScope.launch {
            setLoading(true)
            val result = safeApiCall {
                if (current == null) {
                    RetrofitClient.api.createThesis(request)
                } else {
                    RetrofitClient.api.updateThesis(current.id, request)
                }
            }
            if (isFinishing || isDestroyed) return@launch
            setLoading(false)
            saveInProgress = false

            when (result) {
                is Resource.Success -> {
                    Toast.makeText(
                        this@ThesisFormActivity,
                        result.data.message ?: "Skripsi berhasil disimpan.",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                is Resource.Error -> {
                    if (result.code == 401) {
                        redirectToLogin("Sesi berakhir, silakan login kembali")
                    } else {
                        Toast.makeText(
                            this@ThesisFormActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                else -> Unit
            }
        }
    }

    private fun statusLabel(value: String): String =
        statusValues.firstOrNull { it.second == value }?.first ?: "Berjalan"

    private fun setLoading(loading: Boolean) {
        b.progress.isVisible = loading
        b.btnSave.isEnabled = !loading
        b.etTitle.isEnabled = !loading
        b.etDesc.isEnabled = !loading
        b.actSupervisor.isEnabled = !loading
        b.etStart.isEnabled = !loading
        b.etTarget.isEnabled = !loading
        b.actStatus.isEnabled = !loading
    }
}
