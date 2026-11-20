package com.skripsitrack.app.ui.thesis

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.skripsitrack.app.data.model.Thesis
import com.skripsitrack.app.databinding.ItemThesisBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.thesisStatusStyle

class ThesisAdapter(
    private val isDosen: Boolean,
    private val onClick: (Thesis) -> Unit,
    private val onEdit: (Thesis) -> Unit,
    private val onDelete: (Thesis) -> Unit
) : RecyclerView.Adapter<ThesisAdapter.VH>() {

    private var items: List<Thesis> = emptyList()

    fun submit(list: List<Thesis>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemThesisBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemThesisBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val t = items[position]
        val b = h.b

        b.tvTitle.text = t.title
        b.tvSub.text = if (isDosen)
            "${t.student?.name ?: "-"}  •  ${t.student?.nim_nidn ?: "-"}"
        else
            "Pembimbing: ${t.supervisor?.name ?: "-"}"

        val (label, textColor, bgColor) = thesisStatusStyle(t.status)
        b.tvStatus.text = label
        b.tvStatus.setTextColor(textColor)
        b.tvStatus.setBackgroundColor(bgColor)

        val total = t.stages_count ?: 0
        val done = t.done_stages_count ?: 0
        val percent = if (total > 0) done * 100 / total else 0
        b.lpi.setProgressCompat(percent, false)
        b.tvProgress.text = "$done/$total tahapan selesai ($percent%)"
        b.tvTarget.text = "Target: ${DateUtil.formatDate(t.target_date)}"

        b.root.setOnClickListener { onClick(t) }

        // Hanya mahasiswa pemilik yang boleh mengubah/menghapus
        b.btnMenu.isVisible = !isDosen
        b.btnMenu.setOnClickListener { v ->
            PopupMenu(v.context, v).apply {
                menu.add("Ubah")
                menu.add("Hapus")
                setOnMenuItemClickListener {
                    if (it.title == "Ubah") onEdit(t) else onDelete(t)
                    true
                }
                show()
            }
        }
    }

    override fun getItemCount() = items.size
}