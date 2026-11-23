package com.skripsitrack.app.ui.thesis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.model.Thesis
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.FragmentThesisListBinding
import com.skripsitrack.app.util.Resource
import com.skripsitrack.app.util.redirectToLogin
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ThesisListFragment : Fragment() {

    private var _b: FragmentThesisListBinding? = null
    private val b get() = _b!!

    private val isDosen by lazy { SkripsiApp.session.getRole() == "dosen" }
    private lateinit var adapter: ThesisAdapter

    private var searchJob: Job? = null
    private var requestJob: Job? = null
    private var requestId = 0
    private var query = ""
    private var status: String? = null

    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) load(full = true)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentThesisListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ThesisAdapter(
            isDosen = isDosen,
            onClick = { thesis ->
                formLauncher.launch(
                    Intent(requireContext(), ThesisDetailActivity::class.java)
                        .putExtra("thesis_id", thesis.id)
                )
            },
            onEdit = { thesis -> openForm(thesis) },
            onDelete = { thesis -> confirmDelete(thesis) }
        )

        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        b.fabAdd.isVisible = !isDosen
        b.fabAdd.setOnClickListener { openForm(null) }

        b.swipe.setOnRefreshListener { load(full = false) }
        b.btnRetry.setOnClickListener { load(full = true) }

        b.etSearch.doAfterTextChanged { editable ->
            query = editable?.toString()?.trim().orEmpty()

            // Respons pencarian lama tidak boleh muncul ketika pengguna masih mengetik.
            requestId++
            requestJob?.cancel()
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(400)
                load(full = false)
            }
        }

        b.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            searchJob?.cancel()
            searchJob = null
            status = when (checkedIds.firstOrNull()) {
                b.chipBerjalan.id -> "berjalan"
                b.chipSelesai.id -> "selesai"
                b.chipDitunda.id -> "ditunda"
                else -> null
            }
            load(full = true)
        }

        load(full = true)
    }

    private fun openForm(thesis: Thesis?) {
        val intent = Intent(requireContext(), ThesisFormActivity::class.java)
        thesis?.let { intent.putExtra("thesis", Gson().toJson(it)) }
        formLauncher.launch(intent)
    }

    private fun load(full: Boolean) {
        val currentBinding = _b ?: return

        requestJob?.cancel()
        val id = ++requestId
        val search = query
        val selectedStatus = status

        currentBinding.layoutError.isVisible = false
        currentBinding.tvEmpty.isVisible = false

        if (full) {
            currentBinding.progressLoading.isVisible = true
            currentBinding.swipe.isVisible = false
            currentBinding.swipe.isRefreshing = false
        } else {
            currentBinding.swipe.isVisible = true
            currentBinding.swipe.isRefreshing = true
        }

        requestJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.api.theses(search.ifEmpty { null }, selectedStatus)
            }

            if (id != requestId || _b == null) return@launch

            val binding = _b ?: return@launch
            binding.progressLoading.isVisible = false
            binding.swipe.isRefreshing = false
            binding.swipe.isVisible = true

            when (result) {
                is Resource.Success -> showList(result.data.data.orEmpty())
                is Resource.Error -> handleError(result, showFullError = full)
                else -> Unit
            }
        }
    }

    private fun showList(list: List<Thesis>) {
        val binding = _b ?: return
        adapter.submit(list)
        binding.layoutError.isVisible = false
        binding.tvEmpty.isVisible = list.isEmpty()

        if (list.isEmpty()) {
            val filtering = query.isNotEmpty() || status != null
            binding.tvEmpty.text = when {
                filtering -> "Tidak ada hasil. Coba ubah kata kunci atau filter."
                isDosen -> "Belum ada mahasiswa bimbingan yang terdaftar."
                else -> "Belum ada skripsi. Ketuk tombol + untuk menambahkan judul."
            }
        }
    }

    private fun handleError(error: Resource.Error, showFullError: Boolean) {
        if (error.code == 401) {
            requireContext().redirectToLogin("Sesi berakhir, silakan login kembali")
            return
        }

        val binding = _b ?: return
        if (showFullError || adapter.itemCount == 0) {
            binding.swipe.isVisible = false
            binding.layoutError.isVisible = true
            binding.tvError.text = error.message
        } else {
            Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDelete(thesis: Thesis) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus skripsi?")
            .setMessage(
                "\"${thesis.title}\"\n\nSeluruh tahapan, unggahan, dan komentar ikut terhapus. Tindakan ini tidak dapat dibatalkan."
            )
            .setPositiveButton("Hapus") { _, _ -> delete(thesis) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun delete(thesis: Thesis) {
        viewLifecycleOwner.lifecycleScope.launch {
            _b?.swipe?.isRefreshing = true
            val result = safeApiCall { RetrofitClient.api.deleteThesis(thesis.id) }

            when (result) {
                is Resource.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Skripsi berhasil dihapus.",
                        Toast.LENGTH_SHORT
                    ).show()
                    load(full = false)
                }

                is Resource.Error -> {
                    _b?.swipe?.isRefreshing = false
                    handleError(result, showFullError = false)
                }

                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        requestId++
        searchJob?.cancel()
        requestJob?.cancel()
        searchJob = null
        requestJob = null
        _b = null
        super.onDestroyView()
    }
}
