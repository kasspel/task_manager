package com.example.taskmanager.screens

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import com.example.taskmanager.R

class WebViewActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        val url = intent.getStringExtra("url")?:""
        if(url.isEmpty()) finish()

        val closeView = findViewById<View>(R.id.close)
        closeView.setOnClickListener {
            finish()
        }

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true //разрешение на работу js скриптов
        webView.settings.domStorageEnabled = true //разрешение на запись в память браузера
        webView.loadUrl(url)
//        webView.loadUrl("file:///android_res/raw/agreemen2.html")
        CookieManager.getInstance().setAcceptCookie(true)

    }
}