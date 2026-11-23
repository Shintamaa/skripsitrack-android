package com.skripsitrack.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.FragmentActivityHistoryBinding
import com.skripsitrack.app.util.Resource
import com.skripsitrack.app.util.redirectToLogin
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ActivityHistoryFragment : Fragment() {

    private var _b: FragmentActivityHistoryBinding? = null
    private val b get() = _b!!

    private val adapter = ActivityHistoryAdapter()

    private var page = 1
    private var lastPage = 1
    private var isLoading = false
    private var action: String? = null
    private var requestId = 0
    private var requestJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentActivityHistoryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(requireContext())
        b.rv.layoutManager = layoutManager
        b.rv.adapter = adapter

        b.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || isLoading || page >= lastPage || adapter.itemCount == 0) {
                    return
                }

                if (layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    load(reset = false, full = false)
                }
            }
        })

        b.swipe.setOnRefreshListener {
            load(reset = true, full = false)
        }

        b.btnRetry.setOnClickListener {
            load(reset = true, full = true)
        }

        b.btnRetryMore.setOnClickListener {
            load(reset = false, full = false)
        }

        b.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedAction = when (checkedIds.firstOrNull()) {
                b.chipUpload.id -> "upload"
                b.chipStatus.id -> "ubah_status"
                b.chipComment.id -> "komentar"
                b.chipDelete.id -> "hapus"
                else -> null
            }

            if (selectedAction != action) {
                action = selectedAction
                page = 1
                lastPage = 1
                adapter.submit(emptyList())
                b.tvEmpty.isVisible = false
                load(reset = true, full = true)
            }
        }

        load(reset = true, full = true)
    }

    private fun load(reset: Boolean, full: Boolean) {
        val currentBinding = _b ?: return

        if (!reset && (isLoading || page >= lastPage)) return

        if (reset) requestJob?.cancel()

        val id = ++requestId
        val targetPage = if (reset) 1 else page + 1
        val selectedAction = action

        isLoading = true
        currentBinding.layoutError.isVisible = false
        currentBinding.layoutMoreError.isVisible = false
        currentBinding.tvEmpty.isVisible = false

        when {
            full -> {
                currentBinding.layoutLoading.isVisible = true
                currentBinding.swipe.isRefreshing = false
                currentBinding.swipe.isVisible = false
            }
            reset -> {
                currentBinding.layoutLoading.isVisible = false
                currentBinding.swipe.isVisible = true
                currentBinding.swipe.isRefreshing = true
            }
            else -> {
                currentBinding.progressMore.isVisible = true
            }
        }

        requestJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = safeApiCall {
                RetrofitClient.api.activities(targetPage, selectedAction)
            }

            // Respons dari filter/refresh lama tidak boleh mengubah layar terbaru.
            if (id != requestId || _b == null) return@launch

            val binding = _b ?: return@launch
            isLoading = false
            binding.layoutLoading.isVisible = false
            binding.progressMore.isVisible = false
            binding.swipe.isRefreshing = false
            binding.swipe.isVisible = true

            when (result) {
                is Resource.Success -> {
                    val pagination = result.data.data
                    if (pagination == null) {
                        handleLoadError(reset, "Data riwayat belum dapat dibaca.")
                        return@launch
                    }

                    page = pagination.current_page.coerceAtLeast(1)
                    lastPage = pagination.last_page.coerceAtLeast(page)

                    if (reset) adapter.submit(pagination.data)
                    else adapter.append(pagination.data)

                    val isEmpty = adapter.itemCount == 0
                    binding.tvEmpty.isVisible = isEmpty
                    if (isEmpty) {
                        binding.tvEmpty.text = if (selectedAction != null) {
                            "Tidak ada aktivitas untuk filter ini."
                        } else {
                            "Belum ada aktivitas. Aktivitas skripsimu akan muncul di sini."
                        }
                    }
                }

                is Resource.Error -> {
                    if (result.code == 401) {
                        requireContext().redirectToLogin(
                            "Sesi berakhir, silakan login kembali"
                        )
                    } else {
                        handleLoadError(reset, result.message)
                    }
                }

                else -> Unit
            }
        }
    }

    private fun handleLoadError(reset: Boolean, message: String) {
        val binding = _b ?: return

        if (!reset) {
            binding.progressMore.isVisible = false
            binding.layoutMoreError.isVisible = true
            binding.tvMoreError.text = "Gagal memuat aktivitas berikutnya."
            return
        }

        if (adapter.itemCount == 0) {
            showError(message)
        } else {
            // Data yang sudah tampil tidak dihapus jika pull-to-refresh gagal.
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showError(message: String) {
        val binding = _b ?: return
        binding.layoutLoading.isVisible = false
        binding.progressMore.isVisible = false
        binding.layoutMoreError.isVisible = false
        binding.swipe.isRefreshing = false
        binding.swipe.isVisible = false
        binding.tvEmpty.isVisible = false
        binding.layoutError.isVisible = true
        binding.tvError.text = message
    }

    override fun onDestroyView() {
        requestId++
        requestJob?.cancel()
        requestJob = null
        isLoading = false
        _b = null
        super.onDestroyView()
    }
}
