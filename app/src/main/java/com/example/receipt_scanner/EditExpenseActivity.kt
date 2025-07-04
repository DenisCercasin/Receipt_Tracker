package com.example.receipt_scanner

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.receipt_scanner.databinding.ActivityEditExpenseBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditExpenseBinding
    private val db = Firebase.firestore
    private lateinit var expenseId: String

    private val categoryOptions = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Other")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Edit Expense"

        expenseId = intent.getStringExtra("expenseId") ?: return

        // Spinner categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.editCategory.adapter = adapter

        // Show DatePicker on click
        binding.editDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                binding.editDate.setText(dateFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Load data
        db.collection("expenses").document(expenseId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    binding.editAmount.setText(doc.getDouble("amount")?.toString() ?: "")

                    val timestamp = doc.getTimestamp("expenseDate")
                    if (timestamp != null) {
                        val date = timestamp.toDate()
                        calendar.time = date
                        binding.editDate.setText(dateFormat.format(date))
                    }

                    val category = doc.getString("category") ?: ""
                    val index = categoryOptions.indexOf(category)
                    if (index != -1) {
                        binding.editCategory.setSelection(index)
                    }
                }
            }

        // Save updates
        binding.saveEditBtn.setOnClickListener {
            val updatedAmount = binding.editAmount.text.toString().toDoubleOrNull()
            val updatedDate = binding.editDate.text.toString()
            val parsedDate = dateFormat.parse(updatedDate)!!
            val updatedExpenseDate = com.google.firebase.Timestamp(parsedDate)
            val updatedCategory = binding.editCategory.selectedItem.toString()


            if (updatedAmount == null || updatedDate.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("UPDATE_DEBUG", "Updating: amount=$updatedAmount, category=$updatedCategory, expenseDate=$updatedExpenseDate")
            db.collection("expenses").document(expenseId)
                .update(
                    mapOf(
                        "amount" to updatedAmount,
                        "expenseDate" to updatedExpenseDate,
                        "category" to updatedCategory
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
                    finish() // go back to History
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
