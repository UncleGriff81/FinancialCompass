package com.example.familybudget.managers

import android.content.res.Configuration
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.example.familybudget.database.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

class UIManager(
    private val tvFreeBalance: TextView,
    private val tvCashBalance: TextView,
    private val tvCardBalance: TextView,
    private val listView: ListView
) {

    private val operations = mutableListOf<String>()
    lateinit var adapter: ArrayAdapter<String>

    fun initAdapter(context: android.content.Context) {
        adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, operations) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                val item = getItem(position)

                val isDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val defaultTextColor = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK

                if (item != null && item.startsWith("———") && item.endsWith("———")) {
                    textView.setTypeface(null, android.graphics.Typeface.BOLD)
                    textView.setTextColor(android.graphics.Color.rgb(100, 100, 100))
                    textView.setBackgroundColor(if (isDarkTheme) android.graphics.Color.rgb(50, 50, 50) else android.graphics.Color.rgb(240, 240, 240))
                } else if (item != null && item.contains("★ Основной доход ★")) {
                    textView.setTypeface(null, android.graphics.Typeface.BOLD)
                    textView.setTextColor(android.graphics.Color.rgb(33, 150, 243))
                } else {
                    textView.setTypeface(null, android.graphics.Typeface.NORMAL)
                    textView.setTextColor(defaultTextColor)
                }
                return view
            }
        }
        listView.adapter = adapter
    }

    fun updateBalancesDisplay(cashBalance: Double, cardBalance: Double) {
        val total = cashBalance + cardBalance
        tvFreeBalance.text = String.format("%.2f ₽", total)
        tvCashBalance.text = String.format("💵 Наличные: %.2f ₽", cashBalance)
        tvCardBalance.text = String.format("   |   💳 Безналичные: %.2f ₽", cardBalance)
    }

    fun displayOperations(transactions: List<TransactionEntity>) {
        operations.clear()
        var lastDate: String? = null
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        for (t in transactions) {
            val currentDate = dateFormat.format(t.date)
            if (lastDate != currentDate) {
                operations.add("——— $currentDate ———")
                lastDate = currentDate
            }
            val emoji = if (t.type == "Доход") "🟢" else "🔴"
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(t.date)

            val text = if (t.type == "Доход" && t.category == "Основной доход") {
                "$emoji $timeStr | ${t.type}: ${t.amount} ₽ | ${t.paymentMethod} | ★ ${t.category} ★ | Остаток: ${t.balanceAfter} ₽"
            } else if (t.type == "Доход") {
                "$emoji $timeStr | ${t.type}: ${t.amount} ₽ | ${t.paymentMethod} | ${t.category} | Остаток: ${t.balanceAfter} ₽"
            } else {
                "$emoji $timeStr | ${t.type}: ${t.amount} ₽ | ${t.paymentMethod} | ${t.category} | Остаток: ${t.balanceAfter} ₽"
            }
            operations.add(text)
        }
        adapter.notifyDataSetChanged()
    }

    fun getOperationAtPosition(position: Int): String {
        return operations[position]
    }

    fun isDateSeparator(position: Int): Boolean {
        val item = operations[position]
        return item.startsWith("———") && item.endsWith("———")
    }
}