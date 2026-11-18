package com.skripsitrack.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.ActivityMainBinding
import com.skripsitrack.app.ui.auth.LoginActivity
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "Halo, ${SkripsiApp.session.getName()}"
        binding.tvRole.text = "Role: ${SkripsiApp.session.getRole()}"

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Yakin ingin keluar?")
                .setPositiveButton("Ya") { _, _ -> doLogout() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun doLogout() {
        lifecycleScope.launch {
            safeApiCall { RetrofitClient.api.logout() }   // gagal pun tetap lanjut keluar
            SkripsiApp.session.clear()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }
}