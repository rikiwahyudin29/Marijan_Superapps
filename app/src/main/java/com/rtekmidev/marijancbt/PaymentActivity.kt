package com.rtekmidev.marijancbt

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PaymentActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val url = intent.getStringExtra("PAYMENT_URL") ?: ""

        if (url.isEmpty()) {
            Toast.makeText(this, "URL Pembayaran tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBackPayment).setOnClickListener { finish() }

        val webView = findViewById<WebView>(R.id.webViewPayment)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)

        // Konfigurasi WebView agar mendukung script Tripay
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        // Cegah webview melempar link ke browser luar (Chrome)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = WebChromeClient()

        // Muat URL Tripay ke dalam aplikasi
        webView.loadUrl(url)
    }

    // Biar tombol back bawaan HP ngga langsung nutup aplikasi, tapi ngebck halaman webnya
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webViewPayment)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}