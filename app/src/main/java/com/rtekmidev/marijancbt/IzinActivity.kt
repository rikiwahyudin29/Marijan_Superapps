package com.rtekmidev.marijancbt

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*

class IzinActivity : AppCompatActivity() {

    private var userRole = ""
    private var identifier = ""

    private var base64Image = ""
    private var isSubmitting = false

    // Variabel CameraX
    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null

    // Launcher Minta Izin Kamera
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            bukaKameraTanam()
        } else {
            Toast.makeText(this, "Izin kamera wajib diberikan untuk foto bukti!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Transparent Status Bar (Samakan dengan Dashboard)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_izin)

        // Padding untuk headerLayout agar tidak menabrak status bar
        val headerLayout = findViewById<LinearLayout>(R.id.headerLayout)
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val topPaddingPx = (20 * resources.displayMetrics.density).toInt()
            view.setPadding(view.paddingLeft, systemBars.top + topPaddingPx, view.paddingRight, view.paddingBottom)
            insets
        }

        // Update Header Text Colors based on theme (Samakan dengan Dashboard)
        val headerTitleColor = if (isNightMode) android.graphics.Color.parseColor("#FFFFFF") else android.graphics.Color.parseColor("#1A1B41")
        val headerSubtextColor = if (isNightMode) android.graphics.Color.parseColor("#D1D5DB") else android.graphics.Color.parseColor("#6B7280")
        findViewById<TextView>(R.id.tvAppTitle).setTextColor(headerTitleColor)
        findViewById<TextView>(R.id.tvNamaDashboard).setTextColor(headerTitleColor)
        findViewById<TextView>(R.id.tvKelas).setTextColor(headerSubtextColor)

        // 🔥 LOGIKA HYBRID BACA SESI (ANTI-NYASAR) 🔥
        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val prefSiswa = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)

        var namaUser = ""
        var fotoProfilUrl = ""

        if (prefGuru.getBoolean("isLoggedIn", false)) {
            userRole = "GURU"
            identifier = prefGuru.getString("id_user", "") ?: ""
            namaUser = prefGuru.getString("nama", "Guru") ?: "Guru"
            fotoProfilUrl = prefGuru.getString("foto_profil", "") ?: ""
        } else if (prefSiswa.getBoolean("isLoggedIn", false)) {
            userRole = "SISWA"
            identifier = prefSiswa.getString("nisn", "") ?: ""
            namaUser = prefSiswa.getString("nama", "Siswa") ?: "Siswa"
            fotoProfilUrl = prefSiswa.getString("foto_profil", "") ?: ""
        } else {
            Toast.makeText(this, "Sesi tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvNamaDashboard).text = "Selamat Datang, $namaUser"
        findViewById<TextView>(R.id.tvKelas).text = "Form Pengajuan Izin / Sakit"

        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        if (ivProfilPhoto != null && fotoProfilUrl.isNotEmpty()) {
            val finalUrl = if (!fotoProfilUrl.startsWith("http") && userRole == "GURU") {
                "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/" + fotoProfilUrl
            } else if (!fotoProfilUrl.startsWith("http") && userRole == "SISWA") {
                "https://smkriyadhuljannahjalancagak.sch.id/uploads/siswa/" + fotoProfilUrl
            } else {
                fotoProfilUrl
            }
            
            com.bumptech.glide.Glide.with(this)
                .load(finalUrl)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .circleCrop()
                .into(ivProfilPhoto)
        }

        viewFinder = findViewById(R.id.viewFinderIzin)

        // Setup Bottom Nav & FAB as back buttons (since this is an overlay activity)
        val closeAction = View.OnClickListener { finish() }
        
        val bottomNavSiswa = findViewById<LinearLayout>(R.id.bottomNavigationSiswa)
        val bottomNavGuru = findViewById<LinearLayout>(R.id.bottomNavigationGuru)
        
        if (userRole == "GURU" || userRole == "USTADZ") {
            bottomNavSiswa.visibility = View.GONE
            bottomNavGuru.visibility = View.VISIBLE
            findViewById<View>(R.id.navBerandaGuru).setOnClickListener(closeAction)
            findViewById<View>(R.id.navPresensiGuru).setOnClickListener(closeAction)
            findViewById<View>(R.id.navAkademikGuru).setOnClickListener(closeAction)
            findViewById<View>(R.id.navProfilGuru).setOnClickListener(closeAction)
            
            // Tampilkan Dinas Luar untuk Guru
            findViewById<View>(R.id.rbDinasLuar).visibility = View.VISIBLE
        } else {
            bottomNavSiswa.visibility = View.VISIBLE
            bottomNavGuru.visibility = View.GONE
            findViewById<View>(R.id.navBeranda).setOnClickListener(closeAction)
            findViewById<View>(R.id.navAkademik).setOnClickListener(closeAction)
            findViewById<View>(R.id.navCbt).setOnClickListener(closeAction)
            findViewById<View>(R.id.navPresensi).setOnClickListener(closeAction)
            findViewById<View>(R.id.navKeuangan).setOnClickListener(closeAction)
            findViewById<View>(R.id.navProfil).setOnClickListener(closeAction)
            
            // Sembunyikan Dinas Luar untuk Siswa
            findViewById<View>(R.id.rbDinasLuar).visibility = View.GONE
        }

        findViewById<View>(R.id.fabScanner).setOnClickListener(closeAction)
        findViewById<View>(R.id.cvProfilPic).setOnClickListener(closeAction)

        // 1. Pilih Tanggal
        val etTgl = findViewById<EditText>(R.id.etTanggalIzin)
        etTgl.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                etTgl.setText("$y-${m+1}-$d")
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 2. Tombol Buka Kamera
        findViewById<CardView>(R.id.btnPilihFile).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                bukaKameraTanam()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // 3. Tombol Tutup Kamera
        findViewById<ImageView>(R.id.btnTutupKamera).setOnClickListener {
            tutupKameraTanam()
        }

        // 4. Tombol Jepret Foto
        findViewById<CardView>(R.id.btnJepretIzin).setOnClickListener {
            jepretFoto()
        }

        // 5. Tombol Kirim Form
        findViewById<Button>(R.id.btnKirimIzin).setOnClickListener {
            if (!isSubmitting) kirimIzin()
        }
    }

    // ==========================================
    // FUNGSI CAMERAX EMBEDDED
    // ==========================================
    private fun bukaKameraTanam() {
        findViewById<View>(R.id.btnPilihFile).visibility = View.GONE
        findViewById<View>(R.id.containerKameraInline).visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Toast.makeText(this, "Gagal memuat kamera.", Toast.LENGTH_SHORT).show()
                tutupKameraTanam()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun tutupKameraTanam() {
        findViewById<View>(R.id.containerKameraInline).visibility = View.GONE
        findViewById<View>(R.id.btnPilihFile).visibility = View.VISIBLE
    }

    private fun jepretFoto() {
        val imageCapture = imageCapture ?: return
        Toast.makeText(this, "Menyimpan foto...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                image.close()

                findViewById<ImageView>(R.id.ivPreviewIzin).setImageBitmap(bitmap)
                findViewById<TextView>(R.id.tvLabelFile).text = "Foto Surat Terekam!"

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                tutupKameraTanam()
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@IzinActivity, "Gagal mengambil foto!", Toast.LENGTH_SHORT).show()
                tutupKameraTanam()
            }
        })
    }

    // ==========================================
    // FUNGSI KIRIM DATA API
    // ==========================================
    private fun kirimIzin() {
        val tgl = findViewById<EditText>(R.id.etTanggalIzin).text.toString()
        val ket = findViewById<EditText>(R.id.etKeterangan).text.toString()
        
        val rgStatus = findViewById<RadioGroup>(R.id.rgStatus)
        val selectedRbId = rgStatus.checkedRadioButtonId
        val statusText = if (selectedRbId != -1) findViewById<RadioButton>(selectedRbId).text.toString() else "Izin"
        
        val status = when {
            statusText.contains("Sakit") -> "Sakit"
            statusText.contains("Izin Keperluan") -> "Izin"
            statusText.contains("Dinas Luar") -> "Dinas Luar"
            else -> "Izin"
        }

        if (tgl.isEmpty() || ket.isEmpty() || base64Image.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data dan ambil foto surat!", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val btnKirim = findViewById<Button>(R.id.btnKirimIzin)
        btnKirim.text = "Mengirim Pengajuan..."
        btnKirim.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🔥 TEMBAK API SESUAI ROLE 🔥
                val resp = if (userRole == "GURU") {
                    ApiClient.instance.ajukanIzinGuru(identifier, tgl, status, ket, base64Image)
                } else {
                    ApiClient.instance.ajukanIzin(identifier, tgl, status, ket, base64Image)
                }

                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    btnKirim.text = "Kirim Pengajuan"
                    btnKirim.isEnabled = true

                    if (resp.isSuccessful) {
                        Toast.makeText(this@IzinActivity, resp.body()?.message ?: "Berhasil", Toast.LENGTH_LONG).show()
                        if (resp.body()?.status == true) finish()
                    } else {
                        Toast.makeText(this@IzinActivity, "Gagal mengirim pengajuan", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    btnKirim.text = "Kirim Pengajuan"
                    btnKirim.isEnabled = true
                    Toast.makeText(this@IzinActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
