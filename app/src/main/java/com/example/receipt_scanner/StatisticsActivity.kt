package com.example.receipt_scanner

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.receipt_scanner.model.Expense
import com.example.receipt_scanner.util.MonthYearPickerDialog
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.TimeZone


class StatisticsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private var selectedMonth: Calendar = Calendar.getInstance()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        val btnSelectMonth = findViewById<Button>(R.id.btnSelectMonth)

        firestore = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        btnSelectMonth.setOnClickListener {
            showMonthPicker()
        }

        loadAndDisplayData()
    }

    private fun showMonthPicker() {
        val year = selectedMonth.get(Calendar.YEAR)
        val month = selectedMonth.get(Calendar.MONTH)

        val dialog = MonthYearPickerDialog(this, { y, m ->
            selectedMonth.set(Calendar.YEAR, y)
            selectedMonth.set(Calendar.MONTH, m)
            loadAndDisplayData()
        }, year, month)

        dialog.show()
    }

    private fun loadAndDisplayData() {
        val utc = TimeZone.getTimeZone("UTC")

        val start = Calendar.getInstance(utc).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val end = Calendar.getInstance(utc).apply {
            time = start.time
            add(Calendar.MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startMillis = start.timeInMillis
        val endMillis = end.timeInMillis

        Log.d("STATISTICS", "StartMillis: $startMillis (${start.time})")
        Log.d("STATISTICS", "EndMillis: $endMillis (${end.time})")

        firestore.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                val allExpenses = docs.mapNotNull { it.toObject(Expense::class.java) }

                // ✅ Фильтрация по миллисекундам локально
                val filtered = allExpenses.filter { expense ->
                    val millis = expense.expenseDate?.toDate()?.time ?: 0L
                    millis in startMillis until endMillis
                }

                Log.d("STATISTICS", "Filtered expenses: ${filtered.size}")
                filtered.forEach {
                    Log.d("STATISTICS", "Amount=${it.amount}, Date=${it.expenseDate?.toDate()}, Category=${it.category}")
                }

                updatePieChart(filtered)
                updateBarChartGrouped(filtered)
            }
            .addOnFailureListener {
                Log.e("STATISTICS", "Firestore fetch failed: ${it.message}", it)
            }
    }

    private fun updatePieChart(expenses: List<Expense>) {
        val totalsByCategory = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val entries = totalsByCategory.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "")
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS.toList())
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.animateY(800)
        pieChart.invalidate()
    }

    private fun updateBarChartGrouped(expenses: List<Expense>) {
        val grouped = mutableMapOf<Int, MutableMap<Int, Double>>() // dayOfWeek -> weekOfMonth -> amount

        for (expense in expenses) {
            val cal = Calendar.getInstance()
            cal.time = expense.expenseDate?.toDate() ?: throw IllegalStateException("expenseDate is null")

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday
            val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)

            val weekMap = grouped.getOrPut(dayOfWeek) { mutableMapOf() }
            weekMap[weekOfMonth] = weekMap.getOrDefault(weekOfMonth, 0.0) + expense.amount
        }

        val week1 = mutableListOf<BarEntry>()
        val week2 = mutableListOf<BarEntry>()
        val week3 = mutableListOf<BarEntry>()
        val week4 = mutableListOf<BarEntry>()

        for (day in 1..7) {
            week1.add(BarEntry(day.toFloat(), grouped[day]?.get(1)?.toFloat() ?: 0f))
            week2.add(BarEntry(day.toFloat(), grouped[day]?.get(2)?.toFloat() ?: 0f))
            week3.add(BarEntry(day.toFloat(), grouped[day]?.get(3)?.toFloat() ?: 0f))
            week4.add(BarEntry(day.toFloat(), grouped[day]?.get(4)?.toFloat() ?: 0f))
        }

        val set1 = BarDataSet(week1, "Week 1")
        val set2 = BarDataSet(week2, "Week 2")
        val set3 = BarDataSet(week3, "Week 3")
        val set4 = BarDataSet(week4, "Week 4")

        set1.color = "#2196F3".toColorInt() // Blue
        set2.color = "#F44336".toColorInt() // Red
        set3.color = "#4CAF50".toColorInt() // Green
        set4.color = "#FFEB3B".toColorInt() // Yellow

        val data = BarData(set1, set2, set3, set4)
        val barWidth = 0.2f
        val groupSpace = 0.2f
        val barSpace = 0.05f

        data.barWidth = barWidth

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setFitBars(true)
        barChart.legend.isWordWrapEnabled = true

        // X-Axis settings
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 8f

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        // Group the bars
        barChart.groupBars(0f, groupSpace, barSpace)

        barChart.invalidate()
    }

}
