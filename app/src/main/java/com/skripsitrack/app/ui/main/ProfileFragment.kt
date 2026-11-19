package com.skripsitrack.app.ui.main

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.FragmentProfileBinding
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentProfileBinding.inflate(inflater, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val s = SkripsiApp.session
        b.tvName.text = s.getName()
        b.tvEmail.text = s.getEmail()
        b.tvRole.text = s.getRole().replaceFirstChar { it.uppercase() }

        b.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Yakin ingin keluar?")
                .setPositiveButton("Ya") { _, _ -> doLogout() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun doLogout() {
        val act = requireActivity() as MainActivity
        act.lifecycleScope.launch {
            safeApiCall { RetrofitClient.api.logout() }   // gagal pun tetap keluar
            act.forceLogout()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}