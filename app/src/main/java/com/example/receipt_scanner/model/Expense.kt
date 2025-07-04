package com.example.receipt_scanner.model
import com.google.firebase.Timestamp


data class Expense(
    var id: String? = null,
    var amount: Double = 0.0,
    var category: String = "",
    var expenseDate: Timestamp? = null,
    var userId: String = "",
    var timestamp: Timestamp = Timestamp.now()
)