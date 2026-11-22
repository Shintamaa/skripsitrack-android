package com.skripsitrack.app.ui.thesis

import com.skripsitrack.app.util.setPillColor
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.skripsitrack.app.data.model.ThesisStage
import com.skripsitrack.app.databinding.ItemStageBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.stageStatusStyle

class StageAdapter(private val onClick: (ThesisStage) -> Unit) :
    RecyclerView.Adapter<StageAdapter.VH>() {

    private var items: List<ThesisStage> = emptyList()

    fun submit(list: List<ThesisStage>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemStageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val s = items[position]
        val b = h.b
        val (label, textColor, bgColor) = stageStatusStyle(s.status)

        b.tvName.text = s.name
        b.tvStatus.text = label
        b.tvStatus.setTextColor(textColor)
        b.tvStatus.setPillColor(bgColor)

        b.viewDot.backgroundTintList = ColorStateList.valueOf(textColor)
        b.viewLine.visibility = if (position == items.size - 1) View.INVISIBLE else View.VISIBLE

        b.tvTarget.text = "Target: ${DateUtil.formatDate(s.target_date)}"

        val info = if (s.status == "selesai" && s.completed_at != null)
            "Selesai pada ${DateUtil.formatDate(s.completed_at)}"
        else DateUtil.deadlineInfo(s.status, s.target_date)
        b.tvInfo.isVisible = info != null
        b.tvInfo.text = info
        b.tvInfo.setTextColor(
            if (info?.startsWith("Terlambat") == true) Color.parseColor("#C62828")
            else Color.parseColor("#757575")
        )

        val n = s.submissions_count ?: 0
        b.tvVersions.text = if (n == 0) "Belum ada unggahan" else "$n versi unggahan"
        b.card.contentDescription =
            "${s.name}. $label. Target ${DateUtil.formatDate(s.target_date)}. " +
                (info ?: "Tidak ada informasi deadline tambahan.")

        b.card.setOnClickListener { onClick(s) }
    }

    override fun getItemCount() = items.size
}