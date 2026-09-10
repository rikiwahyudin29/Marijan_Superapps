package com.rtekmidev.smkrjsuperapps

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
import com.rtekmidev.smkrjsuperapps.api.ApiClient
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
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
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
        
        applyEnterTransition()
        
        // Transparent Status Bar (Samakan dengan Dashboard)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_izin)

        // Terapkan system bar insets pada root view (samakan dengan RekapMengajar)
        val rootLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        // Update Header Text Colors based on theme
        val headerTitleColor = if (isNightMode) android.graphics.Color.parseColor("#FFFFFF") else android.graphics.Color.parseColor("#1E293B")
        findViewById<TextView>(R.id.tvHeaderTitle)?.setTextColor(headerTitleColor)

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
            ApiClient.authToken = prefGuru.getString("token", "") ?: ""
        } else if (prefSiswa.getBoolean("isLoggedIn", false)) {
            userRole = "SISWA"
            identifier = prefSiswa.getString("nisn", "") ?: ""
            namaUser = prefSiswa.getString("nama", "Siswa") ?: "Siswa"
            fotoProfilUrl = prefSiswa.getString("foto_profil", "") ?: ""
            ApiClient.authToken = prefSiswa.getString("token", "") ?: ""
        } else {
            Toast.makeText(this, "Sesi tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userLabel = if (userRole == "GURU") "Guru: $namaUser" else "Siswa: $namaUser"
        findViewById<TextView>(R.id.tvKelas)?.text = userLabel
        findViewById<TextView>(R.id.tvNamaDashboard)?.text = "Pengajuan Izin Tidak Masuk"

        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<CardView>(R.id.cvProfilPic)

        val finalUrl = if (fotoProfilUrl.isNotEmpty()) {
            if (!fotoProfilUrl.startsWith("http") && userRole == "GURU") {
                "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/" + fotoProfilUrl
            } else if (!fotoProfilUrl.startsWith("http") && userRole == "SISWA") {
                "https://smkriyadhuljannahjalancagak.sch.id/uploads/siswa/" + fotoProfilUrl
            } else {
                fotoProfilUrl
            }
        } else {
            null
        }

        com.rtekmidev.smkrjsuperapps.util.AvatarHelper.setAvatar(
            context = this,
            name = namaUser,
            fotoUrl = finalUrl,
            ivPhoto = ivProfilPhoto,
            tvInitial = tvProfilInisial,
            cardContainer = cvProfilPic
        )

        viewFinder = findViewById(R.id.viewFinderIzin)

        // Konfigurasi jenis izin khusus (Dinas Luar hanya untuk Guru)
        if (userRole == "GURU" || userRole == "USTADZ") {
            findViewById<View>(R.id.rbDinasLuar)?.visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.rbDinasLuar)?.visibility = View.GONE
        }

        cvProfilPic?.setOnClickListener { finish() }

        // 1. Pilih Tanggal
        val etTgl = findViewById<EditText>(R.id.etTanggalIzin)
        etTgl.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val checkCal = Calendar.getInstance()
                checkCal.set(y, m, d)
                val dow = checkCal.get(Calendar.DAY_OF_WEEK)
                if (dow == Calendar.SUNDAY || dow == Calendar.SATURDAY) {
                    val hari = if (dow == Calendar.SUNDAY) "Hari Minggu" else "Hari Sabtu"
                    Toast.makeText(this, "$hari adalah hari libur sekolah. Tidak dapat mengajukan izin.", Toast.LENGTH_LONG).show()
                    etTgl.setText("")
                    return@DatePickerDialog
                }
                val formattedMonth = String.format(Locale.US, "%02d", m + 1)
                val formattedDay = String.format(Locale.US, "%02d", d)
                etTgl.setText("$y-$formattedMonth-$formattedDay")
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

        // Tombol Balik Kamera (Flip)
        findViewById<ImageView>(R.id.btnFlipKamera).setOnClickListener {
            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            bukaKameraTanam() // Rebind camera
        }

        // 4. Tombol Jepret Foto
        findViewById<CardView>(R.id.btnJepretIzin).setOnClickListener {
            jepretFoto()
        }

        // 5. Tombol Kirim Form
        findViewById<Button>(R.id.btnKirimIzin).setOnClickListener {
            if (!isSubmitting) kirimIzin()
        }

        // Auto-select status jika dilempar dari portal presensi (misal: "Dinas Luar" atau "Izin")
        val defStatus = intent.getStringExtra("default_status")
        if (!defStatus.isNullOrEmpty()) {
            if (defStatus.equals("Dinas Luar", ignoreCase = true)) {
                findViewById<RadioButton>(R.id.rbDinasLuar)?.isChecked = true
            } else if (defStatus.equals("Sakit", ignoreCase = true)) {
                findViewById<RadioButton>(R.id.rbSakit)?.isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbIzin)?.isChecked = true
            }
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

            val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

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

                // Resize bitmap to avoid Payload Too Large (max 800px)
                val maxDim = 800f
                val scale = Math.min(maxDim / bitmap.width, maxDim / bitmap.height)
                val resizedBitmap = if (scale < 1) {
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                } else {
                    bitmap
                }

                findViewById<ImageView>(R.id.ivPreviewIzin).setImageBitmap(resizedBitmap)
                findViewById<TextView>(R.id.tvLabelFile).text = "Foto Surat Terekam!"

                val baos = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
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
            statusText.contains("Cuti") -> "Cuti"
            statusText.contains("Terlambat") -> "Terlambat"
            statusText.contains("Dinas Luar") -> "Dinas Luar"
            else -> "Izin"
        }

        if (tgl.isEmpty() || ket.isEmpty() || base64Image.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data dan ambil foto surat!", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi jika tanggal pengajuan adalah hari libur akhir pekan (Sabtu/Minggu)
        try {
            val sdfCheck = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateParsed = sdfCheck.parse(tgl)
            if (dateParsed != null) {
                val calCheck = Calendar.getInstance()
                calCheck.time = dateParsed
                val dow = calCheck.get(Calendar.DAY_OF_WEEK)
                if (dow == Calendar.SUNDAY || dow == Calendar.SATURDAY) {
                    val hari = if (dow == Calendar.SUNDAY) "Hari Minggu" else "Hari Sabtu"
                    Toast.makeText(this, "Tidak dapat mengajukan izin pada hari libur sekolah ($hari).", Toast.LENGTH_LONG).show()
                    return
                }
            }
        } catch (_: Exception) {}

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
                        var errorMsg = "Gagal mengirim pengajuan"
                        try {
                            val jObjError = org.json.JSONObject(resp.errorBody()?.string() ?: "")
                            errorMsg = jObjError.getString("message")
                        } catch (e: Exception) {
                            errorMsg = "Gagal (Kode: ${resp.code()})"
                        }
                        Toast.makeText(this@IzinActivity, errorMsg, Toast.LENGTH_LONG).show()
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

    override fun finish() {
        super.finish()
        applyExitTransition()
    }
}
