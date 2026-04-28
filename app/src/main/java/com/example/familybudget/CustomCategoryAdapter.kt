package com.example.familybudget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.CustomCategoryDao
import com.example.familybudget.database.CustomCategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomCategoryAdapter(
    private var categories: List<CustomCategoryEntity>,
    private val dao: CustomCategoryDao,
    private val db: AppDatabase,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<CustomCategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<CustomCategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_category_title)
        private val tvRemaining: TextView = itemView.findViewById(R.id.tv_category_remaining)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_category)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_category)

        fun bind(category: CustomCategoryEntity) {
            tvTitle.text = category.name
            val remaining = category.remainingAmount
            if (remaining >= 0) {
                tvRemaining.text = String.format("💰 Остаток: %.2f ₽", remaining)
                tvRemaining.setTextColor(android.graphics.Color.rgb(0, 128, 0))
            } else {
                tvRemaining.text = String.format("💰 Остаток: 0 ₽   |   🔴 Перерасход: %.2f ₽", -remaining)
                tvRemaining.setTextColor(android.graphics.Color.rgb(255, 0, 0))
            }

            btnEdit.setOnClickListener {
                showEditDialog(category)
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Удаление категории")
                    .setMessage("Удалить категорию \"${category.name}\"? Все связанные расходы останутся с этой категорией.")
                    .setPositiveButton("Удалить") { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.delete(category)
                            onDataChanged()
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }

        private fun showEditDialog(category: CustomCategoryEntity) {
            val dialogView = LinearLayout(itemView.context)
            dialogView.orientation = LinearLayout.VERTICAL
            dialogView.setPadding(60, 40, 60, 40)

            val nameInput = EditText(itemView.context)
            nameInput.hint = "Название категории"
            nameInput.setText(category.name)
            nameInput.inputType = android.text.InputType.TYPE_CLASS_TEXT

            val limitInput = EditText(itemView.context)
            limitInput.hint = "Лимит (₽)"
            limitInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            limitInput.setText(category.limitAmount.toString())

            dialogView.addView(nameInput)
            dialogView.addView(limitInput)

            AlertDialog.Builder(itemView.context)
                .setTitle("Редактировать категорию")
                .setView(dialogView)
                .setPositiveButton("Сохранить") { _, _ ->
                    val newName = nameInput.text.toString().trim()
                    val newLimit = limitInput.text.toString().toDoubleOrNull()
                    if (newName.isNotEmpty() && newLimit != null && newLimit > 0) {
                        // Рассчитываем сумму уже потраченных средств по этой категории
                        Thread {
                            val transactions = db.transactionDao().getAllTransactions()
                            var spent = 0.0
                            for (t in transactions) {
                                if (t.type == "Расход" && t.category == category.name) {
                                    spent += t.amount
                                }
                            }
                            val newRemaining = newLimit - spent
                            val updatedCategory = category.copy(
                                name = newName,
                                limitAmount = newLimit,
                                remainingAmount = newRemaining
                            )
                            dao.update(updatedCategory)
                            onDataChanged()
                        }.start()
                    } else {
                        Toast.makeText(itemView.context, "Введите корректную сумму лимита", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}