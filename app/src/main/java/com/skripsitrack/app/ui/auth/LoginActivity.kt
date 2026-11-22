package com.skripsitrack.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.skripsitrack.app.R
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.model.LoginRequest
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.ActivityLoginBinding
import com.skripsitrack.app.ui.main.MainActivity
import com.skripsitrack.app.util.Resource
import com.skripsitrack.app.util.normalizeBaseUrl
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var loginInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.st_deep)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvServer.setOnClickListener { showServerDialog() }
        binding.tvRegister.setOnClickListener {
            if (!loginInProgress) {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }
    }

    private fun attemptLogin() {
        if (loginInProgress) return

        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        when {
            email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Masukkan alamat email yang valid"
                binding.etEmail.requestFocus()
                return
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Kata sandi minimal 6 karakter"
                binding.etPassword.requestFocus()
                return
            }
        }

        loginInProgress = true
        lifecycleScope.launch {
            setLoading(true)
            val result = safeApiCall {
                RetrofitClient.api.login(LoginRequest(email, password))
            }
            if (isFinishing || isDestroyed) return@launch
            setLoading(false)
            loginInProgress = false

            when (result) {
                is Resource.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        SkripsiApp.session.saveSession(data.token, data.user)
                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                        )
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Respons login belum lengkap. Silakan coba lagi.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                is Resource.Error -> {
                    Toast.makeText(
                        this@LoginActivity,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> Unit
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnLogin.isEnabled = !loading
        binding.tvRegister.isEnabled = !loading
        binding.tvServer.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }

    private fun showServerDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_input, null)
        val til = view.findViewById<TextInputLayout>(R.id.tilInput)
        val editText = view.findViewById<TextInputEditText>(R.id.etInput)
        til.hint = "Alamat server, contoh: 192.168.1.110:8000"
        editText.setText(SkripsiApp.session.getBaseUrl())

        val dialog = AlertDialog.Builder(this)
            .setTitle("Atur alamat server")
            .setView(view)
            .setPositiveButton("Simpan", null)
            .setNeutralButton("Reset", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = normalizeBaseUrl(editText.text?.toString().orEmpty())
                if (url == null) {
                    til.error = "Alamat server tidak valid"
                    return@setOnClickListener
                }
                SkripsiApp.session.setBaseUrl(url)
                Toast.makeText(this, "Alamat server berhasil disimpan.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val defaultUrl = RetrofitClient.DEFAULT_BASE_URL
                SkripsiApp.session.setBaseUrl(defaultUrl)
                editText.setText(defaultUrl)
                til.error = null
            }
        }
        dialog.show()
    }
}
