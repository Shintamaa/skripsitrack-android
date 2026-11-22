package com.skripsitrack.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }
        b.tvLogin.setOnClickListener { finish() }

        b.actRole.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles))
        b.actRole.setText(roles[0], false)
        b.actRole.setOnItemClickListener { _, _, pos, _ ->
            b.tilInvite.isVisible = pos == 1        // kode undangan hanya untuk dosen
        }

        b.btnRegister.setOnClickListener { attempt() }
    }

    private fun attempt() {
        val name = b.etName.text.toString().trim()
        val nim = b.etNim.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString()
        val confirm = b.etConfirm.text.toString()
        val role = if (b.actRole.text.toString() == "Dosen") "dosen" else "mahasiswa"
        val invite = b.etInvite.text.toString().trim()

        listOf(b.tilName, b.tilNim, b.tilEmail, b.tilInvite, b.tilPassword, b.tilConfirm)
            .forEach { it.error = null }

        when {
            name.length < 3 -> { b.tilName.error = "Nama minimal 3 karakter"; return }
            nim.length < 5 -> { b.tilNim.error = "NIM/NIDN minimal 5 digit"; return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { b.tilEmail.error = "Email tidak valid"; return }
            role == "dosen" && invite.isEmpty() -> { b.tilInvite.error = "Kode undangan wajib diisi"; return }
            pass.length < 6 -> { b.tilPassword.error = "Password minimal 6 karakter"; return }
            pass != confirm -> { b.tilConfirm.error = "Konfirmasi password tidak cocok"; return }
        }

        val body = RegisterRequest(
            name = name, email = email, nim_nidn = nim, role = role,
            password = pass, password_confirmation = confirm,
            invite_code = if (role == "dosen") invite else null
        )

        lifecycleScope.launch {
            setLoading(true)
            val r = safeApiCall { RetrofitClient.api.register(body) }
            setLoading(false)

            when (r) {
                is Resource.Success -> {
                    val data = r.data.data
                    if (data != null) {
                        SkripsiApp.session.saveSession(data.token, data.user)
                        Toast.makeText(this@RegisterActivity,
                            "Registrasi berhasil. Selamat datang, ${data.user.name}", Toast.LENGTH_LONG).show()
                        startActivity(
                            Intent(this@RegisterActivity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                    }
                }
                is Resource.Error ->
                    Toast.makeText(this@RegisterActivity, r.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnRegister.isEnabled = !loading
    }
}