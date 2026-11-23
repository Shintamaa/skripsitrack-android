package com.skripsitrack.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.FragmentProfileBinding
import com.skripsitrack.app.util.initialsOf
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!

    private var logoutInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SkripsiApp.session
        val name = session.getName()
        val email = session.getEmail()
        val role = session.getRole()

        b.tvName.text = name.ifBlank { "Pengguna SkripsiTrack" }
        b.tvEmail.text = email
        b.tvEmailInfo.text = email
        b.tvAvatar.text = initialsOf(name)
        b.tvRole.text = when (role.lowercase()) {
            "mahasiswa" -> "Mahasiswa"
            "dosen" -> "Dosen"
            else -> role.replaceFirstChar { it.uppercase() }
        }

        b.progressLogout.isVisible = false
        b.btnLogout.isEnabled = true
        b.btnLogout.setOnClickListener {
            if (logoutInProgress) return@setOnClickListener

            AlertDialog.Builder(requireContext())
                .setTitle("Keluar dari akun?")
                .setMessage("Kamu perlu masuk kembali untuk mengakses data skripsi.")
                .setPositiveButton("Ya, keluar") { _, _ -> doLogout() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun doLogout() {
        if (logoutInProgress || !isAdded) return
        val mainActivity = activity as? MainActivity ?: return

        logoutInProgress = true
        _b?.btnLogout?.isEnabled = false
        _b?.btnLogout?.text = "Sedang keluar..."
        _b?.progressLogout?.isVisible = true

        // Logout lokal tetap dilakukan meskipun server tidak dapat dihubungi.
        mainActivity.lifecycleScope.launch {
            safeApiCall { RetrofitClient.api.logout() }
            if (!mainActivity.isFinishing) {
                mainActivity.forceLogout("Kamu telah keluar dari akun.")
            }
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
