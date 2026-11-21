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
import com.skripsitrack.app.util.redirectToLogin
import com.skripsitrack.app.ui.thesis.ThesisListFragment

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
                R.id.nav_thesis -> ThesisListFragment()
                R.id.nav_activity -> ActivityHistoryFragment()
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

    fun forceLogout(message: String? = null) = redirectToLogin(message)
}