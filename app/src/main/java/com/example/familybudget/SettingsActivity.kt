package com.example.familybudget

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchLimitNotifications: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        switchLimitNotifications = findViewById(R.id.switch_limit_notifications)
        switchLimitNotifications.isChecked = prefs.getBoolean("notify_limit_exceeded", true)

        switchLimitNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notify_limit_exceeded", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Уведомления включены" else "Уведомления выключены", Toast.LENGTH_SHORT).show()
        }
    }
}