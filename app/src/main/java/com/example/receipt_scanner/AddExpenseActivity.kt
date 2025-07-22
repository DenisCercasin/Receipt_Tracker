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

        // allowing to optional prefill -> if coming from ocr for example
        // default value not 0.0, because it could be valid amount = -1 is never valid
        val prefillAmount = intent.getDoubleExtra("prefill_amount", -1.0)
        val prefillDate = intent.getStringExtra("prefill_date")

        // filling all the values if they were sent
        if (prefillAmount >= 0) {
            binding.amountET.setText(prefillAmount.toString())
        }
        // If prefillDate is not null or empty, then use it, else use todays date
        if (!prefillDate.isNullOrEmpty()) {
            binding.dateET.setText(prefillDate)
        } else {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            binding.dateET.setText(today)
        }

        supportActionBar?.title = "Add Expense"

        // initialize the firestore db
        val db = Firebase.firestore

        // load category options from res/values/strings.xml
        // then assign them to spinner dropdown menu
        val categoryAdapter = ArrayAdapter.createFromResource(
                this,
        R.array.expense_categories,
        android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        // allows prefilling
        val prefillCategory = intent.getStringExtra("prefill_category")
        if (prefillCategory != null) {
            val index = categoryAdapter.getPosition(prefillCategory)
            if (index >= 0) {
                binding.categorySpinner.setSelection(index)
            }
        }


        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.dateET.setText(today)

        // show material date picker dialog
        binding.dateET.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .build()

            picker.show(supportFragmentManager, picker.toString())

            // when the user select a date -> the date is saved in a human readable format
            picker.addOnPositiveButtonClickListener { selection ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(selection))
                binding.dateET.setText(date)
            }
        }

        // saving expense to firestore
        // firstly reading all input fields
        // secondly, if category is "all", we ask to choose one
        // validate that all fields are filled
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
                    // parsing the date for the firestore db
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    // !! - not null assertion -> i am sure it wont return null - if so, crash the app
                    val parsedDate: Date = formatter.parse(dateString)!!
                    val expenseDate = Timestamp(parsedDate)

                    // we didnt use expense model, but created our own key-value map
                    // its simple and directly serializable by firebase
                    // also possible like that:
//                    val expense = Expense(
//                        amount = amount.toDouble(),
//                        category = category,
//                        expenseDate = expenseDate,
//                        userId = userId,
//                        timestamp = Timestamp.now()
//                    )
//
//                    db.collection("expenses")
//                        .add(expense) // Firebase will serialize it automatically

                    val expense = hashMapOf(
                        "amount" to amount.toDouble(),
                        "category" to category,
                        "expenseDate" to expenseDate,
                        "userId" to userId,
                        "timestamp" to Timestamp.now()
                    )
                    // the data is inserted + handling success/failure
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