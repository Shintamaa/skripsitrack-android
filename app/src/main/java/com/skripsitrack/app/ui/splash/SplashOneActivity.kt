package com.skripsitrack.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.skripsitrack.app.R
import com.skripsitrack.app.databinding.ActivitySplashOneBinding

class SplashOneActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashOneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashOneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.st_deep)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false

        binding.ivLogo.scaleX = 0.3f
        binding.ivLogo.scaleY = 0.3f
        binding.ivLogo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(1000)
            .setInterpolator(OvershootInterpolator())
            .start()

        binding.tvAppName.animate()
            .alpha(1f).setStartDelay(600).setDuration(800).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, SplashTwoActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2200)
    }
}