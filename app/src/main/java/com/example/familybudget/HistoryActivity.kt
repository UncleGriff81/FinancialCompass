package com.example.familybudget

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.TransactionEntity
import com.example.familybudget.managers.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var listView: ListView
    private lateinit var spinnerFilterType: Spinner
    private lateinit var spinnerSecond: Spinner
    private lateinit var tvDateRange: TextView
    private lateinit var btnCalendar: Button
    private lateinit var adapter: HistoryAdapter
    private lateinit var dialogManager: DialogManager

    private var allTransactions = listOf<TransactionEntity>()
    private var filteredTransactions = listOf<TransactionEntity>()
    private val expenseCategories = mutableListOf<String>()
    private val incomeCategories = mutableListOf<String>()

    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var currentSortMode = "date_desc"

    private val sortOptions = listOf(
        "📅 По дате (сначала новые)",
        "📅 По дате (сначала старые)",
        "💰 По сумме (сначала большие)",
        "💰 По сумме (сначала малые)",
        "💵 Сначала наличные",
        "💳 Сначала безналичные"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        db = AppDatabase.getInstance(this)
        dialogManager = DialogManager(this)

        listView = findViewById(R.id.list_history)
        spinnerFilterType = findViewById(R.id.spinner_filter_type)
        spinnerSecond = findViewById(R.id.spinner_second)
        tvDateRange = findViewById(R.id.tv_date_range)
        btnCalendar = findViewById(R.id.btn_calendar)

        setupToolbar()
        setupSpinners()
        setupCalendarButton()
        loadData()

        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (position < filteredTransactions.size) {
                val transaction = filteredTransactions[position]
                dialogManager.showEditDeleteDialog(
                    transaction,
                    onEdit = { showEditDialog(transaction) },
                    onDelete = { showDeleteDialog(transaction) }
                )
            }
            true
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCalendarButton() {
        btnCalendar.setOnClickListener {
            showDateRangePickerDialog()
        }
    }

    private fun showDateRangePickerDialog() {
        val options = arrayOf("Один день", "Диапазон (с — по)", "Сбросить фильтр")
        AlertDialog.Builder(this)
            .setTitle("Выбор периода")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSingleDatePicker()
                    1 -> showRangeDatePicker()
                    2 -> resetDateFilter()
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
                updateDateRangeText()
                applyFilters()
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
                        updateDateRangeText()
                        applyFilters()
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

    private fun resetDateFilter() {
        startDate = null
        endDate = null
        updateDateRangeText()
        applyFilters()
    }

    private fun updateDateRangeText() {
        tvDateRange.text = when {
            startDate != null && endDate != null && startDate == endDate ->
                "Период: ${dateFormat.format(startDate)}"
            startDate != null && endDate != null ->
                "Период: ${dateFormat.format(startDate)} — ${dateFormat.format(endDate)}"
            else -> "Период: всё время"
        }
    }

    private fun setupSpinners() {
        val typeOptions = arrayOf("Все операции", "Доходы", "Расходы")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilterType.adapter = typeAdapter
        spinnerFilterType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateSecondSpinner(position)
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val categoryOptions = mutableListOf("Все категории")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSecond.adapter = categoryAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            val customCategories = db.customCategoryDao().getAllCategories()
            withContext(Dispatchers.Main) {
                expenseCategories.clear()
                for (cat in customCategories) {
                    expenseCategories.add(cat.name)
                }
            }
            loadIncomeCategories()
        }
    }

    private fun updateSecondSpinner(typePosition: Int) {
        val items = when (typePosition) {
            0 -> sortOptions
            1 -> incomeCategories
            2 -> expenseCategories
            else -> listOf()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSecond.adapter = adapter

        spinnerSecond.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (typePosition) {
                    0 -> {
                        currentSortMode = when (position) {
                            0 -> "date_desc"
                            1 -> "date_asc"
                            2 -> "amount_desc"
                            3 -> "amount_asc"
                            4 -> "cash_first"
                            5 -> "card_first"
                            else -> "date_desc"
                        }
                    }
                }
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadIncomeCategories() {
        val prefs = getSharedPreferences("budget_data", MODE_PRIVATE)
        val savedCategories = prefs.getString("income_categories", "")
        incomeCategories.clear()
        if (savedCategories.isNullOrEmpty()) {
            incomeCategories.addAll(listOf("Основной доход"))
        } else {
            incomeCategories.addAll(savedCategories.split("|||"))
        }
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            allTransactions = db.transactionDao().getAllTransactions()
            withContext(Dispatchers.Main) {
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val typePosition = spinnerFilterType.selectedItemPosition
        val selectedSecondItem = if (spinnerSecond.selectedItem != null) {
            spinnerSecond.selectedItem.toString()
        } else {
            when (typePosition) {
                0 -> "📅 По дате (сначала новые)"
                1 -> "Все категории"
                2 -> "Все категории"
                else -> ""
            }
        }

        filteredTransactions = allTransactions.filter { transaction ->
            val typeMatch = when (typePosition) {
                1 -> transaction.type == "Доход"
                2 -> transaction.type == "Расход"
                else -> true
            }

            val categoryMatch = when (typePosition) {
                1 -> selectedSecondItem == "Все категории" || transaction.category == selectedSecondItem
                2 -> selectedSecondItem == "Все категории" || transaction.category == selectedSecondItem
                else -> true
            }

            val dateMatch = when {
                startDate != null && endDate != null && startDate == endDate ->
                    isSameDay(transaction.date, startDate!!)
                startDate != null && endDate != null ->
                    transaction.date in startDate!!..endDate!!
                else -> true
            }
            typeMatch && categoryMatch && dateMatch
        }

        if (typePosition == 0) {
            filteredTransactions = when (currentSortMode) {
                "date_desc" -> filteredTransactions.sortedByDescending { it.date }
                "date_asc" -> filteredTransactions.sortedBy { it.date }
                "amount_desc" -> filteredTransactions.sortedByDescending { it.amount }
                "amount_asc" -> filteredTransactions.sortedBy { it.amount }
                "cash_first" -> filteredTransactions.sortedByDescending { it.paymentMethod == "💵 Наличные" }
                "card_first" -> filteredTransactions.sortedByDescending { it.paymentMethod == "💳 Безналичные" }
                else -> filteredTransactions.sortedByDescending { it.date }
            }
        } else {
            filteredTransactions = filteredTransactions.sortedByDescending { it.date }
        }

        adapter = HistoryAdapter(this, filteredTransactions)
        listView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun showEditDialog(transaction: TransactionEntity) {
        val categories = if (transaction.type == "Доход") {
            incomeCategories
        } else {
            loadExpenseCategories()
            expenseCategories
        }

        dialogManager.showEditTransactionDialog(
            transaction.amount,
            transaction.category,
            transaction.paymentMethod,
            categories
        ) { newAmount, newCategory, newPaymentMethod ->
            updateTransaction(transaction, newAmount, newCategory, newPaymentMethod)
        }
    }

    private fun loadExpenseCategories(): List<String> {
        val prefs = getSharedPreferences("budget_data", MODE_PRIVATE)
        val savedCategories = prefs.getString("expense_categories", "")
        expenseCategories.clear()
        if (savedCategories.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val categories = db.customCategoryDao().getAllCategories()
                withContext(Dispatchers.Main) {
                    for (cat in categories) {
                        expenseCategories.add(cat.name)
                    }
                }
            }
        } else {
            expenseCategories.addAll(savedCategories.split("|||"))
        }
        return expenseCategories
    }

    private fun updateTransaction(oldTransaction: TransactionEntity, newAmount: Double, newCategory: String, newPaymentMethod: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedTransaction = oldTransaction.copy(
                amount = newAmount,
                category = newCategory,
                paymentMethod = newPaymentMethod,
                balanceAfter = oldTransaction.balanceAfter
            )
            db.transactionDao().update(updatedTransaction)
            withContext(Dispatchers.Main) {
                loadData()
                Toast.makeText(this@HistoryActivity, "Операция обновлена", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteDialog(transaction: TransactionEntity) {
        dialogManager.showDeleteConfirmDialog(transaction.amount) {
            lifecycleScope.launch(Dispatchers.IO) {
                db.transactionDao().deleteTransaction(transaction.id)
                withContext(Dispatchers.Main) {
                    loadData()
                    Toast.makeText(this@HistoryActivity, "Операция удалена", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class HistoryAdapter(context: Context, private val transactions: List<TransactionEntity>) :
        ArrayAdapter<TransactionEntity>(context, 0, transactions) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false)
            val transaction = getItem(position)!!

            val title = view.findViewById<TextView>(android.R.id.text1)
            val subtitle = view.findViewById<TextView>(android.R.id.text2)

            val emoji = if (transaction.type == "Доход") "🟢" else "🔴"
            val dateStr = dateFormat.format(transaction.date)

            title.text = "$emoji ${transaction.type}: ${transaction.amount} ₽ | ${transaction.paymentMethod}"
            subtitle.text = "$dateStr | ${transaction.category} | Остаток: ${transaction.balanceAfter} ₽"

            return view
        }
    }
}