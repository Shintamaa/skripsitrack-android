package com.skripsitrack.app.ui.thesis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.skripsitrack.app.data.model.Submission
import com.skripsitrack.app.databinding.ItemSubmissionBinding
import com.skripsitrack.app.util.DateUtil
import com.skripsitrack.app.util.submissionStatusStyle

class SubmissionAdapter(private val onOpen: (Submission) -> Unit) :
    RecyclerView.Adapter<SubmissionAdapter.VH>() {

    private var items: List<Submission> = emptyList()

    fun submit(list: List<Submission>) { items = list; notifyDataSetChanged() }

    inner class VH(val b: ItemSubmissionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val s = items[position]
        val b = h.b

        b.tvVersion.text = "v${s.version}"
        val (label, textColor, bgColor) = submissionStatusStyle(s.status)
        b.tvStatus.text = label
        b.tvStatus.setTextColor(textColor)
        b.tvStatus.setBackgroundColor(bgColor)
        b.tvDate.text = DateUtil.formatDateTime(s.created_at)

        val isImage = s.file_type == "image"
        b.ivPreview.isVisible = isImage
        b.tvFile.isVisible = !isImage
        if (isImage) {
            Glide.with(b.ivPreview).load(s.file_url).centerCrop().into(b.ivPreview)
        } else {
            b.tvFile.text = "Berkas ${s.file_type.uppercase()}  •  ketuk untuk membuka"
        }

        b.tvNote.isVisible = !s.note.isNullOrBlank()
        b.tvNote.text = s.note

        b.card.setOnClickListener { onOpen(s) }
    }

    override fun getItemCount() = items.size
}