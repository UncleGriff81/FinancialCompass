package com.example.familybudget

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.familybudget.database.*

class FixedExpensesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomCategoryAdapter
    private lateinit var btnAddCategory: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fixed_expenses)

        recyclerView = findViewById(R.id.rv_categories)
        btnAddCategory = findViewById(R.id.btn_add_category)
        btnBack = findViewById(R.id.btn_back_to_main)

        recyclerView.layoutManager = LinearLayoutManager(this)

        db = AppDatabase.getInstance(this)

        loadCategories()

        btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCategories() {
        Thread {
            val categories = db.customCategoryDao().getAllCategories()
            runOnUiThread {
                adapter = CustomCategoryAdapter(categories, db.customCategoryDao(), db) {
                    loadCategories()
                }
                recyclerView.adapter = adapter
            }
        }.start()
    }

    private fun showAddCategoryDialog() {
        val dialogView = LinearLayout(this)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(60, 40, 60, 40)

        val nameInput = EditText(this)
        nameInput.hint = "Название категории"
        nameInput.inputType = android.text.InputType.TYPE_CLASS_TEXT

        val limitInput = EditText(this)
        limitInput.hint = "Лимит (₽)"
        limitInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        dialogView.addView(nameInput)
        dialogView.addView(limitInput)

        AlertDialog.Builder(this)
            .setTitle("Новая категория")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = nameInput.text.toString().trim()
                val limit = limitInput.text.toString().toDoubleOrNull()
                if (name.isNotEmpty() && limit != null && limit >= 0) {
                    val newCategory = CustomCategoryEntity(
                        name = name,
                        limitAmount = limit,
                        remainingAmount = limit
                    )
                    Thread {
                        db.customCategoryDao().insert(newCategory)
                        runOnUiThread {
                            Toast.makeText(this@FixedExpensesActivity, "✅ Категория '$name' добавлена в БД", Toast.LENGTH_LONG).show()
                        }
                        loadCategories()
                    }.start()
                    Toast.makeText(this, "Категория '$name' добавлена с лимитом ${limit} ₽", Toast.LENGTH_SHORT).show()
                } else if (name.isNotEmpty() && limit == null) {
                    val newCategory = CustomCategoryEntity(
                        name = name,
                        limitAmount = 0.0,
                        remainingAmount = 0.0
                    )
                    Thread {
                        db.customCategoryDao().insert(newCategory)
                        runOnUiThread {
                            Toast.makeText(this@FixedExpensesActivity, "✅ Категория '$name' добавлена в БД (без лимита)", Toast.LENGTH_LONG).show()
                        }
                        loadCategories()
                    }.start()
                    Toast.makeText(this, "Категория '$name' добавлена без лимита", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Введите название категории", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    companion object {
        fun updateRemainingAfterExpense(categoryName: String, amount: Double, db: AppDatabase, context: Context) {
            Thread {
                try {
                    val allCategories = db.customCategoryDao().getAllCategories()
                    val category = allCategories.find { it.name == categoryName }
                    if (category != null) {
                        val oldRemaining = category.remainingAmount
                        val newRemaining = oldRemaining - amount
                        val updated = category.copy(remainingAmount = newRemaining)
                        db.customCategoryDao().update(updated)

                        if (oldRemaining >= 0 && newRemaining < 0) {
                            val overspent = -newRemaining
                            val notificationHelper = NotificationHelper(context)
                            notificationHelper.showLimitExceededNotification(categoryName, overspent)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }
}