package com.example.receipt_scanner

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.NumberPicker
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.drawerlayout.widget.DrawerLayout
import com.example.receipt_scanner.model.Expense
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.TimeZone


class StatisticsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private var selectedDate: Calendar? = Calendar.getInstance()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var drawerToggle: ActionBarDrawerToggle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        val btnSelectMonth = findViewById<Button>(R.id.btnSelectMonth)

        firestore = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Set up toolbar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.title = "Statistics"


        // Set up drawer toggle
        drawerToggle = ActionBarDrawerToggle(
            this,
            findViewById(R.id.drawerLayout),
            topAppBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        findViewById<DrawerLayout>(R.id.drawerLayout).addDrawerListener(drawerToggle)
        drawerToggle.syncState()

// Set up navigation - where can i go from my navigation bar
        findViewById<NavigationView>(R.id.navigationView).setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> startActivity(Intent(this, DashboardActivity::class.java))
                R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { _, _ ->
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
            findViewById<DrawerLayout>(R.id.drawerLayout).closeDrawers()
            true
        }
        // used for filtering
        btnSelectMonth.setOnClickListener {
            // 2 pickers only -> month + year and ALL button as kinda easy reset
            showMonthPicker()
        }

        loadAndDisplayData()
    }
    // creating this month/year picker + all button
    private fun showMonthPicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.yearPicker)
        val allButton = dialogView.findViewById<Button>(R.id.allButton)

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = currentMonth + 1

        yearPicker.minValue = 2000
        yearPicker.maxValue = 2100
        yearPicker.value = currentYear

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Month and Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                if (selectedDate == null) selectedDate = Calendar.getInstance()
                selectedDate!!.set(Calendar.YEAR, yearPicker.value)
                selectedDate!!.set(Calendar.MONTH, monthPicker.value - 1)
                selectedDate!!.set(Calendar.DAY_OF_MONTH, 1)
                loadAndDisplayData()
            }
            .setNegativeButton("Cancel", null)
            .create()

        allButton.setOnClickListener {
            selectedDate = null  // clear filter
            dialog.dismiss()
            loadAndDisplayData()
        }

        dialog.show()
    }


    private fun loadAndDisplayData() {
        // not to mess up
        val utc = TimeZone.getTimeZone("UTC")

        val start = Calendar.getInstance(utc)
        val end = Calendar.getInstance(utc)

        // filtering -> beginning and end of the month needed
        if (selectedDate != null) {
            start.set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
            start.set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            end.time = start.time
            end.add(Calendar.MONTH, 1)

            Log.d("STATISTICS", "StartMillis: ${start.timeInMillis} (${start.time})")
            Log.d("STATISTICS", "EndMillis: ${end.timeInMillis} (${end.time})")
        } else {
            Log.d("STATISTICS", "No date filtering – showing all expenses")
        }

        // loading all the data from the firestore, filtering then by month
        firestore.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                val allExpenses = docs.mapNotNull { it.toObject(Expense::class.java) }

                val filtered = if (selectedDate != null) {
                    val startMillis = start.timeInMillis
                    val endMillis = end.timeInMillis

                    allExpenses.filter { expense ->
                        val millis = expense.expenseDate?.toDate()?.time ?: 0L
                        millis in startMillis until endMillis
                    }
                } else {
                    allExpenses
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
        // grouping all expenses by category and then creating piechart with  categories and sums
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
        // showing the expenses by weeks and days of the week
        // map all day>week>sum
        val grouped = mutableMapOf<Int, MutableMap<Int, Double>>() // dayOfWeek -> weekOfMonth -> amount

        for (expense in expenses) {
            val cal = Calendar.getInstance()
            cal.time = expense.expenseDate?.toDate() ?: throw IllegalStateException("expenseDate is null")

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday
            val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)

            val weekMap = grouped.getOrPut(dayOfWeek) { mutableMapOf() }
            weekMap[weekOfMonth] = weekMap.getOrDefault(weekOfMonth, 0.0) + expense.amount
        }
        // creating 4 weeks, later will be 4 sets for the bardataset
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

        // colors, height etc
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
