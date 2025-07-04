package com.example.receipt_scanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.receipt_scanner.R
import com.example.receipt_scanner.model.Expense
import java.text.SimpleDateFormat
import java.util.Locale

class ExpenseAdapter(
    private val expenses: MutableList<Expense>,
    private val onClick: (Expense) -> Unit,
    private val onLongClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        val textAmount: TextView = itemView.findViewById(R.id.textAmount)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.textCategory.text = expense.category
        holder.textAmount.text = "%.2f €".format(expense.amount)

        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        holder.textDate.text = formatter.format(expense.expenseDate?.toDate() ?: throw IllegalStateException("expenseDate is null"))

        holder.itemView.setOnLongClickListener {
            onLongClick(expense)
            true
        }

        holder.itemView.setOnClickListener {
            onClick(expense)
        }
    }

    override fun getItemCount(): Int = expenses.size
}
