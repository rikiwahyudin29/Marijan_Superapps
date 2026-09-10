package com.rtekmidev.smkrjsuperapps.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.rtekmidev.smkrjsuperapps.R

object LoadingDialogHelper {

    fun show(context: Context?, message: String = "Memuat data..."): Dialog? {
        if (context == null) return null
        if (context is Activity && (context.isFinishing || context.isDestroyed)) return null

        return try {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading_lottie, null)
            view.findViewById<TextView>(R.id.tvDialogMessage)?.text = message
            dialog.setContentView(view)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)

            dialog.window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setLayout(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                window.setDimAmount(0.5f)
            }

            dialog.show()
            dialog
        } catch (_: Exception) {
            null
        }
    }

    fun dismiss(dialog: Dialog?) {
        try {
            if (dialog != null && dialog.isShowing) {
                dialog.dismiss()
            }
        } catch (_: Exception) {}
    }
}
