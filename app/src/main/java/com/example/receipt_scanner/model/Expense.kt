package com.example.receipt_scanner.model
import com.google.firebase.Timestamp

// using kotlin data class then to have all the methods like toString(), equals(), copy and so on
data class Expense(
    var id: String? = null,
    var amount: Double = 0.0,
    var category: String = "",
    var expenseDate: Timestamp? = null,
    var userId: String = "",
    var timestamp: Timestamp = Timestamp.now()
)