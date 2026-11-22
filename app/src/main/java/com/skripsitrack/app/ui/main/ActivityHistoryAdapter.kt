package com.skripsitrack.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.res.ColorStateList
import androidx.recyclerview.widget.RecyclerView
import com.skripsitrack.app.data.model.ActivityLog
import com.skripsitrack.app.databinding.ItemActivityBinding
import com.skripsitrack.app.databinding.ItemActivityDayHeaderBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.activityActionStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Adapter khusus riwayat. Aktivitas dikelompokkan berdasarkan tanggal lokal,
 * sementara ActivityAdapter tetap dipakai untuk daftar aktivitas ringkas di Dashboard.
 */
class ActivityHistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class DayHeader(val key: String, val title: String) : Row()
        data class Entry(val activity: ActivityLog) : Row()
    }

    private var activities: List<ActivityLog> = emptyList()
    private var rows: List<Row> = emptyList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ACTIVITY = 1
    }

    fun submit(list: List<ActivityLog>) {
        activities = list.distinctBy { it.id }
        rebuildRows()
    }

    fun append(list: List<ActivityLog>) {
        if (list.isEmpty()) return
        val knownIds = activities.mapTo(mutableSetOf()) { it.id }
        val newItems = list.filter { knownIds.add(it.id) }
        if (newItems.isEmpty()) return
        activities = activities + newItems
        rebuildRows()
    }

    private fun rebuildRows() {
        val result = mutableListOf<Row>()
        var previousKey: String? = null

        activities.forEach { activity ->
            val (key, label) = dayGroup(activity.created_at)
            if (key != previousKey) {
                result += Row.DayHeader(key, label)
                previousKey = key
            }
            result += Row.Entry(activity)
        }

        rows = result
        notifyDataSetChanged()
    }

    private fun dayGroup(iso: String?): Pair<String, String> {
        val date = parseUtcDate(iso)
            ?: return "unknown" to "Tanggal tidak diketahui"

        val itemDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val yesterday = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(itemDay.time)
        val title = when {
            itemDay.timeInMillis == today.timeInMillis -> "Hari ini"
            itemDay.timeInMillis == yesterday.timeInMillis -> "Kemarin"
            else -> SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
                .format(itemDay.time)
        }
        return key to title
    }

    private fun parseUtcDate(value: String?): Date? {
        if (value.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(value.take(19))
        } catch (_: Exception) {
            null
        }
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.DayHeader -> TYPE_HEADER
        is Row.Entry -> TYPE_ACTIVITY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemActivityDayHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderVH(binding)
        } else {
            val binding = ItemActivityBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ActivityVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.DayHeader -> (holder as HeaderVH).bind(row)
            is Row.Entry -> (holder as ActivityVH).bind(row.activity)
        }
    }

    override fun getItemCount(): Int = rows.size

    private class HeaderVH(
        private val binding: ItemActivityDayHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: Row.DayHeader) {
            binding.tvDayHeader.text = row.title
            binding.tvDayHeader.contentDescription = "Aktivitas ${row.title}"
        }
    }

    private class ActivityVH(
        private val binding: ItemActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: ActivityLog) {
            val description = activity.description.ifBlank { "Aktivitas skripsi" }
            val actor = activity.user?.name?.takeIf { it.isNotBlank() } ?: "Pengguna"
            val (icon, foreground, background) =
                activityActionStyle(activity.action, description)

            binding.ivIcon.setImageResource(icon)
            binding.ivIcon.imageTintList = ColorStateList.valueOf(foreground)
            binding.ivIconBg.backgroundTintList = ColorStateList.valueOf(background)
            binding.tvDesc.text = description
            binding.tvMeta.text =
                "$actor  •  ${DateUtil.formatDateTime(activity.created_at)}"
            binding.root.contentDescription =
                "$description. Dilakukan oleh $actor. ${DateUtil.formatDateTime(activity.created_at)}"
        }
    }
}
