package com.skripsitrack.app.ui.thesis

import com.skripsitrack.app.util.setPillColor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.skripsitrack.app.data.model.Submission
import com.skripsitrack.app.databinding.ItemCommentBinding
import com.skripsitrack.app.databinding.ItemSubmissionBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.submissionStatusStyle

class SubmissionAdapter(
    private val isDosen: Boolean,
    private val cb: Callbacks
) : RecyclerView.Adapter<SubmissionAdapter.VH>() {

    interface Callbacks {
        fun onOpen(s: Submission)
        fun onComment(s: Submission)
        fun onApprove(s: Submission)
        fun onRevise(s: Submission)
        fun onDelete(s: Submission)
        fun onSelectionChanged(count: Int)
    }

    private var items: List<Submission> = emptyList()
    private val selected = mutableSetOf<Int>()
    var selectionMode = false
        private set
    private var interactionEnabled = true

    fun submit(list: List<Submission>) {
        items = list
        selected.retainAll(list.map { it.id }.toSet())
        notifyDataSetChanged()
    }

    fun setSelectionMode(on: Boolean) {
        selectionMode = on
        if (!on) selected.clear()
        notifyDataSetChanged()
        cb.onSelectionChanged(selected.size)
    }

    fun selectAll() {
        selected.clear()
        selected.addAll(items.filter { it.status != "disetujui" }.map { it.id })
        notifyDataSetChanged()
        cb.onSelectionChanged(selected.size)
    }

    fun selectedIds(): List<Int> = selected.toList()

    fun setInteractionEnabled(enabled: Boolean) {
        interactionEnabled = enabled
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemSubmissionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val s = items[position]
        val b = h.b
        val ctx = b.root.context
        val approved = s.status == "disetujui"
        val latestId = items.maxByOrNull { it.version }?.id

        // Header
        b.tvVersion.text = "v${s.version}"
        val (label, textColor, bgColor) = submissionStatusStyle(s.status)
        b.tvStatus.text = label
        b.tvStatus.setTextColor(textColor)
        b.tvStatus.setPillColor(bgColor)
        b.tvDate.text = DateUtil.formatDateTime(s.created_at)

        // Pratinjau berkas
        val isImage = s.file_type == "image"
        b.ivPreview.isVisible = isImage
        b.tvFile.isVisible = !isImage
        if (isImage) Glide.with(b.ivPreview).load(s.file_url).centerCrop().into(b.ivPreview)
        else b.tvFile.text = "Berkas ${s.file_type.uppercase()}  •  ketuk untuk membuka"

        b.tvNote.isVisible = !s.note.isNullOrBlank()
        b.tvNote.text = s.note
        b.card.contentDescription =
            "Versi ${s.version}. Status $label. ${DateUtil.formatDateTime(s.created_at)}. " +
                (s.note?.takeIf { it.isNotBlank() } ?: "Tidak ada catatan.")

        // Komentar (urut dari yang terlama)
        b.llComments.removeAllViews()
        val comments = s.comments.orEmpty().sortedWith(compareBy({ it.created_at }, { it.id }))
        b.llComments.isVisible = comments.isNotEmpty()
        comments.forEach { c ->
            val cbind = ItemCommentBinding.inflate(LayoutInflater.from(ctx), b.llComments, false)
            val role = if (c.user?.role == "dosen") "Dosen" else "Mahasiswa"
            cbind.tvAuthor.text = "${c.user?.name ?: "-"} ($role)  •  ${DateUtil.formatDateTime(c.created_at)}"
            cbind.tvBody.text = c.body
            b.llComments.addView(cbind.root)
        }

        // Tombol aksi sesuai role dan kondisi
        val reviewable = isDosen && s.id == latestId && !approved
        val deletable = !isDosen && !approved
        b.btnComment.isVisible = !selectionMode
        b.btnComment.isEnabled = interactionEnabled
        b.btnApprove.isVisible = !selectionMode && reviewable
        b.btnApprove.isEnabled = interactionEnabled
        b.btnRevise.isVisible = !selectionMode && reviewable && s.status == "menunggu"
        b.btnRevise.isEnabled = interactionEnabled
        b.btnDelete.isVisible = !selectionMode && deletable
        b.btnDelete.isEnabled = interactionEnabled
        b.layoutActions.isVisible = !selectionMode

        b.btnComment.setOnClickListener { cb.onComment(s) }
        b.btnApprove.setOnClickListener { cb.onApprove(s) }
        b.btnRevise.setOnClickListener { cb.onRevise(s) }
        b.btnDelete.setOnClickListener { cb.onDelete(s) }

        // Mode pilih banyak
        b.cbSelect.isVisible = selectionMode && !approved
        b.cbSelect.isChecked = selected.contains(s.id)

        b.card.setOnClickListener {
            if (!interactionEnabled) return@setOnClickListener
            if (!selectionMode) {
                cb.onOpen(s)
            } else if (approved) {
                Toast.makeText(ctx, "Unggahan yang sudah disetujui tidak dapat dihapus",
                    Toast.LENGTH_SHORT).show()
            } else {
                if (!selected.add(s.id)) selected.remove(s.id)
                val adapterPosition = h.bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(adapterPosition)
                }
                cb.onSelectionChanged(selected.size)
            }
        }
    }

    override fun getItemCount() = items.size
}