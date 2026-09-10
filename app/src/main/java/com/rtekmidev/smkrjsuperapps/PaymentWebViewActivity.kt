package com.rtekmidev.smkrjsuperapps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PaymentWebViewActivity : AppCompatActivity() {

    private lateinit var wvPayment: WebView
    private lateinit var pbPaymentProgress: ProgressBar
    private lateinit var layoutPaymentError: LinearLayout
    private lateinit var tvPaymentErrorMsg: TextView
    private lateinit var btnRetryPayment: Button
    private lateinit var btnBackPayment: ImageView
    private lateinit var btnRefreshPayment: ImageView
    private lateinit var btnSelesaiBayar: Button
    private lateinit var tvPaymentSubtitle: TextView

    private var paymentUrl: String = ""
    private var paymentTitle: String = "Pembayaran"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_webview)

        paymentUrl = intent.getStringExtra("EXTRA_PAYMENT_URL") ?: ""
        paymentTitle = intent.getStringExtra("EXTRA_PAYMENT_TITLE") ?: "Pembayaran Tagihan"

        if (paymentUrl.isEmpty()) {
            Toast.makeText(this, "URL Pembayaran tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupWebView()
        loadPaymentUrl()
    }

    private fun initViews() {
        wvPayment = findViewById(R.id.wvPayment)
        pbPaymentProgress = findViewById(R.id.pbPaymentProgress)
        layoutPaymentError = findViewById(R.id.layoutPaymentError)
        tvPaymentErrorMsg = findViewById(R.id.tvPaymentErrorMsg)
        btnRetryPayment = findViewById(R.id.btnRetryPayment)
        btnBackPayment = findViewById(R.id.btnBackPayment)
        btnRefreshPayment = findViewById(R.id.btnRefreshPayment)
        btnSelesaiBayar = findViewById(R.id.btnSelesaiBayar)
        tvPaymentSubtitle = findViewById(R.id.tvPaymentSubtitle)

        tvPaymentSubtitle.text = paymentTitle

        btnBackPayment.setOnClickListener {
            handleBackAction()
        }

        btnRefreshPayment.setOnClickListener {
            wvPayment.reload()
        }

        btnRetryPayment.setOnClickListener {
            layoutPaymentError.visibility = View.GONE
            loadPaymentUrl()
        }

        btnSelesaiBayar.setOnClickListener {
            Toast.makeText(this, "Memperbarui status pembayaran...", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = wvPayment.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        wvPayment.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                pbPaymentProgress.progress = newProgress
                if (newProgress >= 100) {
                    pbPaymentProgress.visibility = View.GONE
                } else {
                    pbPaymentProgress.visibility = View.VISIBLE
                }
            }
        }

        wvPayment.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                layoutPaymentError.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pbPaymentProgress.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    layoutPaymentError.visibility = View.VISIBLE
                    tvPaymentErrorMsg.text = "Gagal memuat halaman: ${error?.description ?: "Koneksi bermasalah"}"
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // Izinkan navigasi eksternal ke m-banking / e-wallet apps
                if (url.startsWith("intent://") || url.startsWith("gojek://") || url.startsWith("shopeepay://") || url.startsWith("dana://") || url.startsWith("bca://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent != null) {
                            view?.context?.startActivity(intent)
                            return true
                        }
                    } catch (_: Exception) {
                        try {
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            view?.context?.startActivity(marketIntent)
                            return true
                        } catch (_: Exception) {}
                    }
                }

                return false
            }
        }
    }

    private fun loadPaymentUrl() {
        layoutPaymentError.visibility = View.GONE
        wvPayment.loadUrl(paymentUrl)
    }

    private fun handleBackAction() {
        if (wvPayment.canGoBack()) {
            wvPayment.goBack()
        } else {
            setResult(RESULT_OK)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackAction()
    }
}
