package com.skripsitrack.app.ui.main

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlaceholderFragment : Fragment() {
    companion object {
        fun newInstance(title: String, note: String) = PlaceholderFragment().apply {
            arguments = Bundle().apply { putString("title", title); putString("note", note) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        return TextView(requireContext()).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            text = "${arguments?.getString("title")}\n${arguments?.getString("note")}"
        }
    }
}