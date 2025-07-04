package com.example.receipt_scanner

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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
    private var selectedMonth: YearMonth? = null
    private var selectedCategory: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "History"

        setupSpinner()
        setupDatePicker()

        adapter = ExpenseAdapter(
            expenses,
            onClick = { expense ->
                val intent = Intent(this, EditExpenseActivity::class.java)
                intent.putExtra("expenseId", expense.id)
                startActivity(intent)
            },
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
        val categories = resources.getStringArray(R.array.expense_categories)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categoryFilter.adapter = spinnerAdapter

        binding.categoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = parent.getItemAtPosition(position).toString()
                loadExpenses()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupDatePicker() {
        binding.dateFilterBtn.setOnClickListener {
            val now = Calendar.getInstance()
            val year = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH)

            val datePicker = DatePickerDialog(this,
                { _, selectedYear, selectedMonthIndex, _ ->
                    selectedMonth = YearMonth.of(selectedYear, selectedMonthIndex + 1)
                    loadExpenses()
                },
                year, month, 1
            )

            val dayField = datePicker.datePicker.findViewById<View>(
                resources.getIdentifier("day", "id", "android")
            )
            dayField?.visibility = View.GONE
            datePicker.show()
        }
    }

    private fun loadExpenses() {
        Log.d("Filter", "Loading for category: $selectedCategory and month: $selectedMonth")

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        var query = db.collection("expenses")
            .whereEqualTo("userId", userId)

        if (selectedCategory != "All") {
            query = query.whereEqualTo("category", selectedCategory)
        }

        query.orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                expenses.clear()
                for (document in result) {
                    val expense = document.toObject(Expense::class.java)
                    expense.id = document.id

                    if (selectedMonth != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = expense.timestamp!!.toDate()

                        val expenseMonth = calendar.get(Calendar.MONTH) + 1
                        val expenseYear = calendar.get(Calendar.YEAR)

                        if (expenseMonth != selectedMonth!!.monthValue || expenseYear != selectedMonth!!.year) {
                            continue
                        }
                    }

                    expenses.add(expense)
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }
}
