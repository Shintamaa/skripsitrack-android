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
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.model.DashboardData
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.FragmentDashboardBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.Resource
import com.skripsitrack.app.util.initialsOf
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!

    private val adapter = ActivityAdapter()
    private var loadJob: Job? = null
    private var requestId = 0
    private var hasLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.rvActivity.layoutManager = LinearLayoutManager(requireContext())
        b.rvActivity.adapter = adapter

        val userName = SkripsiApp.session.getName()
        b.tvGreeting.text = userName
        b.tvAvatar.text = initialsOf(userName)

        b.swipe.setOnRefreshListener { load(firstLoad = false) }
        b.btnRetry.setOnClickListener { load(firstLoad = true) }

        load(firstLoad = true)
    }

    private fun load(firstLoad: Boolean) {
        loadJob?.cancel()
        val id = ++requestId

        if (firstLoad) {
            showState(loading = true)
        } else {
            _b?.swipe?.isRefreshing = true
        }

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = safeApiCall { RetrofitClient.api.dashboard() }

            if (id != requestId || _b == null) return@launch

            val binding = _b ?: return@launch
            binding.swipe.isRefreshing = false

            when (result) {
                is Resource.Success -> {
                    val dashboard = result.data.data
                    if (dashboard == null) {
                        hasLoaded = false
                        showState(error = "Data dashboard belum dapat dibaca.")
                    } else {
                        hasLoaded = true
                        bind(dashboard)
                    }
                }

                is Resource.Error -> {
                    if (result.code == 401) {
                        (requireActivity() as MainActivity)
                            .forceLogout("Sesi berakhir, silakan login kembali")
                    } else if (firstLoad || !hasLoaded) {
                        showState(error = result.message)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                else -> Unit
            }
        }
    }

    private fun showState(loading: Boolean = false, error: String? = null) {
        val binding = _b ?: return
        binding.layoutLoading.isVisible = loading
        binding.layoutError.isVisible = error != null
        binding.swipe.isVisible = !loading && error == null
        if (error != null) binding.tvError.text = error
    }

    private fun bind(data: DashboardData) {
        val binding = _b ?: return
        showState()

        val isDosen = SkripsiApp.session.getRole() == "dosen"
        val noThesis = data.total_skripsi == 0

        binding.cardProgress.isVisible = !noThesis
        binding.tvEmptyThesis.isVisible = noThesis
        binding.tvEmptyThesis.text = if (isDosen) {
            "Belum ada mahasiswa bimbingan. Daftar bimbingan akan muncul di sini."
        } else {
            "Anda belum memiliki skripsi. Buka tab Skripsi untuk menambahkan judul."
        }

        val percent = data.progress_persen.coerceIn(0, 100)
        binding.cpiProgress.setProgressCompat(percent, true)
        binding.tvPercent.text = "$percent%"
        binding.tvProgressLabel.text =
            if (isDosen) "Rata-rata progres bimbingan" else "Progres skripsi Anda"
        binding.tvStageCount.text =
            "${data.tahapan_selesai} dari ${data.total_tahapan} tahapan selesai"

        binding.tvWaiting.text = data.menunggu_review.coerceAtLeast(0).toString()
        binding.tvRevision.text = data.perlu_revisi.coerceAtLeast(0).toString()

        val deadline = data.deadline_terdekat
        if (deadline == null) {
            binding.tvDeadlineStage.text = "Tidak ada deadline"
            binding.tvDeadlineDate.text = "Belum ada tahapan dengan target tanggal."
        } else {
            binding.tvDeadlineStage.text = deadline.name
            val days = DateUtil.daysUntil(deadline.target_date)
            val info = when {
                days == null -> ""
                days < 0 -> " • terlambat ${-days} hari"
                days == 0L -> " • hari ini"
                else -> " • $days hari lagi"
            }
            binding.tvDeadlineDate.text = DateUtil.formatDate(deadline.target_date) + info
        }

        adapter.submit(data.aktivitas_terbaru)
        val hasActivity = data.aktivitas_terbaru.isNotEmpty()
        binding.tvEmptyActivity.isVisible = !hasActivity
        binding.rvActivity.isVisible = hasActivity
        if (!hasActivity) {
            binding.tvEmptyActivity.text = "Belum ada aktivitas terbaru."
        }
    }

    override fun onDestroyView() {
        requestId++
        loadJob?.cancel()
        loadJob = null
        _b = null
        super.onDestroyView()
    }
}
