package com.skripsitrack.app.ui.main

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skripsitrack.app.data.model.ActivityLog
import com.skripsitrack.app.databinding.ItemActivityBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.activityActionStyle

class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.VH>() {

    private var items: List<ActivityLog> = emptyList()

    fun submit(list: List<ActivityLog>) {
        items = list.distinctBy { it.id }
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemActivityBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            ItemActivityBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val activity = items[position]
        val description = activity.description.ifBlank { "Aktivitas skripsi" }
        val actor = activity.user?.name?.takeIf { it.isNotBlank() } ?: "Pengguna"
        val timestamp = DateUtil.formatDateTime(activity.created_at)
        val (icon, foreground, background) =
            activityActionStyle(activity.action, description)

        holder.b.ivIcon.setImageResource(icon)
        holder.b.ivIcon.imageTintList = ColorStateList.valueOf(foreground)
        holder.b.ivIconBg.backgroundTintList = ColorStateList.valueOf(background)
        holder.b.tvDesc.text = description
        holder.b.tvMeta.text = "$actor  •  $timestamp"
        holder.b.root.contentDescription =
            "$description. Dilakukan oleh $actor. $timestamp"
    }

    override fun getItemCount(): Int = items.size

    fun append(list: List<ActivityLog>) {
        val knownIds = items.mapTo(mutableSetOf()) { it.id }
        val newItems = list.filter { knownIds.add(it.id) }
        if (newItems.isEmpty()) return

        val start = items.size
        items = items + newItems
        notifyItemRangeInserted(start, newItems.size)
    }
}
