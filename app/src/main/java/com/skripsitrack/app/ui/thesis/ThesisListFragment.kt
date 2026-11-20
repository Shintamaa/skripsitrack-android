package com.skripsitrack.app.ui.thesis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
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
    private var query = ""
    private var status: String? = null

    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) load(full = true)
        }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentThesisListBinding.inflate(inflater, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ThesisAdapter(
            isDosen,
            onClick = {
                Toast.makeText(requireContext(),
                    "Detail dan timeline dibuat di Bagian 4", Toast.LENGTH_SHORT).show()
            },
            onEdit = { openForm(it) },
            onDelete = { confirmDelete(it) }
        )
        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        // Hanya mahasiswa yang boleh menambah skripsi
        b.fabAdd.isVisible = !isDosen
        b.fabAdd.setOnClickListener { openForm(null) }

        b.swipe.setOnRefreshListener { load(full = false) }
        b.btnRetry.setOnClickListener { load(full = true) }

        // Pencarian dengan jeda 450 ms agar tidak memanggil API di setiap ketikan
        b.etSearch.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(450)
                query = text.toString().trim()
                load(full = false)
            }
        }

        b.chipGroup.setOnCheckedStateChangeListener { _, ids ->
            status = when (ids.firstOrNull()) {
                b.chipBerjalan.id -> "berjalan"
                b.chipSelesai.id -> "selesai"
                b.chipDitunda.id -> "ditunda"
                else -> null
            }
            load(full = false)
        }

        load(full = true)
    }

    private fun openForm(t: Thesis?) {
        val i = Intent(requireContext(), ThesisFormActivity::class.java)
        t?.let { i.putExtra("thesis", Gson().toJson(it)) }
        formLauncher.launch(i)
    }

    private fun load(full: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            b.layoutError.isVisible = false
            if (full) {
                b.progressLoading.isVisible = true
                b.tvEmpty.isVisible = false
                b.swipe.isVisible = false
            } else {
                b.swipe.isRefreshing = true
            }

            val result = safeApiCall {
                RetrofitClient.api.theses(query.ifEmpty { null }, status)
            }

            b.progressLoading.isVisible = false
            b.swipe.isRefreshing = false
            b.swipe.isVisible = true

            when (result) {
                is Resource.Success -> showList(result.data.data.orEmpty())
                is Resource.Error -> handleError(result, showFullError = full)
                else -> {}
            }
        }
    }

    private fun showList(list: List<Thesis>) {
        adapter.submit(list)
        b.tvEmpty.isVisible = list.isEmpty()
        if (list.isEmpty()) {
            val filtering = query.isNotEmpty() || status != null
            b.tvEmpty.text = when {
                filtering -> "Tidak ada hasil untuk pencarian atau filter ini."
                isDosen -> "Belum ada mahasiswa bimbingan."
                else -> "Belum ada skripsi.\nKetuk tombol + untuk menambahkan."
            }
        }
    }

    private fun handleError(e: Resource.Error, showFullError: Boolean) {
        if (e.code == 401) {
            requireContext().redirectToLogin("Sesi berakhir, silakan login kembali")
            return
        }
        if (showFullError) {
            b.swipe.isVisible = false
            b.layoutError.isVisible = true
            b.tvError.text = e.message
        } else {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDelete(t: Thesis) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus skripsi?")
            .setMessage("\"${t.title}\"\n\nSeluruh tahapan, unggahan, dan komentar ikut terhapus. Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ -> delete(t) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun delete(t: Thesis) {
        viewLifecycleOwner.lifecycleScope.launch {
            b.swipe.isRefreshing = true
            val r = safeApiCall { RetrofitClient.api.deleteThesis(t.id) }
            when (r) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Skripsi dihapus", Toast.LENGTH_SHORT).show()
                    load(full = false)
                }
                is Resource.Error -> {
                    b.swipe.isRefreshing = false
                    handleError(r, showFullError = false)
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}