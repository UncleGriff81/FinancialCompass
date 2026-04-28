package com.example.familybudget

import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FeedbackActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // ВСТАВЬТЕ ВАШУ ССЫЛКУ НА ФОРМУ
        val formUrl = "https://docs.google.com/forms/d/e/1FAIpQLSe1-agZ9cv2yNG82ReUAGedG7ZC-n0HEZx84NVmtGJrwOighQ/viewform?usp=dialog"

        webView = findViewById(R.id.webview_feedback)

        // Настройки WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        // Очищаем кэш
        webView.clearCache(true)
        webView.clearHistory()

        // Включаем отладку для Android 4.4+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                Toast.makeText(this@FeedbackActivity, "Ошибка: $description", Toast.LENGTH_LONG).show()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Страница загрузилась
            }
        }

        webView.loadUrl(formUrl)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}