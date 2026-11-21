package com.skripsitrack.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.model.LoginRequest
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.ActivityLoginBinding
import com.skripsitrack.app.ui.main.MainActivity
import com.skripsitrack.app.util.Resource
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.skripsitrack.app.R
import com.skripsitrack.app.util.normalizeBaseUrl

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvServer.setOnClickListener { showServerDialog() }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Validasi input (syarat nomor 5)
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Masukkan email yang valid"; return
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password minimal 6 karakter"; return
        }

        lifecycleScope.launch {
            setLoading(true)
            val result = safeApiCall { RetrofitClient.api.login(LoginRequest(email, password)) }
            setLoading(false)

            when (result) {
                is Resource.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        SkripsiApp.session.saveSession(data.token, data.user)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }
                is Resource.Error -> Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun showServerDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_input, null)
        val til = view.findViewById<TextInputLayout>(R.id.tilInput)
        val et = view.findViewById<TextInputEditText>(R.id.etInput)
        til.hint = "Alamat server, contoh: 192.168.1.110:8000"
        et.setText(SkripsiApp.session.getBaseUrl())

        val dialog = AlertDialog.Builder(this)
            .setTitle("Atur server")
            .setView(view)
            .setPositiveButton("Simpan", null)
            .setNeutralButton("Reset", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = normalizeBaseUrl(et.text.toString())
                if (url == null) { til.error = "Alamat tidak valid"; return@setOnClickListener }
                SkripsiApp.session.setBaseUrl(url)
                Toast.makeText(this, "Server: $url", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                SkripsiApp.session.setBaseUrl(RetrofitClient.DEFAULT_BASE_URL)
                et.setText(RetrofitClient.DEFAULT_BASE_URL)
            }
        }
        dialog.show()
    }
}