package com.example.familybudget

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val tvVersion = findViewById<TextView>(R.id.tv_version)
        val tvDeveloper = findViewById<TextView>(R.id.tv_developer)
        val btnPrivacyPolicy = findViewById<Button>(R.id.btn_privacy_policy)
        val btnContact = findViewById<Button>(R.id.btn_contact)

        // Показываем версию приложения
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Версия ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            tvVersion.text = "Версия 1.0"
        }

        tvDeveloper.text = "Разработчик: Uncle_Griff_studio"

        btnPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        btnContact.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
    }
}