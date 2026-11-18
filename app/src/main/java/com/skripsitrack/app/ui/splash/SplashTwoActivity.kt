package com.skripsitrack.app.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.databinding.ActivitySplashTwoBinding
import com.skripsitrack.app.ui.auth.LoginActivity
import com.skripsitrack.app.ui.main.MainActivity

class SplashTwoActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashTwoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashTwoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listOf(binding.tvTitle, binding.tvTagline).forEachIndexed { i, v ->
            v.translationY = 80f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(i * 300L).setDuration(700).start()
        }

        ObjectAnimator.ofInt(binding.progress, "progress", 0, 100).apply {
            duration = 2000
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val next = if (SkripsiApp.session.isLoggedIn())
                MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, next))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2300)
    }
}