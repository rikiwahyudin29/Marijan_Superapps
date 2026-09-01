package com.rtekmidev.marijancbt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.provider.Settings
import android.os.Build

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    private val otpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val otpCode = result.data?.getStringExtra("OTP_CODE")
            if (!otpCode.isNullOrEmpty()) {
                performLogin(otpCode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fix Status Bar Android 15+ 
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etNISN)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.pbLoadingLogin)

        // Animated Background
        val vBackgroundGradient = findViewById<View>(R.id.vBackgroundGradient)
        val gradientAnimation = vBackgroundGradient.background as? android.graphics.drawable.AnimationDrawable
        gradientAnimation?.apply {
            setEnterFadeDuration(2000)
            setExitFadeDuration(2000)
            start()
        }

        btnLogin.setOnClickListener {
            if (etUsername.text.toString().trim().isEmpty() || etPassword.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Isi Username/NISN dan Password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performLogin(null)
        }
    }

    @android.annotation.SuppressLint("HardwareIds")
    private fun performLogin(otpCode: String?) {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE_ID"
        val deviceName = Build.MODEL ?: "Android Device"

        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.login(username, password, deviceId, deviceName, "0.0", "0.0", otpCode)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    val statusEl = response.body()?.status
                    val statusStr = statusEl?.asString

                    if (response.isSuccessful && statusStr == "requires_2fa") {
                        // Meminta OTP / 2FA dari pengguna
                        Toast.makeText(this@LoginActivity, response.body()?.message ?: "Masukkan kode OTP", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, OtpActivity::class.java)
                        otpLauncher.launch(intent)
                        return@withContext
                    }

                    val isStatusTrue = statusStr == "true" || statusEl?.asBoolean == true
                    if (response.isSuccessful && isStatusTrue) {
                        val user = response.body()?.data
                        if (user != null) {
                            val role = user.role ?: "siswa"
                            val isGuruOrAdmin = role == "guru" || role == "kepsek" || role == "admin"
                            val prefName = if (isGuruOrAdmin) "SesiGuru" else "SesiUjian"
                            val sharedPref = getSharedPreferences(prefName, MODE_PRIVATE)
                            
                            val fotoUrl = if (isGuruOrAdmin) user.detail_guru?.foto ?: "" else ""

                            sharedPref.edit {
                                putString("id_user", user.id_user ?: "")
                                putString("nisn", user.username ?: "")
                                putString("username", user.username ?: "")
                                putString("nama", user.nama_lengkap ?: "")
                                putString("role", role)
                                putString("token", user.token ?: "")
                                putString("foto_profil", fotoUrl)
                                putBoolean("isLoggedIn", true)
                            }

                            Toast.makeText(this@LoginActivity, "Selamat Datang ${user.nama_lengkap}", Toast.LENGTH_SHORT).show()
                            if (role == "guru" || role == "kepsek" || role == "admin") {
                                startActivity(Intent(this@LoginActivity, DashboardGuruActivity::class.java))
                            } else {
                                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                            }
                            finish()
                        }
                    } else {
                        var msg = response.body()?.message
                        if (msg.isNullOrEmpty() && !response.isSuccessful) {
                            try {
                                val errorStr = response.errorBody()?.string()
                                if (!errorStr.isNullOrEmpty()) {
                                    val jsonObject = org.json.JSONObject(errorStr)
                                    msg = jsonObject.optString("message", "")
                                }
                            } catch (_: Exception) {
                                // ignore
                            }
                        }
                        if (msg.isNullOrEmpty()) msg = "Login Gagal! Cek Username & Password"
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