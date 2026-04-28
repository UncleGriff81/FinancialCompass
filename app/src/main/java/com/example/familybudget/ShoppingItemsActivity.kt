package com.example.familybudget

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.ShoppingItemEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ShoppingItemsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingItemsAdapter
    private lateinit var tvTotal: TextView
    private lateinit var etItemName: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etPrice: EditText
    private lateinit var btnAddItem: Button
    private lateinit var btnComplete: ImageButton
    private lateinit var btnShare: ImageButton

    private var listId: Long = 0
    private var listName: String = ""
    private var itemsList = mutableListOf<ShoppingItemEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_items)

        db = AppDatabase.getInstance(this)

        listId = intent.getLongExtra("list_id", 0)
        listName = intent.getStringExtra("list_name") ?: "Список покупок"

        if (listId == 0L) {
            Toast.makeText(this, "Ошибка: список не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.rv_items)
        tvTotal = findViewById(R.id.tv_total)
        etItemName = findViewById(R.id.et_item_name)
        etQuantity = findViewById(R.id.et_quantity)
        etPrice = findViewById(R.id.et_price)
        btnAddItem = findViewById(R.id.btn_add_item)
        btnComplete = findViewById(R.id.btn_complete)
        btnShare = findViewById(R.id.btn_share)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ShoppingItemsAdapter(
            itemsList,
            { item, isChecked -> updateItemChecked(item, isChecked) },
            { item -> updateItem(item) },
            { item -> deleteItem(item) }
        )
        recyclerView.adapter = adapter

        loadItems()

        btnAddItem.setOnClickListener { addItem() }

        btnShare.setOnClickListener {
            shareShoppingList()
        }

        btnComplete.setOnClickListener {
            showCompleteConfirmationDialog()
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = listName
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun shareShoppingList() {
        if (itemsList.isEmpty()) {
            Toast.makeText(this, "Нет товаров для экспорта", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${listName}_${timestamp}_shopping_list.txt"

                val content = buildShoppingListContent()

                val file = File(cacheDir, fileName)
                FileWriter(file).use { writer ->
                    writer.write(content)
                }

                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Поделиться списком"))

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun buildShoppingListContent(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val content = StringBuilder()
        content.append("=========================================\n")
        content.append("СПИСОК ПОКУПОК: ${listName.uppercase()}\n")
        content.append("Дата: ${dateFormat.format(Date())}\n")
        content.append("=========================================\n\n")

        content.append("КУПЛЕНО:\n")
        content.append("-----------------------------------------\n")
        var hasChecked = false
        for (item in itemsList.filter { it.isChecked }) {
            hasChecked = true
            content.append("  • ${item.name}\n")
            content.append("    Количество: ${item.quantity} × ${item.unitPrice} ₽ = ${item.quantity * item.unitPrice} ₽\n\n")
        }
        if (!hasChecked) {
            content.append("  (нет купленных товаров)\n\n")
        }

        content.append("\nНЕ КУПЛЕНО:\n")
        content.append("-----------------------------------------\n")
        var hasUnchecked = false
        for (item in itemsList.filter { !it.isChecked }) {
            hasUnchecked = true
            content.append("  • ${item.name}\n")
            content.append("    Количество: ${item.quantity} × ${item.unitPrice} ₽ = ${item.quantity * item.unitPrice} ₽\n\n")
        }
        if (!hasUnchecked) {
            content.append("  (все товары куплены!)\n\n")
        }

        content.append("\n=========================================\n")
        val totalChecked = itemsList.filter { it.isChecked }.sumOf { it.quantity * it.unitPrice }
        content.append("ИТОГО ПОКУПКА: ${String.format("%.2f", totalChecked)} ₽\n")
        content.append("=========================================\n")

        return content.toString()
    }

    private fun showCompleteConfirmationDialog() {
        val checkedCount = itemsList.count { it.isChecked }
        val totalPrice = itemsList.filter { it.isChecked }.sumOf { it.quantity * it.unitPrice }

        if (checkedCount == 0) {
            Toast.makeText(this, "Нет отмеченных товаров", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val categories = db.customCategoryDao().getAllCategories()
            runOnUiThread {
                if (categories.isEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Завершить покупку")
                        .setMessage("Куплено товаров: $checkedCount\nОбщая сумма: ${String.format("%.2f", totalPrice)} ₽\n\nНет категорий расходов.\nСначала добавьте категории в «Обязательные траты».")
                        .setPositiveButton("Сохранить список") { _, _ ->
                            saveShoppingListToFile()
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                    return@runOnUiThread
                }

                val categoryNames = categories.map { it.name }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Выберите категорию расхода")
                    .setItems(categoryNames) { _, which ->
                        val selectedCategory = categories[which]
                        addExpenseToBudget(selectedCategory.name, totalPrice)
                        saveShoppingListToFile()
                        Toast.makeText(this@ShoppingItemsActivity,
                            "Расход ${String.format("%.2f", totalPrice)} ₽ добавлен в категорию \"${selectedCategory.name}\"",
                            Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton("Отмена") { _, _ ->
                        saveShoppingListToFile()
                    }
                    .show()
            }
        }.start()
    }

    private fun addExpenseToBudget(categoryName: String, amount: Double) {
        Thread {
            try {
                var balance = db.balanceDao().getBalance()
                if (balance == null) {
                    balance = com.example.familybudget.database.BalanceEntity(cashBalance = 0.0, cardBalance = 0.0)
                    db.balanceDao().insert(balance)
                }

                val newCardBalance = balance.cardBalance - amount
                val updatedBalance = balance.copy(cardBalance = newCardBalance)
                db.balanceDao().update(updatedBalance)

                val categories = db.customCategoryDao().getAllCategories()
                val category = categories.find { it.name == categoryName }
                if (category != null) {
                    val newRemaining = category.remainingAmount - amount
                    val updatedCategory = category.copy(remainingAmount = newRemaining)
                    db.customCategoryDao().update(updatedCategory)
                }

                val totalBalance = balance.cashBalance + newCardBalance
                val transaction = com.example.familybudget.database.TransactionEntity(
                    type = "Расход",
                    category = categoryName,
                    amount = amount,
                    paymentMethod = "💳 Безналичные",
                    date = Date(),
                    balanceAfter = totalBalance
                )
                db.transactionDao().insert(transaction)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun saveShoppingListToFile() {
        Thread {
            try {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${listName}_${timestamp}_shopping_list.txt"

                val content = buildShoppingListContent()

                saveTextFile(fileName, content)

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun saveTextFile(fileName: String, content: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    runOnUiThread {
                        Toast.makeText(this, "Список сохранён в папке «Документы»", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } ?: runOnUiThread {
                    Toast.makeText(this, "Не удалось создать файл", Toast.LENGTH_SHORT).show()
                }
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, fileName)
                FileWriter(file).use { writer ->
                    writer.write(content)
                }
                runOnUiThread {
                    Toast.makeText(this, "Список сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            }
            e.printStackTrace()
        }
    }

    private fun loadItems() {
        Thread {
            val items = db.shoppingItemDao().getItemsByListId(listId)
            runOnUiThread {
                itemsList.clear()
                itemsList.addAll(items)
                adapter.updateData(itemsList)
                updateTotal()
            }
        }.start()
    }

    private fun updateTotal() {
        val total = adapter.getTotalPrice()
        tvTotal.text = String.format("Итого: %.2f ₽", total)
    }

    private fun addItem() {
        val name = etItemName.text.toString().trim()
        val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0

        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название товара", Toast.LENGTH_SHORT).show()
            return
        }
        if (quantity <= 0) {
            Toast.makeText(this, "Введите корректное количество", Toast.LENGTH_SHORT).show()
            return
        }
        if (price <= 0) {
            Toast.makeText(this, "Введите корректную цену", Toast.LENGTH_SHORT).show()
            return
        }

        val item = ShoppingItemEntity(
            listId = listId,
            name = name,
            quantity = quantity,
            unitPrice = price,
            isChecked = false
        )

        Thread {
            db.shoppingItemDao().insert(item)
            runOnUiThread {
                loadItems()
                etItemName.text.clear()
                etQuantity.text.clear()
                etPrice.text.clear()
                Toast.makeText(this, "Товар добавлен", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun updateItemChecked(item: ShoppingItemEntity, isChecked: Boolean) {
        Thread {
            val updatedItem = item.copy(isChecked = isChecked)
            db.shoppingItemDao().update(updatedItem)
            runOnUiThread { loadItems() }
        }.start()
    }

    private fun updateItem(item: ShoppingItemEntity) {
        Thread {
            db.shoppingItemDao().update(item)
            runOnUiThread { loadItems() }
        }.start()
    }

    private fun deleteItem(item: ShoppingItemEntity) {
        Thread {
            db.shoppingItemDao().delete(item)
            runOnUiThread { loadItems() }
        }.start()
    }
}