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

// Adapter that binds list of Expense objects to RecyclerView items
class ExpenseAdapter(
    private val expenses: MutableList<Expense>, // list of the expenses
    private val onClick: (Expense) -> Unit, // if short click
    private val onLongClick: (Expense) -> Unit // if long click
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    // holds reference to the vies inside each item
    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        val textAmount: TextView = itemView.findViewById(R.id.textAmount)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
    }
    // called when RecyclerView needs a new ViewHolder -> with inflating
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            // R.layout.item_expense - This refers to res/layout/item_expense.xml
            //parent - This is the RecyclerView
            // false = inflate the layout but don’t attach it yet
            // false to prevent double attaching, because RecyclerView will do it later
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        // Set the text values for category and amount
        holder.textCategory.text = expense.category
        holder.textAmount.text = "%.2f €".format(expense.amount)

        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        holder.textDate.text = formatter.format(expense.expenseDate?.toDate() ?: throw IllegalStateException("expenseDate is null"))

        // long click listener for deletion
        holder.itemView.setOnLongClickListener {
            onLongClick(expense)
            true
        }
        // short click listener for editing
        holder.itemView.setOnClickListener {
            onClick(expense)
        }
    }

    // how many items in the list totally
    override fun getItemCount(): Int = expenses.size
}
