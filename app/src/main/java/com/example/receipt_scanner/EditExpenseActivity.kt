package com.example.receipt_scanner

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.receipt_scanner.databinding.ActivityEditExpenseBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditExpenseBinding
    private val db = Firebase.firestore
    private lateinit var expenseId: String

    // predefined values
    private val categoryOptions = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Other")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Spinner setup - native calendar dialog
        // adaptin also the date format
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.editCategory.adapter = adapter

        // Setup DatePicker
        binding.editDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    binding.editDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Check mode -> if expenseId was passed - we fetch existing expense from Firestor and fill all the values
        expenseId = intent.getStringExtra("expenseId") ?: ""

        if (expenseId.isNotEmpty()) {
            supportActionBar?.title = "Edit Expense"

            // Load data from Firestore
            db.collection("expenses").document(expenseId)
                .get()
                .addOnSuccessListener { doc ->
                    // if exists in firestore - we load the data in these values
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
        } else {
            supportActionBar?.title = "Add & Edit Expense"

            // Prefill from OCR
            val prefillAmount = intent.getDoubleExtra("prefill_amount", -1.0)
            if (prefillAmount >= 0) binding.editAmount.setText(prefillAmount.toString())

            //  checking if ocr found a date - if not we try parsedatesmart - looking maybe at different formats
            val prefillDate = intent.getStringExtra("prefill_date")
            if (!prefillDate.isNullOrBlank()) {
                val parsed = parseDateSmart(prefillDate)
                if (parsed != null) {
                    calendar.time = parsed
                    binding.editDate.setText(dateFormat.format(parsed))
                }
            }

            val prefillCategory = intent.getStringExtra("prefill_category")
            if (!prefillCategory.isNullOrBlank()) {
                val index = categoryOptions.indexOf(prefillCategory)
                if (index != -1) binding.editCategory.setSelection(index)
            }
        }

        // Save button logic
        binding.saveEditBtn.setOnClickListener {
            val updatedAmount = binding.editAmount.text.toString().toDoubleOrNull()
            val updatedDate = binding.editDate.text.toString()
            val parsedDate = dateFormat.parse(updatedDate)
            val updatedCategory = binding.editCategory.selectedItem.toString()

            // validates that amount and date are OK
            if (updatedAmount == null || parsedDate == null) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expenseData = mapOf(
                "amount" to updatedAmount,
                "expenseDate" to Timestamp(parsedDate),
                "category" to updatedCategory,
                "userId" to FirebaseAuth.getInstance().currentUser?.uid,
                "timestamp" to Timestamp.now()
            )
            // if expenseId is known - we do update in the db
            if (expenseId.isNotEmpty()) {

                db.collection("expenses").document(expenseId)
                    .update(expenseData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // if no expenseId is known - then we create one - when from OCR we click on edit
                // and after it we save
                db.collection("expenses")
                    .add(expenseData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // function mostly for OCR - so if we didnt get from there a normal datestring then
    // we check maybe its just in another format
    private fun parseDateSmart(dateString: String): Date? {
        val possibleFormats = listOf(
            "dd.MM.yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd.MM.yy",
            "dd/MM/yy"
        )
        for (format in possibleFormats) {
            try {
                val formatter = SimpleDateFormat(format, Locale.getDefault())
                return formatter.parse(dateString)
            } catch (_: Exception) {}
        }
        return null
    }
}

