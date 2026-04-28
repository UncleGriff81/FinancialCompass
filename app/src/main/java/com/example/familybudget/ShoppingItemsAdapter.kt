package com.example.familybudget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.familybudget.database.ShoppingItemEntity

class ShoppingItemsAdapter(
    private var items: List<ShoppingItemEntity>,
    private val onItemChecked: (ShoppingItemEntity, Boolean) -> Unit,
    private val onItemUpdated: (ShoppingItemEntity) -> Unit,
    private val onItemDeleted: (ShoppingItemEntity) -> Unit
) : RecyclerView.Adapter<ShoppingItemsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ShoppingItemEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getTotalPrice(): Double {
        return items.sumOf { it.quantity * it.unitPrice }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.cb_checked)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvPrice: TextView = itemView.findViewById(R.id.tv_price)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(item: ShoppingItemEntity) {
            // Убираем старый слушатель, чтобы избежать дублирования
            checkBox.setOnCheckedChangeListener(null)

            // Устанавливаем состояние
            checkBox.isChecked = item.isChecked
            tvName.text = item.name
            tvPrice.text = String.format("%.2f x %.2f = %.2f ₽", item.quantity, item.unitPrice, item.quantity * item.unitPrice)

            // Добавляем новый слушатель
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onItemChecked(item, isChecked)
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Удаление товара")
                    .setMessage("Удалить товар \"${item.name}\"?")
                    .setPositiveButton("Удалить") { _, _ ->
                        onItemDeleted(item)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }

            itemView.setOnLongClickListener {
                showEditDialog(item)
                true
            }
        }

        private fun showEditDialog(item: ShoppingItemEntity) {
            val dialogView = LinearLayout(itemView.context)
            dialogView.orientation = LinearLayout.VERTICAL
            dialogView.setPadding(60, 40, 60, 40)

            val tvNameLabel = TextView(itemView.context)
            tvNameLabel.text = "Название"
            tvNameLabel.textSize = 12f
            tvNameLabel.setTextColor(android.graphics.Color.GRAY)

            val nameInput = EditText(itemView.context)
            nameInput.hint = "Введите название"
            nameInput.setText(item.name)

            val tvQuantityLabel = TextView(itemView.context)
            tvQuantityLabel.text = "Количество"
            tvQuantityLabel.textSize = 12f
            tvQuantityLabel.setTextColor(android.graphics.Color.GRAY)

            val quantityInput = EditText(itemView.context)
            quantityInput.hint = "Введите количество"
            quantityInput.setText(item.quantity.toString())
            quantityInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

            val tvPriceLabel = TextView(itemView.context)
            tvPriceLabel.text = "Цена за единицу"
            tvPriceLabel.textSize = 12f
            tvPriceLabel.setTextColor(android.graphics.Color.GRAY)

            val priceInput = EditText(itemView.context)
            priceInput.hint = "Введите цену"
            priceInput.setText(item.unitPrice.toString())
            priceInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

            dialogView.addView(tvNameLabel)
            dialogView.addView(nameInput)
            dialogView.addView(tvQuantityLabel)
            dialogView.addView(quantityInput)
            dialogView.addView(tvPriceLabel)
            dialogView.addView(priceInput)

            AlertDialog.Builder(itemView.context)
                .setTitle("Редактировать товар")
                .setView(dialogView)
                .setPositiveButton("Сохранить") { _, _ ->
                    val newName = nameInput.text.toString().trim()
                    val newQuantity = quantityInput.text.toString().toDoubleOrNull() ?: 0.0
                    val newPrice = priceInput.text.toString().toDoubleOrNull() ?: 0.0
                    if (newName.isNotEmpty() && newQuantity > 0 && newPrice > 0) {
                        val updatedItem = item.copy(name = newName, quantity = newQuantity, unitPrice = newPrice)
                        onItemUpdated(updatedItem)
                    } else {
                        Toast.makeText(itemView.context, "Заполните все поля корректно", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}