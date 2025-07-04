package com.example.receipt_scanner.util

import android.app.DatePickerDialog
import android.content.Context

class MonthYearPickerDialog(
    context: Context,
    private val listener: (year: Int, month: Int) -> Unit,
    year: Int,
    month: Int
) : DatePickerDialog(context, { _, y, m, _ -> listener(y, m) }, year, month, 1) {

    init {
        try {
            // Скрыть выбор дня
            val dayField = this.datePicker.findViewById(
                context.resources.getIdentifier("day", "id", "android")
            ) as? android.view.View
            dayField?.visibility = android.view.View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
