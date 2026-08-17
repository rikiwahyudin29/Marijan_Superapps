package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🌟 Fix Status Bar Android 15+ (Force dark icons because background is white)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etNISN)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progressBar = findViewById<ProgressBar>(R.id.pbLoadingLogin)

        // 🌟 Start Background Gradient Animation
        val vBackgroundGradient = findViewById<View>(R.id.vBackgroundGradient)
        val gradientAnimation = vBackgroundGradient.background as? android.graphics.drawable.AnimationDrawable
        gradientAnimation?.apply {
            setEnterFadeDuration(2000)
            setExitFadeDuration(2000)
            start()
        }

        // 🌟 Password Toggle Logic
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        var isPasswordVisible = false

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // Show Password
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setColorFilter(android.graphics.Color.parseColor("#38BDF8")) // Highlight color
            } else {
                // Hide Password
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setColorFilter(android.graphics.Color.parseColor("#9CA3AF")) // Default color
            }
            // Pindahkan kursor ke akhir teks
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi Username/NISN dan Password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tampilkan loading, matikan tombol biar gak di-spam klik
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            // Get Device Info
            val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
            val deviceName = android.os.Build.MODEL ?: "Android Device"

            // Tembak API pakai Coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.instance.login(username, password, deviceId, deviceName)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true

                        val statusElement = response.body()?.status
                        val isStatusTrue = statusElement != null && statusElement.isJsonPrimitive && (
                            statusElement.asString == "true" || statusElement.asString == "success"
                        )

                        if (response.isSuccessful && isStatusTrue) {

                            // Ambil data langsung dari 'data' sesuai format JSON CI4 bos
                            val user = response.body()?.data

                            if (user != null) {
                                val role = user.role?.lowercase() ?: "siswa"

                                // Simpan sesi login ke SharedPreferences sesuai Role
                                val prefName = if (role == "guru") "SesiGuru" else "SesiUjian"
                                val sharedPref = getSharedPreferences(prefName, Context.MODE_PRIVATE)

                                with(sharedPref.edit()) {
                                    // Kembalikan ke ID Akun (user_id) karena backend akan mencocokkan dengan kolom user_id di tbl_guru
                                    putString("id_user", user.id_user ?: "")
                                    putString("nisn", user.username ?: "") // Tetap simpan sebagai nisn untuk compatibility
                                    putString("username", user.username ?: "")
                                    putString("nama", user.nama_lengkap ?: "")
                                    putString("role", role)
                                    putString("token", user.token ?: "")
                                    putBoolean("isLoggedIn", true)
                                    
                                    // Simpan foto profil guru jika ada
                                    if (role == "guru" && user.detail_guru != null) {
                                        putString("foto_profil", user.detail_guru.foto ?: "")
                                    }
                                    
                                    apply()
                                }

                                ApiClient.authToken = user.token ?: ""

                                Toast.makeText(this@LoginActivity, "Selamat Datang ${user.nama_lengkap}", Toast.LENGTH_SHORT).show()

                                // PINDAH HALAMAN SESUAI ROLE
                                if (role == "guru") {
                                    startActivity(Intent(this@LoginActivity, DashboardGuruActivity::class.java))
                                } else {
                                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                                }
                                finish() // Tutup halaman login
                            }
                        } else {
                            var msg = "Login Gagal! Cek Username & Password"
                            try {
                                val errorBodyStr = response.errorBody()?.string()
                                if (!errorBodyStr.isNullOrEmpty()) {
                                    val errorJson = org.json.JSONObject(errorBodyStr)
                                    msg = errorJson.optString("message", msg)
                                } else {
                                    msg = response.body()?.message ?: msg
                                }
                            } catch (e: Exception) {
                                msg = response.body()?.message ?: msg
                            }
                            Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Error Koneksi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
