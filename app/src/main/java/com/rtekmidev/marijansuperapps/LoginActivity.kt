package com.rtekmidev.marijansuperapps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rtekmidev.marijansuperapps.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null

    private var lastUsername = ""
    private var lastPassword = ""
    private var lastOtpCode: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLoc = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseLoc = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        if (fineLoc || coarseLoc) {
            fetchLocationAndLogin(lastUsername, lastPassword, lastOtpCode)
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk absensi yang akurat!", Toast.LENGTH_LONG).show()
            // Tetap login meskipun tanpa lokasi, API akan menerima 0.0
            fetchLocationAndLogin(lastUsername, lastPassword, lastOtpCode)
        }
    }

    private val otpLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val otpCode = result.data?.getStringExtra("OTP_CODE")
            if (!otpCode.isNullOrEmpty()) {
                lastOtpCode = otpCode
                doLogin(lastUsername, lastPassword, lastOtpCode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🌟 Fix Status Bar Android 15+ (Force dark icons because background is white)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_login)

        // Terapkan bottom inset secara global agar konten tidak tertutup navigasi bawaan HP
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                // Jika view sudah punya padding top (dari header dsb), pertahankan. Hanya tambah bottom.
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }


        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val etUsername = findViewById<EditText>(R.id.etNISN)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progressBar = findViewById<View>(R.id.pbLoadingLogin)

        // 🌟 Start Background Gradient Animation
        val vBackgroundGradient = findViewById<View>(R.id.vBackgroundGradient)
        val gradientAnimation = vBackgroundGradient.background as? android.graphics.drawable.AnimationDrawable
        gradientAnimation?.apply {
            setEnterFadeDuration(2000)
            setExitFadeDuration(2000)
            start()
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi Username/NISN dan Password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lastUsername = username
            lastPassword = password
            lastOtpCode = null

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            // Check location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                
                requestPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            } else {
                fetchLocationAndLogin(lastUsername, lastPassword, lastOtpCode)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndLogin(username: String, pass: String, otp: String?) {
        try {
            currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 🛡️ Proteksi Anti-Fake GPS / Mock Location
        if (com.rtekmidev.marijansuperapps.util.LocationHelper.isLocationMock(currentLocation)) {
            val progressBar = findViewById<View>(R.id.pbLoadingLogin)
            val btnLogin = findViewById<Button>(R.id.btnLogin)
            progressBar.visibility = View.GONE
            btnLogin.isEnabled = true

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Fake GPS Terdeteksi!")
                .setMessage("Aplikasi mendeteksi penggunaan Fake GPS / Mock Location pada perangkat Anda. Harap nonaktifkan Fake GPS untuk dapat masuk.")
                .setPositiveButton("Mengerti", null)
                .setCancelable(false)
                .show()
            return
        }

        doLogin(username, pass, otp)
    }

    @SuppressLint("HardwareIds")
    private fun doLogin(username: String, pass: String, otp: String?) {
        val progressBar = findViewById<View>(R.id.pbLoadingLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val lat = currentLocation?.latitude?.toString() ?: "0.0"
        val lon = currentLocation?.longitude?.toString() ?: "0.0"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ambil token FCM perangkat (jika belum tersimpan)
                var fcmToken: String? = getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE).getString("FCM_TOKEN", null)
                if (fcmToken.isNullOrEmpty()) {
                    try {
                        val tokenTask = com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                        fcmToken = com.google.android.gms.tasks.Tasks.await(tokenTask, 4, java.util.concurrent.TimeUnit.SECONDS)
                        if (!fcmToken.isNullOrEmpty()) {
                            getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)
                                .edit().putString("FCM_TOKEN", fcmToken).apply()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LoginActivity", "FCM token fetch: ${e.message}")
                    }
                }

                val response = ApiClient.instance.login(username, pass, deviceId, deviceName, lat, lon, otp, fcmToken)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    val statusElement = response.body()?.status
                    
                    // Cek jika 2FA
                    if (statusElement?.isJsonPrimitive == true && statusElement.asString == "requires_2fa") {
                        val intent = Intent(this@LoginActivity, OtpActivity::class.java)
                        otpLauncher.launch(intent)
                        return@withContext
                    }

                    val isStatusTrue = statusElement?.isJsonPrimitive == true && (statusElement.asString == "true" || statusElement.asString == "success")

                    if (response.isSuccessful && isStatusTrue) {
                        val user = response.body()?.data

                        if (user != null) {
                            val role = (user.role ?: "siswa").lowercase()
                            val isStaffOrAdmin = role in listOf("guru", "kepsek", "admin", "superadmin")
                            val token = user.token ?: ""

                            // Simpan sesi login ke SharedPreferences sesuai Role
                            val prefName = if (isStaffOrAdmin) "SesiGuru" else "SesiUjian"
                            val sharedPref = getSharedPreferences(prefName, Context.MODE_PRIVATE)

                            with(sharedPref.edit()) {
                                putString("id_user", user.id_user ?: "")
                                putString("nisn", user.username ?: "") // Tetap simpan sebagai nisn untuk compatibility
                                putString("username", user.username ?: "")
                                putString("nama", user.nama_lengkap ?: "")
                                putString("role", role)
                                putString("token", token)
                                
                                if (isStaffOrAdmin) {
                                    val fotoGuru = user.detail_guru?.foto ?: ""
                                    putString("foto_profil", fotoGuru)
                                }
                                
                                putBoolean("isLoggedIn", true)
                                apply()
                            }

                            // Simpan juga ke USER_PREF untuk akses global background FCM service
                            getSharedPreferences("USER_PREF", Context.MODE_PRIVATE).edit()
                                .putString("TOKEN", token)
                                .putString("ROLE", role)
                                .apply()
                            
                            ApiClient.authToken = token

                            Toast.makeText(this@LoginActivity, "Selamat Datang ${user.nama_lengkap}", Toast.LENGTH_SHORT).show()

                            // PINDAH HALAMAN SESUAI ROLE
                            if (isStaffOrAdmin) {
                                startActivity(Intent(this@LoginActivity, DashboardGuruActivity::class.java))
                            } else {
                                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                            }
                            applyEnterTransition()
                            finish() // Tutup halaman login
                        }
                    } else {
                        var msg = response.body()?.message
                        if (msg.isNullOrEmpty()) {
                            try {
                                val errorBodyStr = response.errorBody()?.string()
                                if (!errorBodyStr.isNullOrEmpty()) {
                                    val errorJson = org.json.JSONObject(errorBodyStr)
                                    if (errorJson.has("message")) {
                                        msg = errorJson.getString("message")
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        if (msg.isNullOrEmpty()) {
                            msg = "Login Gagal! Cek Username & Password"
                        }

                        val isDeviceError = msg.contains("perangkat", ignoreCase = true) ||
                                            msg.contains("terikat", ignoreCase = true) ||
                                            msg.contains("tertaut", ignoreCase = true) ||
                                            msg.contains("reset", ignoreCase = true)

                        if (isDeviceError) {
                            androidx.appcompat.app.AlertDialog.Builder(this@LoginActivity)
                                .setTitle("Perangkat Terikat")
                                .setMessage(msg)
                                .setPositiveButton("Mengerti", null)
                                .show()
                        } else {
                            androidx.appcompat.app.AlertDialog.Builder(this@LoginActivity)
                                .setTitle("Login Gagal")
                                .setMessage(msg)
                                .setPositiveButton("Tutup", null)
                                .show()
                        }
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