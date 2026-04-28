package com.example.familybudget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.familybudget.database.ShoppingListEntity
import java.text.SimpleDateFormat
import java.util.*

class ShoppingListsAdapter(
    private var lists: List<ShoppingListEntity>,
    private val onItemClick: (ShoppingListEntity) -> Unit,
    private val onDeleteClick: (ShoppingListEntity) -> Unit
) : RecyclerView.Adapter<ShoppingListsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = lists[position]
        holder.bind(list)
    }

    override fun getItemCount(): Int = lists.size

    fun updateData(newLists: List<ShoppingListEntity>) {
        lists = newLists
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(android.R.id.text1)
        private val tvSubtitle: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(list: ShoppingListEntity) {
            tvTitle.text = list.name
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(list.date))
            tvSubtitle.text = if (list.isCompleted) "✅ Завершён: $dateStr" else "📅 Создан: $dateStr"

            itemView.setOnClickListener { onItemClick(list) }
            itemView.setOnLongClickListener {
                onDeleteClick(list)
                true
            }
        }
    }
}