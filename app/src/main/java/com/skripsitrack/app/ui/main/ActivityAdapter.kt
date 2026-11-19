package com.skripsitrack.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skripsitrack.app.data.model.ActivityLog
import com.skripsitrack.app.databinding.ItemActivityBinding
import com.skripsitrack.app.util.DateUtil

class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.VH>() {
    private var items: List<ActivityLog> = emptyList()

    fun submit(list: List<ActivityLog>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemActivityBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val a = items[position]
        h.b.tvAction.text = DateUtil.actionLabel(a.action).uppercase()
        h.b.tvDesc.text = a.description
        h.b.tvMeta.text = "${a.user?.name ?: "-"}  •  ${DateUtil.formatDateTime(a.created_at)}"
    }

    override fun getItemCount() = items.size
}