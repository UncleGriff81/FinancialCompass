package com.example.familybudget

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.familybudget.database.AppDatabase
import com.example.familybudget.database.ShoppingListEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class ShoppingListsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingListsAdapter
    private lateinit var btnAddList: Button
    private lateinit var btnImport: Button

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            importShoppingList(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_lists)

        db = AppDatabase.getInstance(this)

        recyclerView = findViewById(R.id.rv_lists)
        btnAddList = findViewById(R.id.btn_add_list)
        btnImport = findViewById(R.id.btn_import)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ShoppingListsAdapter(emptyList(), { list ->
            val intent = Intent(this, ShoppingItemsActivity::class.java)
            intent.putExtra("list_id", list.id)
            intent.putExtra("list_name", list.name)
            startActivity(intent)
        }, { list ->
            showDeleteDialog(list)
        })
        recyclerView.adapter = adapter

        loadLists()

        btnAddList.setOnClickListener {
            showAddListDialog()
        }

        btnImport.setOnClickListener {
            importFileLauncher.launch("text/plain")
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadLists()
    }

    private fun loadLists() {
        Thread {
            val lists = db.shoppingListDao().getAllLists()
            runOnUiThread {
                adapter.updateData(lists)
            }
        }.start()
    }

    private fun showAddListDialog() {
        val input = EditText(this)
        input.hint = "Название списка"

        AlertDialog.Builder(this)
            .setTitle("Новый список покупок")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newList = ShoppingListEntity(
                        name = name,
                        date = Date().time,
                        isCompleted = false
                    )
                    Thread {
                        db.shoppingListDao().insert(newList)
                        runOnUiThread {
                            loadLists()
                            Toast.makeText(this@ShoppingListsActivity, "Список '$name' создан", Toast.LENGTH_SHORT).show()
                        }
                    }.start()
                } else {
                    Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(list: ShoppingListEntity) {
        AlertDialog.Builder(this)
            .setTitle("Удаление списка")
            .setMessage("Удалить список \"${list.name}\"? Все товары в нём тоже будут удалены.")
            .setPositiveButton("Удалить") { _, _ ->
                Thread {
                    db.shoppingItemDao().deleteAllByListId(list.id)
                    db.shoppingListDao().delete(list)
                    runOnUiThread {
                        loadLists()
                        Toast.makeText(this@ShoppingListsActivity, "Список удалён", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun importShoppingList(uri: Uri) {
        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                inputStream?.close()

                val lines = content.lines()
                var listName = "Импортированный список"
                val items = mutableListOf<Triple<String, Double, Double>>()

                for (i in lines.indices) {
                    val line = lines[i]
                    when {
                        line.startsWith("СПИСОК ПОКУПОК:") -> {
                            listName = line.replace("СПИСОК ПОКУПОК:", "").trim()
                            listName = listName.replace("_shopping_list", "")
                        }
                        line.trim().startsWith("•") -> {
                            val itemName = line.replace("•", "").trim()
                            if (i + 1 < lines.size) {
                                val priceLine = lines[i + 1]
                                val regex = Regex("Количество: (\\d+(?:\\.\\d+)?) × (\\d+(?:\\.\\d+)?) ₽ = .*")
                                val matchResult = regex.find(priceLine)
                                if (matchResult != null) {
                                    val quantity = matchResult.groupValues[1].toDoubleOrNull() ?: 0.0
                                    val price = matchResult.groupValues[2].toDoubleOrNull() ?: 0.0
                                    items.add(Triple(itemName, quantity, price))
                                }
                            }
                        }
                    }
                }

                if (listName.isEmpty()) {
                    listName = "Импортированный список"
                }

                val newList = ShoppingListEntity(
                    name = listName,
                    date = Date().time,
                    isCompleted = false
                )
                val listId: Long = db.shoppingListDao().insert(newList)

                for (item in items) {
                    val itemEntity = com.example.familybudget.database.ShoppingItemEntity(
                        listId = listId,
                        name = item.first,
                        quantity = item.second,
                        unitPrice = item.third,
                        isChecked = false
                    )
                    db.shoppingItemDao().insert(itemEntity)
                }

                runOnUiThread {
                    loadLists()
                    Toast.makeText(this, "Список '$listName' импортирован (${items.size} товаров)", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}