package com.example.familybudget

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.TransactionEntity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var pieChart: PieChart
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvPeriod: TextView
    private lateinit var btnCalendar: Button
    private lateinit var btnBack: Button

    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        pieChart = findViewById(R.id.pieChart)
        tvTotalExpenses = findViewById(R.id.tv_total_expenses)
        tvPeriod = findViewById(R.id.tv_period)
        btnCalendar = findViewById(R.id.btn_calendar)
        btnBack = findViewById(R.id.btn_back)

        db = AppDatabase.getInstance(this)

        setDefaultPeriod()
        updatePeriodText()

        loadStatistics()

        btnCalendar.setOnClickListener {
            showDateRangePickerDialog()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setDefaultPeriod() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        startDate = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        endDate = calendar.time
    }

    private fun updatePeriodText() {
        tvPeriod.text = when {
            startDate != null && endDate != null && isSameDay(startDate!!, endDate!!) ->
                "Расходы за ${dateFormat.format(startDate)}"
            startDate != null && endDate != null ->
                "Расходы с ${dateFormat.format(startDate)} по ${dateFormat.format(endDate)}"
            else -> "Расходы за всё время"
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun showDateRangePickerDialog() {
        val options = arrayOf("Один день", "Диапазон (с — по)", "Сбросить на текущий месяц")
        AlertDialog.Builder(this)
            .setTitle("Выбор периода")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSingleDatePicker()
                    1 -> showRangeDatePicker()
                    2 -> resetToCurrentMonth()
                }
            }
            .show()
    }

    private fun showSingleDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                }.time
                startDate = selectedDate
                endDate = selectedDate
                updatePeriodText()
                loadStatistics()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showRangeDatePicker() {
        val calendar = Calendar.getInstance()
        val firstDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val start = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                }.time
                startDate = start
                val secondDialog = DatePickerDialog(
                    this,
                    { _, y, m, d ->
                        val end = Calendar.getInstance().apply {
                            set(y, m, d, 23, 59, 59)
                        }.time
                        endDate = end
                        updatePeriodText()
                        loadStatistics()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                secondDialog.setTitle("Выберите конечную дату")
                secondDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        firstDialog.setTitle("Выберите начальную дату")
        firstDialog.show()
    }

    private fun resetToCurrentMonth() {
        setDefaultPeriod()
        updatePeriodText()
        loadStatistics()
    }

    private fun loadStatistics() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allTransactions = db.transactionDao().getAllTransactions()

            val filteredTransactions = allTransactions.filter { transaction ->
                transaction.type == "Расход" &&
                        (startDate == null || endDate == null || (transaction.date in startDate!!..endDate!!))
            }

            val expensesByCategory = mutableMapOf<String, Double>()
            var totalExpenses = 0.0

            for (t in filteredTransactions) {
                val category = t.category
                val amount = t.amount
                expensesByCategory[category] = (expensesByCategory[category] ?: 0.0) + amount
                totalExpenses += amount
            }

            val finalTotalExpenses = totalExpenses
            val finalExpensesByCategory = expensesByCategory

            withContext(Dispatchers.Main) {
                if (finalTotalExpenses == 0.0) {
                    tvTotalExpenses.text = "Общие расходы: 0.00 ₽"
                    tvTotalExpenses.setTextColor(Color.GRAY)
                    pieChart.clear()
                    pieChart.setNoDataText("Нет расходов за выбранный период")
                    pieChart.invalidate()
                } else {
                    tvTotalExpenses.text = String.format("Общие расходы: %.2f ₽", finalTotalExpenses)
                    tvTotalExpenses.setTextColor(Color.RED)
                    setupPieChart(finalExpensesByCategory, finalTotalExpenses)
                }
            }
        }
    }

    private fun setupPieChart(data: Map<String, Double>, totalExpenses: Double) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        val colorList = listOf(
            Color.rgb(255, 99, 132),
            Color.rgb(54, 162, 235),
            Color.rgb(255, 206, 86),
            Color.rgb(75, 192, 192),
            Color.rgb(153, 102, 255),
            Color.rgb(255, 159, 64),
            Color.rgb(199, 199, 199),
            Color.rgb(83, 102, 255),
            Color.rgb(255, 99, 255)
        )

        var i = 0
        for ((category, amount) in data) {
            if (amount > 0) {
                entries.add(PieEntry(amount.toFloat(), category))
                colors.add(colorList[i % colorList.size])
                i++
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("Нет данных для отображения")
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Расходы по категориям")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(11f)
        pieData.setValueTextColor(Color.BLACK)

        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Расходs\n${String.format("%.0f", totalExpenses)} ₽"
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}