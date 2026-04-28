package com.example.familybudget

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DreamsActivity : AppCompatActivity() {

    private lateinit var etDream1Name: EditText
    private lateinit var etDream1Amount: EditText
    private lateinit var etDream2Name: EditText
    private lateinit var etDream2Amount: EditText
    private lateinit var etDream3Name: EditText
    private lateinit var etDream3Amount: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dreams)

        // Привязываем элементы
        etDream1Name = findViewById(R.id.et_dream1_name)
        etDream1Amount = findViewById(R.id.et_dream1_amount)
        etDream2Name = findViewById(R.id.et_dream2_name)
        etDream2Amount = findViewById(R.id.et_dream2_amount)
        etDream3Name = findViewById(R.id.et_dream3_name)
        etDream3Amount = findViewById(R.id.et_dream3_amount)
        btnSave = findViewById(R.id.btn_save_dreams)
        btnBack = findViewById(R.id.btn_back_to_main)

        // Загружаем сохранённые мечты
        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)
        loadDreams()

        // Сохраняем мечты
        btnSave.setOnClickListener {
            saveDreams()
            Toast.makeText(this, "Мечты сохранены!", Toast.LENGTH_SHORT).show()
        }

        // Возврат на главный экран
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun saveDreams() {
        val editor = prefs.edit()
        editor.putString("dream1_name", etDream1Name.text.toString())
        editor.putString("dream1_amount", etDream1Amount.text.toString())
        editor.putString("dream2_name", etDream2Name.text.toString())
        editor.putString("dream2_amount", etDream2Amount.text.toString())
        editor.putString("dream3_name", etDream3Name.text.toString())
        editor.putString("dream3_amount", etDream3Amount.text.toString())
        editor.apply()
    }

    private fun loadDreams() {
        etDream1Name.setText(prefs.getString("dream1_name", ""))
        etDream1Amount.setText(prefs.getString("dream1_amount", ""))
        etDream2Name.setText(prefs.getString("dream2_name", ""))
        etDream2Amount.setText(prefs.getString("dream2_amount", ""))
        etDream3Name.setText(prefs.getString("dream3_name", ""))
        etDream3Amount.setText(prefs.getString("dream3_amount", ""))
    }
}