package com.skripsitrack.app.ui.main

import android.os.Bundle
import android.view.*
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
import kotlinx.coroutines.launch

class ActivityHistoryFragment : Fragment() {
    private var _b: FragmentActivityHistoryBinding? = null
    private val b get() = _b!!
    private val adapter = ActivityAdapter()

    private var page = 1
    private var lastPage = 1
    private var isLoading = false
    private var action: String? = null
    private var requestId = 0

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentActivityHistoryBinding.inflate(inflater, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lm = LinearLayoutManager(requireContext())
        b.rv.layoutManager = lm
        b.rv.adapter = adapter

        // Paginasi: muat halaman berikutnya saat mendekati akhir daftar
        b.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || isLoading || page >= lastPage) return
                if (lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    load(reset = false, full = false)
                }
            }
        })

        b.swipe.setOnRefreshListener { load(reset = true, full = false) }
        b.btnRetry.setOnClickListener { load(reset = true, full = true) }

        b.chipGroup.setOnCheckedStateChangeListener { _, ids ->
            action = when (ids.firstOrNull()) {
                b.chipUpload.id -> "upload"
                b.chipStatus.id -> "ubah_status"
                b.chipComment.id -> "komentar"
                b.chipDelete.id -> "hapus"
                else -> null
            }
            load(reset = true, full = false)
        }

        load(reset = true, full = true)
    }

    private fun load(reset: Boolean, full: Boolean) {
        val id = ++requestId
        val target = if (reset) 1 else page + 1

        viewLifecycleOwner.lifecycleScope.launch {
            isLoading = true
            b.layoutError.isVisible = false
            when {
                full -> { b.progressLoading.isVisible = true; b.swipe.isVisible = false; b.tvEmpty.isVisible = false }
                reset -> b.swipe.isRefreshing = true
                else -> b.progressMore.isVisible = true
            }

            val r = safeApiCall { RetrofitClient.api.activities(target, action) }

            if (id != requestId) return@launch      // hasil basi (filter sudah berganti)
            isLoading = false
            b.progressLoading.isVisible = false
            b.progressMore.isVisible = false
            b.swipe.isRefreshing = false
            b.swipe.isVisible = true

            when (r) {
                is Resource.Success -> {
                    val p = r.data.data
                    if (p == null) { showError(reset, "Data riwayat kosong."); return@launch }
                    page = p.current_page
                    lastPage = p.last_page
                    if (reset) adapter.submit(p.data) else adapter.append(p.data)

                    b.tvEmpty.isVisible = adapter.itemCount == 0
                    if (adapter.itemCount == 0) {
                        b.tvEmpty.text = if (action != null)
                            "Tidak ada aktivitas untuk filter ini."
                        else "Belum ada aktivitas."
                    }
                }
                is Resource.Error -> {
                    if (r.code == 401) requireContext()
                        .redirectToLogin("Sesi berakhir, silakan login kembali")
                    else showError(reset && adapter.itemCount == 0, r.message)
                }
                else -> {}
            }
        }
    }

    private fun showError(asScreen: Boolean, msg: String) {
        if (asScreen) {
            b.swipe.isVisible = false
            b.layoutError.isVisible = true
            b.tvError.text = msg
        } else {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}