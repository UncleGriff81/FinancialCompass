package com.example.familybudget

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.TransactionEntity
import com.example.familybudget.managers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var budgetManager: BudgetManager
    private lateinit var transactionManager: TransactionManager
    private lateinit var categoryManager: CategoryManager
    private lateinit var uiManager: UIManager
    private lateinit var dialogManager: DialogManager

    private lateinit var etAmount: EditText
    private lateinit var etMonthlyIncome: EditText
    private lateinit var btnApplyIncome: Button
    private lateinit var btnIncome: Button
    private lateinit var btnExpense: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        initViews()

        db = AppDatabase.getInstance(this)

        budgetManager = BudgetManager(db)
        transactionManager = TransactionManager(db)
        categoryManager = CategoryManager(this, db)
        dialogManager = DialogManager(this)

        uiManager = UIManager(
            findViewById(R.id.tv_free_balance),
            findViewById(R.id.tv_cash_balance),
            findViewById(R.id.tv_card_balance),
            findViewById(R.id.list_operations)
        )
        uiManager.initAdapter(this)

        setupClickListeners()
        setupLongPressListener()

        lifecycleScope.launch {
            budgetManager.loadBalances()
            categoryManager.loadIncomeCategories()
            categoryManager.syncExpenseCategories()
            loadOperations()
            uiManager.updateBalancesDisplay(budgetManager.cashBalance, budgetManager.cardBalance)
        }
    }

    private fun initViews() {
        etAmount = findViewById(R.id.et_amount)
        etMonthlyIncome = findViewById(R.id.et_monthly_income)
        btnApplyIncome = findViewById(R.id.btn_apply_income)
        btnIncome = findViewById(R.id.btn_income)
        btnExpense = findViewById(R.id.btn_expense)

        findViewById<Button>(R.id.btn_go_to_dreams).setOnClickListener {
            startActivity(android.content.Intent(this, DreamsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_shopping_lists).setOnClickListener {
            startActivity(android.content.Intent(this, ShoppingListsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_go_to_expenses).setOnClickListener {
            startActivity(android.content.Intent(this, FixedExpensesActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_undo).setOnClickListener {
            dialogManager.showUndoConfirmDialog { undoLastOperation() }
        }

        val btnMenu = findViewById<ImageButton>(R.id.btn_menu)
        btnMenu.setOnClickListener {
            showPopupMenu()
        }
    }

    private fun showPopupMenu() {
        val popupMenu = android.widget.PopupMenu(this, findViewById<ImageButton>(R.id.btn_menu))
        popupMenu.menuInflater.inflate(R.menu.bottom_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export -> {
                    exportDataToCsv()
                    true
                }
                R.id.action_stats -> {
                    startActivity(android.content.Intent(this, StatisticsActivity::class.java))
                    true
                }
                R.id.action_history -> {
                    startActivity(android.content.Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.action_settings -> {
                    startActivity(android.content.Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_feedback -> {
                    startActivity(android.content.Intent(this, FeedbackActivity::class.java))
                    true
                }
                R.id.action_about -> {
                    startActivity(android.content.Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setupLongPressListener() {
        findViewById<ListView>(R.id.list_operations).setOnItemLongClickListener { parent, view, position, id ->
            val item = uiManager.getOperationAtPosition(position)
            if (item.startsWith("———") && item.endsWith("———")) {
                return@setOnItemLongClickListener true
            }
            lifecycleScope.launch {
                val transactions = transactionManager.getAllTransactions()
                var transactionIndex = 0
                for (i in 0 until position) {
                    if (!uiManager.isDateSeparator(i)) transactionIndex++
                }
                if (transactionIndex < transactions.size) {
                    val selectedTransaction = transactions[transactionIndex]
                    runOnUiThread {
                        showEditDeleteDialog(selectedTransaction)
                    }
                }
            }
            true
        }
    }

    private fun setupClickListeners() {
        btnApplyIncome.setOnClickListener {
            val amount = etMonthlyIncome.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Введите сумму дохода", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialogManager.showPaymentMethodDialog { paymentMethod ->
                addOperation("Доход", "Основной доход", amount, paymentMethod)
                etMonthlyIncome.text.clear()
            }
        }

        btnIncome.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialogManager.showIncomeCategoryDialog(
                categoryManager.incomeCategories,
                { category ->
                    dialogManager.showPaymentMethodDialog { paymentMethod ->
                        addOperation("Доход", category, amount, paymentMethod)
                        etAmount.text.clear()
                    }
                },
                { showAddIncomeCategoryDialog(amount) }
            )
        }

        btnExpense.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCategoryDialogForExpense(amount)
        }
    }

    private fun showCategoryDialogForExpense(amount: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val customCategories = db.customCategoryDao().getAllCategories()
            withContext(Dispatchers.Main) {
                categoryManager.expenseCategories.clear()
                for (cat in customCategories) {
                    categoryManager.expenseCategories.add(cat.name)
                }
                categoryManager.saveExpenseCategories()

                if (categoryManager.expenseCategories.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Сначала добавьте категории в «Обязательные траты»", Toast.LENGTH_LONG).show()
                    return@withContext
                }

                dialogManager.showPaymentMethodDialog { paymentMethod ->
                    dialogManager.showExpenseCategoryDialog(
                        categoryManager.expenseCategories,
                        { category ->
                            addOperation("Расход", category, amount, paymentMethod)
                            etAmount.text.clear()
                        },
                        { showAddExpenseCategoryDialog(amount, paymentMethod) }
                    )
                }
            }
        }
    }

    private fun showAddExpenseCategoryDialog(amount: Double, paymentMethod: String) {
        dialogManager.showAddCategoryDialog("Новая категория", "Введите название категории") { newCategory ->
            lifecycleScope.launch(Dispatchers.IO) {
                val existingCategories = db.customCategoryDao().getAllCategories()
                if (existingCategories.none { it.name == newCategory }) {
                    val newCatEntity = com.example.familybudget.database.CustomCategoryEntity(
                        name = newCategory,
                        limitAmount = 0.0,
                        remainingAmount = 0.0
                    )
                    db.customCategoryDao().insert(newCatEntity)
                }

                withContext(Dispatchers.Main) {
                    categoryManager.syncExpenseCategories()
                    categoryManager.expenseCategories.add(newCategory)
                    categoryManager.saveExpenseCategories()

                    addOperation("Расход", newCategory, amount, paymentMethod)
                    etAmount.text.clear()
                }
            }
        }
    }

    private fun showAddIncomeCategoryDialog(amount: Double) {
        dialogManager.showAddCategoryDialog("Новая категория дохода", "Введите название категории") { newCategory ->
            categoryManager.addIncomeCategory(newCategory)
            dialogManager.showPaymentMethodDialog { paymentMethod ->
                addOperation("Доход", newCategory, amount, paymentMethod)
                etAmount.text.clear()
            }
        }
    }

    private fun addOperation(type: String, category: String, amount: Double, paymentMethod: String) {
        budgetManager.addToBalance(amount, paymentMethod, type == "Доход")

        if (type == "Расход") {
            FixedExpensesActivity.updateRemainingAfterExpense(category, amount, db, this)
        }

        lifecycleScope.launch {
            transactionManager.addTransaction(type, category, amount, paymentMethod, budgetManager.getTotalBalance())
            budgetManager.saveBalances()
            loadOperations()
        }

        uiManager.updateBalancesDisplay(budgetManager.cashBalance, budgetManager.cardBalance)
        Toast.makeText(this, "$type добавлен: $amount ₽ ($paymentMethod)", Toast.LENGTH_SHORT).show()
    }

    private suspend fun loadOperations() {
        val transactions = transactionManager.getAllTransactions()
        uiManager.displayOperations(transactions)
    }

    private fun undoLastOperation() {
        lifecycleScope.launch {
            val transactions = transactionManager.getAllTransactions()
            if (transactions.isNotEmpty()) {
                val last = transactions[0]
                budgetManager.revertBalanceChange(last.amount, last.paymentMethod, last.type == "Доход")
                budgetManager.saveBalances()
                transactionManager.deleteLastTransaction()
                loadOperations()
                uiManager.updateBalancesDisplay(budgetManager.cashBalance, budgetManager.cardBalance)
                Toast.makeText(this@MainActivity, "Последняя операция отменена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Нет операций для отмены", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportDataToCsv() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transactions = transactionManager.getAllTransactions()
                val categories = db.customCategoryDao().getAllCategories()

                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

                val csvTransactions = StringBuilder()
                csvTransactions.append("Дата;Тип;Категория;Сумма;Способ оплаты;Остаток после операции\n")
                for (t in transactions) {
                    csvTransactions.append("${dateFormat.format(t.date)};")
                    csvTransactions.append("${t.type};")
                    csvTransactions.append("${t.category};")
                    csvTransactions.append("${t.amount};")
                    csvTransactions.append("${t.paymentMethod};")
                    csvTransactions.append("${t.balanceAfter}\n")
                }

                val csvCategories = StringBuilder()
                csvCategories.append("Название категории;Лимит;Остаток\n")
                for (c in categories) {
                    csvCategories.append("${c.name};${c.limitAmount};${c.remainingAmount}\n")
                }

                val zipFileName = "financial_compass_$timestamp.zip"
                val zipFile = File(cacheDir, zipFileName)

                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    zos.putNextEntry(ZipEntry("transactions_$timestamp.csv"))
                    zos.write(csvTransactions.toString().toByteArray())
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("categories_$timestamp.csv"))
                    zos.write(csvCategories.toString().toByteArray())
                    zos.closeEntry()
                }

                saveZipFile(zipFileName, zipFile)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveZipFile(fileName: String, zipFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        FileInputStream(zipFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(this, "Архив сохранён в папке «Загрузки»: $fileName", Toast.LENGTH_LONG).show()
                    }
                    zipFile.delete()
                } ?: runOnUiThread {
                    Toast.makeText(this, "Не удалось создать архив", Toast.LENGTH_SHORT).show()
                }
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val destFile = File(directory, fileName)
                zipFile.copyTo(destFile, overwrite = true)
                zipFile.delete()
                runOnUiThread {
                    Toast.makeText(this, "Архив сохранён: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            }
            e.printStackTrace()
        }
    }

    // ==================== РЕДАКТИРОВАНИЕ И УДАЛЕНИЕ ====================
    private fun showEditDeleteDialog(transaction: TransactionEntity) {
        dialogManager.showEditDeleteDialog(
            transaction,
            onEdit = { showEditTransactionDialog(transaction) },
            onDelete = { showDeleteConfirmationDialog(transaction) }
        )
    }

    private fun showEditTransactionDialog(transaction: TransactionEntity) {
        val categories = if (transaction.type == "Доход") categoryManager.incomeCategories else categoryManager.expenseCategories
        dialogManager.showEditTransactionDialog(
            transaction.amount,
            transaction.category,
            transaction.paymentMethod,
            categories
        ) { newAmount, newCategory, newPaymentMethod ->
            updateTransaction(transaction, newAmount, newCategory, newPaymentMethod)
        }
    }

    private fun showDeleteConfirmationDialog(transaction: TransactionEntity) {
        dialogManager.showDeleteConfirmDialog(transaction.amount) {
            deleteTransaction(transaction)
        }
    }

    private fun updateTransaction(oldTransaction: TransactionEntity, newAmount: Double, newCategory: String, newPaymentMethod: String) {
        lifecycleScope.launch {
            budgetManager.revertBalanceChange(oldTransaction.amount, oldTransaction.paymentMethod, oldTransaction.type == "Доход")
            budgetManager.addToBalance(newAmount, newPaymentMethod, oldTransaction.type == "Доход")

            if (oldTransaction.type == "Расход") {
                FixedExpensesActivity.updateRemainingAfterExpense(oldTransaction.category, -oldTransaction.amount, db, this@MainActivity)
                FixedExpensesActivity.updateRemainingAfterExpense(newCategory, newAmount, db, this@MainActivity)
            }

            transactionManager.updateTransaction(oldTransaction, newAmount, newCategory, newPaymentMethod, budgetManager.getTotalBalance())
            budgetManager.saveBalances()
            loadOperations()
            uiManager.updateBalancesDisplay(budgetManager.cashBalance, budgetManager.cardBalance)
            Toast.makeText(this@MainActivity, "Операция обновлена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteTransaction(transaction: TransactionEntity) {
        lifecycleScope.launch {
            budgetManager.revertBalanceChange(transaction.amount, transaction.paymentMethod, transaction.type == "Доход")

            if (transaction.type == "Расход") {
                FixedExpensesActivity.updateRemainingAfterExpense(transaction.category, -transaction.amount, db, this@MainActivity)
            }

            transactionManager.deleteTransaction(transaction)
            budgetManager.saveBalances()
            loadOperations()
            uiManager.updateBalancesDisplay(budgetManager.cashBalance, budgetManager.cardBalance)
            Toast.makeText(this@MainActivity, "Операция удалена", Toast.LENGTH_SHORT).show()
        }
    }
}