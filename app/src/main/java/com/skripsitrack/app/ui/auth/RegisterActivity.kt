package com.skripsitrack.app.ui.auth

import com.skripsitrack.app.util.applyStatusBarInset
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.skripsitrack.app.R
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.data.model.RegisterRequest
import com.skripsitrack.app.data.remote.RetrofitClient
import com.skripsitrack.app.databinding.ActivityRegisterBinding
import com.skripsitrack.app.ui.main.MainActivity
import com.skripsitrack.app.util.Resource
import com.skripsitrack.app.util.safeApiCall
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding
    private val roles = listOf("Mahasiswa", "Dosen")
    private var registerInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.toolbar.applyStatusBarInset()

        window.statusBarColor = ContextCompat.getColor(this, R.color.st_deep)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false

        b.toolbar.setNavigationOnClickListener { finish() }
        b.tvLogin.setOnClickListener {
            if (!registerInProgress) finish()
        }

        b.actRole.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        )
        b.actRole.setText(roles.first(), false)
        b.tilInvite.isVisible = false

        b.actRole.setOnItemClickListener { _, _, position, _ ->
            val isLecturer = roles[position] == "Dosen"
            b.tilInvite.isVisible = isLecturer
            if (!isLecturer) {
                b.etInvite.setText("")
                b.tilInvite.error = null
            }
        }

        b.btnRegister.setOnClickListener { attempt() }
    }

    private fun attempt() {
        if (registerInProgress) return

        val name = b.etName.text?.toString()?.trim().orEmpty()
        val nim = b.etNim.text?.toString()?.trim().orEmpty()
        val email = b.etEmail.text?.toString()?.trim().orEmpty()
        val password = b.etPassword.text?.toString().orEmpty()
        val confirm = b.etConfirm.text?.toString().orEmpty()
        val role = if (b.actRole.text.toString() == "Dosen") "dosen" else "mahasiswa"
        val invite = b.etInvite.text?.toString()?.trim().orEmpty()

        listOf(
            b.tilName, b.tilNim, b.tilEmail,
            b.tilInvite, b.tilPassword, b.tilConfirm
        ).forEach { it.error = null }

        when {
            name.length < 3 -> {
                b.tilName.error = "Nama minimal 3 karakter"
                b.etName.requestFocus()
                return
            }
            nim.length < 5 -> {
                b.tilNim.error = "NIM/NIDN minimal 5 karakter"
                b.etNim.requestFocus()
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                b.tilEmail.error = "Alamat email tidak valid"
                b.etEmail.requestFocus()
                return
            }
            role == "dosen" && invite.isEmpty() -> {
                b.tilInvite.error = "Kode undangan dosen wajib diisi"
                b.etInvite.requestFocus()
                return
            }
            password.length < 6 -> {
                b.tilPassword.error = "Kata sandi minimal 6 karakter"
                b.etPassword.requestFocus()
                return
            }
            password != confirm -> {
                b.tilConfirm.error = "Konfirmasi kata sandi tidak cocok"
                b.etConfirm.requestFocus()
                return
            }
        }

        val request = RegisterRequest(
            name = name,
            email = email,
            nim_nidn = nim,
            role = role,
            password = password,
            password_confirmation = confirm,
            invite_code = if (role == "dosen") invite else null
        )

        registerInProgress = true
        lifecycleScope.launch {
            setLoading(true)
            val result = safeApiCall { RetrofitClient.api.register(request) }
            if (isFinishing || isDestroyed) return@launch
            setLoading(false)
            registerInProgress = false

            when (result) {
                is Resource.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        SkripsiApp.session.saveSession(data.token, data.user)
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registrasi berhasil. Selamat datang, ${data.user.name}",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(
                            Intent(this@RegisterActivity, MainActivity::class.java)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                        )
                    } else {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registrasi berhasil, tetapi data sesi belum diterima. Silakan login.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }

                is Resource.Error -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> Unit
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progress.isVisible = loading
        b.btnRegister.isEnabled = !loading
        b.tvLogin.isEnabled = !loading
        b.toolbar.isEnabled = !loading
        b.etName.isEnabled = !loading
        b.etNim.isEnabled = !loading
        b.etEmail.isEnabled = !loading
        b.actRole.isEnabled = !loading
        b.etInvite.isEnabled = !loading
        b.etPassword.isEnabled = !loading
        b.etConfirm.isEnabled = !loading
    }
}
