package com.skripsitrack.app.ui.main

import android.os.Bundle
import android.view.*
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
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!
    private val adapter = ActivityAdapter()

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentDashboardBinding.inflate(inflater, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.rvActivity.layoutManager = LinearLayoutManager(requireContext())
        b.rvActivity.adapter = adapter
        b.tvGreeting.text = "Halo, ${SkripsiApp.session.getName()}"

        b.swipe.setOnRefreshListener { load(firstLoad = false) }
        b.btnRetry.setOnClickListener { load(firstLoad = true) }
        load(firstLoad = true)
    }

    private fun load(firstLoad: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (firstLoad) showState(loading = true)

            val result = safeApiCall { RetrofitClient.api.dashboard() }
            b.swipe.isRefreshing = false

            when (result) {
                is Resource.Success -> bind(result.data.data)
                is Resource.Error -> {
                    if (result.code == 401) {
                        (requireActivity() as MainActivity)
                            .forceLogout("Sesi berakhir, silakan login kembali")
                    } else if (firstLoad) {
                        showState(error = result.message)
                    } else {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
                else -> {}
            }
        }
    }

    private fun showState(loading: Boolean = false, error: String? = null) {
        b.progressLoading.isVisible = loading
        b.layoutError.isVisible = error != null
        b.swipe.isVisible = !loading && error == null
        error?.let { b.tvError.text = it }
    }

    private fun bind(d: DashboardData?) {
        if (d == null) { showState(error = "Data dashboard kosong."); return }
        showState()

        val isDosen = SkripsiApp.session.getRole() == "dosen"

        // Data kosong: belum ada skripsi
        val noThesis = d.total_skripsi == 0
        b.cardProgress.isVisible = !noThesis
        b.tvEmptyThesis.isVisible = noThesis
        b.tvEmptyThesis.text = if (isDosen)
            "Belum ada mahasiswa bimbingan."
        else
            "Anda belum memiliki skripsi. Buat skripsi di tab Skripsi."

        b.cpiProgress.setProgressCompat(d.progress_persen, true)
        b.tvPercent.text = "${d.progress_persen}%"
        b.tvProgressLabel.text = if (isDosen) "Rata-rata progres bimbingan" else "Progres skripsi Anda"
        b.tvStageCount.text = "${d.tahapan_selesai} dari ${d.total_tahapan} tahapan selesai"

        b.tvWaiting.text = d.menunggu_review.toString()
        b.tvRevision.text = d.perlu_revisi.toString()

        val dl = d.deadline_terdekat
        if (dl == null) {
            b.tvDeadlineStage.text = "Tidak ada deadline"
            b.tvDeadlineDate.text = "-"
        } else {
            b.tvDeadlineStage.text = dl.name
            val days = DateUtil.daysUntil(dl.target_date)
            val info = when {
                days == null -> ""
                days < 0 -> "  (terlambat ${-days} hari)"
                days == 0L -> "  (hari ini)"
                else -> "  ($days hari lagi)"
            }
            b.tvDeadlineDate.text = DateUtil.formatDate(dl.target_date) + info
        }

        adapter.submit(d.aktivitas_terbaru)
        b.tvEmptyActivity.isVisible = d.aktivitas_terbaru.isEmpty()
        b.rvActivity.isVisible = d.aktivitas_terbaru.isNotEmpty()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}