package com.rtekmidev.marijansuperapps

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class OtpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fix Status Bar
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_otp)

        // Terapkan bottom inset secara global agar konten tidak tertutup navigasi bawaan HP
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                // Jika view sudah punya padding top (dari header dsb), pertahankan. Hanya tambah bottom.
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }


        // Animated Background
        val vBackgroundGradient = findViewById<View>(R.id.vBackgroundGradient)
        val gradientAnimation = vBackgroundGradient.background as? android.graphics.drawable.AnimationDrawable
        gradientAnimation?.apply {
            setEnterFadeDuration(2000)
            setExitFadeDuration(2000)
            start()
        }

        val etOtpCode = findViewById<EditText>(R.id.etOtpCode)
        val btnVerifyOtp = findViewById<Button>(R.id.btnVerifyOtp)
        val pbLoadingOtp = findViewById<View>(R.id.pbLoadingOtp)

        btnVerifyOtp.setOnClickListener {
            val otpCode = etOtpCode.text.toString().trim()
            if (otpCode.length < 6) {
                Toast.makeText(this, "Masukkan 6 digit kode OTP!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tampilkan loading sebentar buat UX
            btnVerifyOtp.isEnabled = false
            btnVerifyOtp.text = ""
            pbLoadingOtp.visibility = View.VISIBLE

            // Kembalikan OTP ke LoginActivity untuk divalidasi API
            val resultIntent = Intent()
            resultIntent.putExtra("OTP_CODE", otpCode)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
