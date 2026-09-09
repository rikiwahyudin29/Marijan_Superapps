package com.rtekmidev.smkrjsuperapps.util

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object StatusBarHelper {

    /**
     * Mengatur status bar menjadi transparan dengan ikon hitam (dark/light status bar),
     * serta mengaplikasikan insets top & bottom padding pada view root agar konten tidak
     * terpotong status bar (notch) maupun gesture navigation bar.
     */
    fun setupTranslucentBar(activity: Activity, targetView: View? = null) {
        val window = activity.window

        // 1. Status bar transparan
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Icon status bar & navigasi berwarna hitam / gelap (dark icons)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        // 3. Terapkan insets padding pada root view
        val viewToPad = targetView
            ?: activity.findViewById<View>(com.rtekmidev.smkrjsuperapps.R.id.main)
            ?: activity.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
            ?: activity.findViewById(android.R.id.content)

        viewToPad?.let { v ->
            ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                val systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            ViewCompat.requestApplyInsets(v)
        }
    }
}
