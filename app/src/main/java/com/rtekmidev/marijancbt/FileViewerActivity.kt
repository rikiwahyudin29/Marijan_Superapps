package com.rtekmidev.marijancbt

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class FileViewerActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_viewer)

        // Terapkan bottom inset secara global agar konten tidak tertutup navigasi bawaan HP
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                // Jika view sudah punya padding top (dari header dsb), pertahankan. Hanya tambah bottom.
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }


        // Setup Edge-to-Edge Transparent Status Bar
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
        
        val rootView = findViewById<View>(R.id.mainViewer)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackViewer)
        val tvTitle = findViewById<TextView>(R.id.tvViewerTitle)
        val webView = findViewById<WebView>(R.id.webViewFile)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarViewer)

        val fileUrl = intent.getStringExtra("FILE_URL") ?: ""
        val fileTitle = intent.getStringExtra("TITLE") ?: "Dokumen"

        tvTitle.text = fileTitle

        btnBack.setOnClickListener { finish() }

        // Configure WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

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

        // Determine if it needs Google Docs Viewer
        val lowerUrl = fileUrl.lowercase()
        val needsDocsViewer = lowerUrl.endsWith(".pdf") || 
                              lowerUrl.endsWith(".doc") || lowerUrl.endsWith(".docx") || 
                              lowerUrl.endsWith(".xls") || lowerUrl.endsWith(".xlsx") || 
                              lowerUrl.endsWith(".ppt") || lowerUrl.endsWith(".pptx")

        if (needsDocsViewer) {
            val docsUrl = "https://docs.google.com/gview?embedded=true&url=$fileUrl"
            webView.loadUrl(docsUrl)
        } else {
            webView.loadUrl(fileUrl)
        }
    }
}
