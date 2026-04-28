package com.example.familybudget.managers

import android.content.Context
import android.content.SharedPreferences
import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.CustomCategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryManager(private val context: Context, private val db: AppDatabase) {

    private val prefs: SharedPreferences = context.getSharedPreferences("budget_data", Context.MODE_PRIVATE)
    val expenseCategories = mutableListOf<String>()
    val incomeCategories = mutableListOf<String>()

    fun loadIncomeCategories() {
        val savedCategories = prefs.getString("income_categories", "")
        if (savedCategories.isNullOrEmpty()) {
            incomeCategories.clear()
            incomeCategories.addAll(listOf("Основной доход"))
            saveIncomeCategories()
        } else {
            incomeCategories.clear()
            incomeCategories.addAll(savedCategories.split("|||"))
        }
    }

    fun saveIncomeCategories() {
        val editor = prefs.edit()
        editor.putString("income_categories", incomeCategories.joinToString("|||"))
        editor.apply()
    }

    fun addIncomeCategory(name: String) {
        if (!incomeCategories.contains(name)) {
            incomeCategories.add(name)
            saveIncomeCategories()
        }
    }

    suspend fun loadExpenseCategoriesFromDb() {
        withContext(Dispatchers.IO) {
            val customCategories = db.customCategoryDao().getAllCategories()
            withContext(Dispatchers.Main) {
                expenseCategories.clear()
                for (cat in customCategories) {
                    expenseCategories.add(cat.name)
                }
                saveExpenseCategories()
            }
        }
    }

    fun saveExpenseCategories() {
        val editor = prefs.edit()
        editor.putString("expense_categories", expenseCategories.joinToString("|||"))
        editor.apply()
    }

    suspend fun addExpenseCategoryToDb(name: String) {
        withContext(Dispatchers.IO) {
            val existingCategories = db.customCategoryDao().getAllCategories()
            if (existingCategories.none { it.name == name }) {
                val newCatEntity = CustomCategoryEntity(
                    name = name,
                    limitAmount = 0.0,
                    remainingAmount = 0.0
                )
                db.customCategoryDao().insert(newCatEntity)
            }
        }
    }

    // Принудительная синхронизация категорий из БД
    suspend fun syncExpenseCategories() {
        withContext(Dispatchers.IO) {
            val customCategories = db.customCategoryDao().getAllCategories()
            withContext(Dispatchers.Main) {
                expenseCategories.clear()
                for (cat in customCategories) {
                    expenseCategories.add(cat.name)
                }
                saveExpenseCategories()
            }
        }
    }
}