package com.example.receipt_scanner

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.example.receipt_scanner.databinding.ActivityDashboardBinding
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.util.Calendar

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // accessing all the views from activity_dashboard.xml
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // dynamically set email in drawer header
        val headerView = binding.navigationView.getHeaderView(0)
        val emailTextView = headerView.findViewById<TextView>(R.id.headerUserEmail)
        val currentUser = com.example.receipt_scanner.MainActivity.auth.currentUser
        emailTextView.text = "Hello, ${currentUser?.email ?: "Guest"}"

        // Setup toolbar as ActionBar - on the top hamburger icon
        setSupportActionBar(binding.topAppBar)

        // Setup Drawer Toggle (hamburger)
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.topAppBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()


        // Setting the navigation in the drawer - to which screen we can come
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { /* Already here -> nothing to do */
                }

                R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.nav_statistics -> startActivity(Intent(this, StatisticsActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_logout -> {
                    // double checking if the user really wants to log out
                    AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { _, _ ->
                            FirebaseAuth.getInstance().signOut() // signs the user out
                            val intent = Intent(this, MainActivity::class.java)
                            // prevents user from returning with the back button
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Bottom Navigation clicks -> from the xml
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_add_manually -> {
                    startActivity(Intent(this, AddExpenseActivity::class.java))
                }

                R.id.nav_scan_receipt -> {
                    startActivity(Intent(this, ScanReceiptActivity::class.java))
                }

            }
            // Reset to dummy to clear the highlight
            // so when we come back to dashboard again -> it resets this highlight
            binding.bottomNavigation.selectedItemId = R.id.nav_none
            true
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_none
        loadMonthlyTotal() // when the user returns to this screen, recalculate the spendings

    }

    // query magic!
    private fun loadMonthlyTotal() {

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid

        // calculate the start of the month to filter expenses
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = Timestamp(calendar.time)

        Log.d("FIRESTORE", "Fetching expenses for userId=$uid since $monthStart")


        Firebase.firestore.collection("expenses")
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("expenseDate", monthStart)
            .get()
            .addOnSuccessListener { result ->
                Log.d("FIRESTORE", "Fetched ${result.size()} documents")

                // add all amounts for all expenses in this month to the total
                var total = 0.0
                for (doc in result) {
                    total += doc.getDouble("amount") ?: 0.0
                }
                val textView = findViewById<TextView>(R.id.monthlyTotalText)
                // update the screen with new number
                textView.text = "This month you've already spent €%.2f".format(total)
            }
            .addOnFailureListener {
                Log.e("FIRESTORE", "Error loading monthly total", it)
            }
    }

}
