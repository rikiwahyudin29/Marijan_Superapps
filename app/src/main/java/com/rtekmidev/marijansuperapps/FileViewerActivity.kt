package com.rtekmidev.marijansuperapps

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rtekmidev.marijansuperapps.util.StatusBarHelper

class FileViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var topProgressBar: ProgressBar
    private lateinit var layLoadingOverlay: LinearLayout
    private lateinit var layErrorOverlay: LinearLayout
    private lateinit var tvViewerTitle: TextView
    private lateinit var tvFileTypeBadge: TextView
    private lateinit var tvViewerSubtitle: TextView

    private lateinit var btnBackViewer: FrameLayout
    private lateinit var btnRefreshViewer: FrameLayout
    private lateinit var btnOpenExternal: FrameLayout
    private lateinit var btnDownloadViewer: FrameLayout
    private lateinit var btnRetry: Button
    private lateinit var btnDirectDownload: Button

    private var fileUrl: String = ""
    private var fileTitle: String = "Berkas"
    private var cleanFileName: String = ""

    private var currentDownloadId: Long = -1L
    private var isReceiverRegistered: Boolean = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L && id == currentDownloadId) {
                    Toast.makeText(
                        this@FileViewerActivity,
                        "✅ Berkas $cleanFileName sudah berhasil diunduh!\nTersimpan di folder Download.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_viewer)

        // Setup Edge-to-Edge Status Bar Transparan & Insets
        StatusBarHelper.setupTranslucentBar(this, findViewById(R.id.mainViewer))

        initViews()
        parseIntentData()
        setupWebView()
        setupListeners()
        setupBackNavigation()
        loadFileContent()
        registerDownloadReceiverSafe()
    }

    private fun initViews() {
        webView = findViewById(R.id.webViewFile)
        topProgressBar = findViewById(R.id.topProgressBar)
        layLoadingOverlay = findViewById(R.id.layLoadingOverlay)
        layErrorOverlay = findViewById(R.id.layErrorOverlay)
        tvViewerTitle = findViewById(R.id.tvViewerTitle)
        tvFileTypeBadge = findViewById(R.id.tvFileTypeBadge)
        tvViewerSubtitle = findViewById(R.id.tvViewerSubtitle)

        btnBackViewer = findViewById(R.id.btnBackViewer)
        btnRefreshViewer = findViewById(R.id.btnRefreshViewer)
        btnOpenExternal = findViewById(R.id.btnOpenExternal)
        btnDownloadViewer = findViewById(R.id.btnDownloadViewer)
        btnRetry = findViewById(R.id.btnRetry)
        btnDirectDownload = findViewById(R.id.btnDirectDownload)
    }

    private fun parseIntentData() {
        fileUrl = intent.getStringExtra("FILE_URL") ?: ""
        fileTitle = intent.getStringExtra("TITLE") ?: "Pratinjau Berkas"

        // Nama file bersih dari url
        val rawName = fileUrl.substringAfterLast("/")
        cleanFileName = if (rawName.contains("_")) rawName.substringAfter("_") else rawName
        if (cleanFileName.isBlank()) cleanFileName = fileTitle

        tvViewerTitle.text = if (fileTitle.isNotBlank()) fileTitle else cleanFileName

        // Badge tipe berkas
        val lower = fileUrl.lowercase()
        when {
            lower.endsWith(".pdf") -> {
                tvFileTypeBadge.text = "PDF"
                tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_red_soft)
                tvFileTypeBadge.setTextColor(Color.parseColor("#E11D48"))
                tvViewerSubtitle.text = "Dokumen PDF Online"
            }
            lower.endsWith(".doc") || lower.endsWith(".docx") -> {
                tvFileTypeBadge.text = "WORD"
                tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_blue_soft)
                tvFileTypeBadge.setTextColor(Color.parseColor("#059669"))
                tvViewerSubtitle.text = "Dokumen Microsoft Word"
            }
            lower.endsWith(".xls") || lower.endsWith(".xlsx") -> {
                tvFileTypeBadge.text = "EXCEL"
                tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_green_soft)
                tvFileTypeBadge.setTextColor(Color.parseColor("#059669"))
                tvViewerSubtitle.text = "Dokumen Microsoft Excel"
            }
            lower.endsWith(".ppt") || lower.endsWith(".pptx") -> {
                tvFileTypeBadge.text = "PPT"
                tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_amber_soft)
                tvFileTypeBadge.setTextColor(Color.parseColor("#D97706"))
                tvViewerSubtitle.text = "Presentasi PowerPoint"
            }
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> {
                tvFileTypeBadge.text = "GAMBAR"
                tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_purple_soft)
                tvFileTypeBadge.setTextColor(Color.parseColor("#7C3AED"))
                tvViewerSubtitle.text = "Gambar Berkas Resolusi Penuh"
            }
            else -> {
                tvFileTypeBadge.text = "BERKAS"
                tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_blue_soft)
                tvFileTypeBadge.setTextColor(Color.parseColor("#064E3B"))
                tvViewerSubtitle.text = "Pratinjau Dokumen Online"
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                topProgressBar.progress = newProgress
                if (newProgress >= 100) {
                    topProgressBar.visibility = View.GONE
                    layLoadingOverlay.visibility = View.GONE
                } else {
                    topProgressBar.visibility = View.VISIBLE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                layErrorOverlay.visibility = View.GONE
                layLoadingOverlay.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                layLoadingOverlay.visibility = View.GONE
                topProgressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    layLoadingOverlay.visibility = View.GONE
                    topProgressBar.visibility = View.GONE
                    layErrorOverlay.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupListeners() {
        btnBackViewer.setOnClickListener { finish() }
        btnRefreshViewer.setOnClickListener { loadFileContent() }
        btnRetry.setOnClickListener { loadFileContent() }

        btnOpenExternal.setOnClickListener { openExternal() }

        btnDownloadViewer.setOnClickListener { downloadFile() }
        btnDirectDownload.setOnClickListener { downloadFile() }
    }

    private fun loadFileContent() {
        if (fileUrl.isBlank()) {
            layLoadingOverlay.visibility = View.GONE
            layErrorOverlay.visibility = View.VISIBLE
            return
        }

        layErrorOverlay.visibility = View.GONE
        layLoadingOverlay.visibility = View.VISIBLE
        topProgressBar.progress = 10
        topProgressBar.visibility = View.VISIBLE

        val lowerUrl = fileUrl.lowercase()
        val isImage = lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
                      lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp") ||
                      lowerUrl.endsWith(".gif")

        val isOfficeDoc = lowerUrl.endsWith(".pdf") ||
                          lowerUrl.endsWith(".doc") || lowerUrl.endsWith(".docx") ||
                          lowerUrl.endsWith(".xls") || lowerUrl.endsWith(".xlsx") ||
                          lowerUrl.endsWith(".ppt") || lowerUrl.endsWith(".pptx")

        if (isImage) {
            // Tampilan elegan untuk foto/gambar
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=4.0, user-scalable=yes">
                    <style>
                        body {
                            margin: 0;
                            padding: 20px 14px;
                            background-color: #0F172A;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            box-sizing: border-box;
                        }
                        img {
                            max-width: 100%;
                            height: auto;
                            border-radius: 12px;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                            transition: transform 0.2s ease;
                        }
                    </style>
                </head>
                <body>
                    <img src="$fileUrl" alt="Berkas" />
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } else if (isOfficeDoc) {
            // Gunakan Google Docs Viewer
            val docsUrl = "https://docs.google.com/gview?embedded=true&url=$fileUrl"
            webView.loadUrl(docsUrl)
        } else {
            webView.loadUrl(fileUrl)
        }
    }

    private fun openExternal() {
        if (fileUrl.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
            startActivity(Intent.createChooser(intent, "Buka Berkas Dengan..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak ada aplikasi yang dapat membuka berkas ini.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadFile() {
        if (fileUrl.isBlank()) return
        try {
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(fileUrl)
            val request = DownloadManager.Request(uri)
                .setTitle(cleanFileName)
                .setDescription("Mengunduh $cleanFileName...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            currentDownloadId = dm.enqueue(request)
            Toast.makeText(this, "Mulai mengunduh: $cleanFileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengunduh berkas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerDownloadReceiverSafe() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(downloadReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(downloadReceiver)
                isReceiverRegistered = false
            } catch (_: Exception) {}
        }
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (_: Exception) {}
    }
}
