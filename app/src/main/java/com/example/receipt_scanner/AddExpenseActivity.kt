package com.example.receipt_scanner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.receipt_scanner.databinding.ActivityAddExpenseBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefillAmount = intent.getDoubleExtra("prefill_amount", -1.0)
        val prefillDate = intent.getStringExtra("prefill_date")

// 💰 Заполняем сумму, если она передана
        if (prefillAmount >= 0) {
            binding.amountET.setText(prefillAmount.toString())
        }

// 📅 Заполняем дату, если передана
        if (!prefillDate.isNullOrEmpty()) {
            binding.dateET.setText(prefillDate)
        } else {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            binding.dateET.setText(today)
        }



        supportActionBar?.title = "Add Expense"

        // ✅ Инициализируем Firestore
        val db = Firebase.firestore

        val categoryAdapter = ArrayAdapter.createFromResource(
                this,
        R.array.expense_categories,
        android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        val prefillCategory = intent.getStringExtra("prefill_category")
        if (prefillCategory != null) {
            val index = categoryAdapter.getPosition(prefillCategory)
            if (index >= 0) {
                binding.categorySpinner.setSelection(index)
            }
        }


        // Установим дату по умолчанию
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.dateET.setText(today)

        // 📅 Открываем календарь по нажатию на поле даты
        binding.dateET.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .build()

            picker.show(supportFragmentManager, picker.toString())

            picker.addOnPositiveButtonClickListener { selection ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(selection))
                binding.dateET.setText(date)
            }
        }

        // 💾 Сохраняем расход по нажатию на кнопку
        binding.saveBtn.setOnClickListener {
            val amount = binding.amountET.text.toString()
            val category = binding.categorySpinner.selectedItem.toString()
            val dateString = binding.dateET.text.toString()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

            if (category == "All") {
                Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount.isNotEmpty() && category.isNotEmpty() && dateString.isNotEmpty()) {
                try {
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val parsedDate: Date = formatter.parse(dateString)!!
                    val expenseDate = Timestamp(parsedDate)

                    val expense = hashMapOf(
                        "amount" to amount.toDouble(),
                        "category" to category,
                        "expenseDate" to expenseDate,
                        "userId" to userId,
                        "timestamp" to Timestamp.now()
                    )

                    db.collection("expenses")
                        .add(expense)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Saved to Firebase!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}