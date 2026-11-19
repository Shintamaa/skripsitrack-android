package com.skripsitrack.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.skripsitrack.app.R
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.databinding.ActivityMainBinding
import com.skripsitrack.app.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isDosen = SkripsiApp.session.getRole() == "dosen"
        if (isDosen) {
            binding.bottomNav.menu.findItem(R.id.nav_thesis).title = "Bimbingan"
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_thesis -> PlaceholderFragment.newInstance(
                    if (isDosen) "Daftar Bimbingan" else "Daftar Skripsi", "Dibuat di Bagian 3")
                R.id.nav_activity -> PlaceholderFragment.newInstance(
                    "Riwayat Aktivitas", "Dibuat di Bagian 6")
                else -> ProfileFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }

    /** Hapus sesi lalu kembali ke Login. Dipakai saat logout dan saat token kedaluwarsa (401). */
    fun forceLogout(message: String? = null) {
        SkripsiApp.session.clear()
        message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}