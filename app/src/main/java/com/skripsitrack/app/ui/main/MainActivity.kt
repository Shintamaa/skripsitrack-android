package com.skripsitrack.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.skripsitrack.app.R
import com.skripsitrack.app.SkripsiApp
import com.skripsitrack.app.databinding.ActivityMainBinding
import com.skripsitrack.app.ui.thesis.ThesisListFragment
import com.skripsitrack.app.util.redirectToLogin

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val STATE_SELECTED_TAB = "selected_tab"
        private const val TAG_DASHBOARD = "tab_dashboard"
        private const val TAG_THESIS = "tab_thesis"
        private const val TAG_ACTIVITY = "tab_activity"
        private const val TAG_PROFILE = "tab_profile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isDosen = SkripsiApp.session.getRole() == "dosen"
        if (isDosen) {
            binding.bottomNav.menu.findItem(R.id.nav_thesis).title = "Bimbingan"
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }

        // Memilih ulang tab yang sama tidak membuat Fragment baru.
        binding.bottomNav.setOnItemReselectedListener { }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNav.selectedItemId != R.id.nav_dashboard) {
                    binding.bottomNav.selectedItemId = R.id.nav_dashboard
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        val selectedTab = savedInstanceState?.getInt(
            STATE_SELECTED_TAB,
            R.id.nav_dashboard
        ) ?: R.id.nav_dashboard

        binding.bottomNav.selectedItemId = selectedTab
        // BottomNavigationView bisa saja sudah memilih item pertama sebelum listener
        // dipasang; pastikan Fragment pertama tetap dibuat.
        if (supportFragmentManager.fragments.none { it.id == R.id.container }) {
            showTab(selectedTab)
        }
    }

    private fun showTab(itemId: Int) {
        val fragmentManager = supportFragmentManager
        val targetTag = when (itemId) {
            R.id.nav_dashboard -> TAG_DASHBOARD
            R.id.nav_thesis -> TAG_THESIS
            R.id.nav_activity -> TAG_ACTIVITY
            else -> TAG_PROFILE
        }

        val transaction = fragmentManager.beginTransaction()
            .setReorderingAllowed(true)

        fragmentManager.fragments
            .filter { it.id == R.id.container && it.isAdded }
            .forEach { transaction.hide(it) }

        var target = fragmentManager.findFragmentByTag(targetTag)
        if (target == null) {
            target = createFragment(itemId)
            transaction.add(R.id.container, target, targetTag)
        } else {
            transaction.show(target)
        }

        transaction.commitNow()
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_dashboard -> DashboardFragment()
        R.id.nav_thesis -> ThesisListFragment()
        R.id.nav_activity -> ActivityHistoryFragment()
        else -> ProfileFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, binding.bottomNav.selectedItemId)
        super.onSaveInstanceState(outState)
    }

    fun forceLogout(message: String? = null) {
        redirectToLogin(message)
    }
}
