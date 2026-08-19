@file:Suppress("DEPRECATION")
package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        // 🌟 Fix Status Bar Android 15+ (Force dark icons because background is white)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val tvJudul = findViewById<TextView>(R.id.tvJudul)
        val tvSubJudul = findViewById<TextView>(R.id.tvSubJudul)
        val tvFooter = findViewById<TextView>(R.id.tvFooter) // Menambahkan Footer untuk dianimasikan

        // 🌟 View Transisi Background 🌟
        val vBackgroundGradient = findViewById<View>(R.id.vBackgroundGradient)

        // 1. SETUP AWAL: Elegan, ukuran tidak terlalu kecil, tanpa rotasi aneh
        ivLogo.scaleX = 0.8f
        ivLogo.scaleY = 0.8f
        ivLogo.alpha = 0f
        
        tvJudul.translationY = 50f
        tvSubJudul.translationY = 50f
        tvJudul.alpha = 0f
        tvSubJudul.alpha = 0f
        
        tvFooter.alpha = 0f

        // 2. BACKGROUND ANIMATED GRADIENT
        val gradientAnimation = vBackgroundGradient.background as? android.graphics.drawable.AnimationDrawable
        gradientAnimation?.apply {
            setEnterFadeDuration(2000)
            setExitFadeDuration(2000)
            start()
        }
        
        vBackgroundGradient.animate()
            .alpha(1f)
            .setDuration(1500)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .start()

        // 3. LOGO FADE IN & SCALE UP (Premium feel)
        ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setStartDelay(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                // 4. TEKS SLIDE UP & FADE IN SECARA BERGANTIAN
                tvJudul.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()

                tvSubJudul.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(150)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()

        // 5. FOOTER MUNCUL TERAKHIR
        tvFooter.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(1000)
            .start()

        // 6. PINDAH HALAMAN SETELAH TOTAL 3.5 DETIK
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginSession()
        }, 3500)
    }

    private fun checkLoginSession() {
        // 🔥 LOGIKA HYBRID PENGECEKAN SESI 🔥

        // 1. Cek apakah ada sesi Guru
        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val isGuruLoggedIn = prefGuru.getBoolean("isLoggedIn", false)

        // 2. Cek apakah ada sesi Siswa
        val prefSiswa = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val isSiswaLoggedIn = prefSiswa.getBoolean("isLoggedIn", false)

        // 3. Percabangan Halaman
        if (isGuruLoggedIn) {
            startActivity(Intent(this, DashboardGuruActivity::class.java))
        } else if (isSiswaLoggedIn) {
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            // Kalau dua-duanya kosong, lempar ke Login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
