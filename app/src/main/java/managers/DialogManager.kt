package com.example.familybudget.managers

import android.app.AlertDialog
import android.content.Context
import android.widget.*
import com.example.familybudget.database.TransactionEntity

class DialogManager(private val context: Context) {

    fun showPaymentMethodDialog(onResult: (String) -> Unit) {
        val options = arrayOf("💵 Наличные", "💳 Безналичные")
        AlertDialog.Builder(context)
            .setTitle("Способ оплаты")
            .setItems(options) { _, which ->
                onResult(options[which])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun showIncomeCategoryDialog(categories: List<String>, onSelect: (String) -> Unit, onAdd: () -> Unit) {
        if (categories.isEmpty()) {
            Toast.makeText(context, "Сначала добавьте категории доходов", Toast.LENGTH_LONG).show()
            return
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Выберите категорию дохода")
        builder.setItems(categories.toTypedArray()) { _, which ->
            onSelect(categories[which])
        }
        builder.setNeutralButton("➕ Добавить категорию") { _, _ ->
            onAdd()
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }

    fun showExpenseCategoryDialog(categories: List<String>, onSelect: (String) -> Unit, onAdd: () -> Unit) {
        if (categories.isEmpty()) {
            Toast.makeText(context, "Сначала добавьте категории в «Обязательные траты»", Toast.LENGTH_LONG).show()
            return
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Выберите категорию расхода")
        builder.setItems(categories.toTypedArray()) { _, which ->
            onSelect(categories[which])
        }
        builder.setNeutralButton("➕ Добавить категорию") { _, _ ->
            onAdd()
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }

    fun showAddCategoryDialog(title: String, hint: String, onResult: (String) -> Unit) {
        val input = EditText(context)
        input.hint = hint
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val newCategory = input.text.toString().trim()
                if (newCategory.isNotEmpty()) {
                    onResult(newCategory)
                } else {
                    Toast.makeText(context, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun showEditDeleteDialog(
        transaction: TransactionEntity,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val options = arrayOf("✏️ Редактировать", "🗑️ Удалить")
        AlertDialog.Builder(context)
            .setTitle("Действие с операцией")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onEdit()
                    1 -> onDelete()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun showEditTransactionDialog(
        amount: Double,
        category: String,
        paymentMethod: String,
        categories: List<String>,
        onSave: (Double, String, String) -> Unit
    ) {
        val dialogView = LinearLayout(context)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(60, 40, 60, 40)

        val amountInput = EditText(context)
        amountInput.hint = "Сумма"
        amountInput.setText(amount.toString())
        amountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val categorySpinner = Spinner(context)
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        val categoryPos = categories.indexOf(category)
        if (categoryPos >= 0) categorySpinner.setSelection(categoryPos)

        val paymentOptions = arrayOf("💵 Наличные", "💳 Безналичные")
        val paymentSpinner = Spinner(context)
        val paymentAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, paymentOptions)
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paymentSpinner.adapter = paymentAdapter
        val paymentPos = paymentOptions.indexOf(paymentMethod)
        if (paymentPos >= 0) paymentSpinner.setSelection(paymentPos)

        // Кнопка "Добавить категорию"
        val addCategoryButton = Button(context)
        addCategoryButton.text = "+ Добавить новую категорию"
        addCategoryButton.setOnClickListener {
            showAddCategoryDialog("Новая категория", "Введите название категории") { newCategory ->
                // Добавляем новую категорию в список и выбираем её
                (categories as MutableList).add(newCategory)
                categoryAdapter.notifyDataSetChanged()
                categorySpinner.setSelection(categories.size - 1)
                Toast.makeText(context, "Категория '$newCategory' добавлена", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.addView(amountInput)
        dialogView.addView(categorySpinner)
        dialogView.addView(paymentSpinner)
        dialogView.addView(addCategoryButton)

        AlertDialog.Builder(context)
            .setTitle("Редактировать операцию")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newAmount = amountInput.text.toString().toDoubleOrNull()
                val newCategory = categorySpinner.selectedItem.toString()
                val newPaymentMethod = paymentSpinner.selectedItem.toString()
                if (newAmount != null && newAmount > 0) {
                    onSave(newAmount, newCategory, newPaymentMethod)
                } else {
                    Toast.makeText(context, "Введите корректную сумму", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun showDeleteConfirmDialog(amount: Double, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Удаление операции")
            .setMessage("Удалить операцию на сумму ${amount} ₽?")
            .setPositiveButton("Удалить") { _, _ -> onConfirm() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun showUndoConfirmDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Отмена операции")
            .setMessage("Отменить последнюю операцию?")
            .setPositiveButton("Да") { _, _ -> onConfirm() }
            .setNegativeButton("Нет", null)
            .show()
    }
}