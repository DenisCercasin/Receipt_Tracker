package com.example.receipt_scanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.example.receipt_scanner.databinding.ActivityDashboardBinding

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🟢 Setup toolbar as ActionBar
        setSupportActionBar(binding.topAppBar)

        // 🟢 Setup Drawer Toggle (hamburger)
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.topAppBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // 🟢 Navigation Drawer item clicks
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { /* Already here */ }
                R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.nav_statistics -> startActivity(Intent(this, StatisticsActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_logout -> {
                    // Optional logout logic here
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 🟢 Bottom Navigation clicks
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_statistics -> startActivity(Intent(this, StatisticsActivity::class.java))
                R.id.nav_add_manually -> startActivity(Intent(this, AddExpenseActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }

        // 🟢 Top button logic
        binding.addManuallyBtn.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

//        binding.viewHistoryBtn.setOnClickListener {
//            startActivity(Intent(this, HistoryActivity::class.java))
//        }
//
//        binding.settingsBtn.setOnClickListener {
//            startActivity(Intent(this, SettingsActivity::class.java))
//        }
//
//        binding.statisticsBtn.setOnClickListener {
//            startActivity(Intent(this, StatisticsActivity::class.java))
//        }

        binding.scanReceiptBtn.setOnClickListener {
            startActivity(Intent(this, ScanReceiptActivity::class.java))
        }
    }

    // Optional: Handle back press to close drawer
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_none

    }
}
