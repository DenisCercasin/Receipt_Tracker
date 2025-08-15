package com.example.receipt_scanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receipt_scanner.adapter.ExpenseAdapter
import com.example.receipt_scanner.databinding.ActivityHistoryBinding
import com.example.receipt_scanner.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.YearMonth
import java.util.Calendar

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    private val db = Firebase.firestore
    private val expenses = mutableListOf<Expense>()
    private lateinit var adapter: ExpenseAdapter

    // both are used for filtering
    private var selectedMonth: YearMonth? = null
    private var selectedCategory: String = "All"
    private lateinit var drawerToggle: ActionBarDrawerToggle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar as ActionBar + text on the top
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.title = "History"

        // dynamically set email in drawer header
        val headerView = binding.navigationView.getHeaderView(0)
        val emailTextView = headerView.findViewById<TextView>(R.id.headerUserEmail)
        val currentUser = com.example.receipt_scanner.MainActivity.auth.currentUser
        emailTextView.text = "Hello, ${currentUser?.email ?: "Guest"}"

        // Setup Drawer Toggle
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.topAppBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Where can i go from the navigation bar basically
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> startActivity(Intent(this, DashboardActivity::class.java))
                R.id.nav_statistics -> startActivity(Intent(this, StatisticsActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_logout -> {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { _, _ ->
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
            binding.drawerLayout.closeDrawers()
            true
        }


        setupSpinner()
        setupDatePicker()

        adapter = ExpenseAdapter(
            expenses,
            // if you click on expense
            onClick = { expense ->
                val intent = Intent(this, EditExpenseActivity::class.java)
                intent.putExtra("expenseId", expense.id)
                startActivity(intent)
            },
            // if long click -> delete the expense?
            onLongClick = { expense ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Expense")
                    .setMessage("Do you really want to delete this expense?")
                    .setPositiveButton("Yes") { _, _ ->
                        db.collection("expenses")
                            .document(expense.id!!)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                                expenses.remove(expense)
                                adapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error deleting", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupSpinner() {
        // getting list of all categories from strings.xml
        val categories = resources.getStringArray(R.array.expense_categories)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // connects the spinner (dropdown) to those options
        binding.categoryFilter.adapter = spinnerAdapter

        // if a user selects a category → update the variable → call loadExpenses() again
        binding.categoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = parent.getItemAtPosition(position).toString()
                loadExpenses()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupDatePicker() {
        // When the "Choose Month" button is clicked → open a custom dialog with showMonthYearPicker
        // user selects a month
        // this value is saved as a YearMonth and passed to loadExpenses() to apply the filter
        binding.dateFilterBtn.setOnClickListener {
            showMonthYearPicker { selected ->
                selectedMonth = selected
                loadExpenses()
            }
        }
    }

    private fun showMonthYearPicker(onDateSelected: (YearMonth?) -> Unit) {

        // creating this cool widget with choosing a month and a year
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)


        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.yearPicker)
        val allButton = dialogView.findViewById<Button>(R.id.allButton)


        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = currentMonth + 1

        yearPicker.minValue = 2000
        yearPicker.maxValue = 2100
        yearPicker.value = currentYear

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Month and Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selected = YearMonth.of(yearPicker.value, monthPicker.value)
                Log.d("MonthYearPicker", "Selected: $selected")
                onDateSelected(selected)
            }
            .setNegativeButton("Cancel", null)
            .create()

        // if all is choosen - then it means no filter
        allButton.setOnClickListener {
            Log.d("MonthYearPicker", "Selected: All")
            dialog.dismiss()
            onDateSelected(null)  // Null means "no filter"
        }
            dialog.show()
    }

    // pulls data from the expenses collection in firebase
    private fun loadExpenses() {
        Log.d("Filter", "Loading for category: $selectedCategory and month: $selectedMonth")

        // fetches only if the user is logged-in
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        var query = db.collection("expenses")
            .whereEqualTo("userId", userId)

        // filtering by category
        if (selectedCategory != "All") {
            query = query.whereEqualTo("category", selectedCategory)
        }

        query.orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                expenses.clear()
                for (document in result) {
                    // turn the document into an Expense model.
                    val expense = document.toObject(Expense::class.java)
                    expense.id = document.id

                    // if there is a month filter
                    if (selectedMonth != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = expense.expenseDate!!.toDate()

                        val expenseMonth = calendar.get(Calendar.MONTH) + 1
                        val expenseYear = calendar.get(Calendar.YEAR)

                        // If the expense is NOT from the selected month OR year -> then skip this expense
                        if (expenseMonth != selectedMonth!!.monthValue || expenseYear != selectedMonth!!.year) {
                            continue
                        }
                    }

                    expenses.add(expense)
                }
                // it adds the matching expenses to the list and updates the UI
                adapter.notifyDataSetChanged()
            }
    }

    override fun onResume() { // when we come back to the screen, after editing/deleting -> reload everything
        super.onResume()
        loadExpenses()
    }
}
