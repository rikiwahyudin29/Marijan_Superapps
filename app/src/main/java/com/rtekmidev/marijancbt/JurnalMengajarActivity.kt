package com.rtekmidev.marijancbt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@android.annotation.SuppressLint("SetTextI18n")
class JurnalMengajarActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var ivPreview: ImageView
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnCapture: ImageButton
    private lateinit var btnRetake: Button
    
    private lateinit var etJamKe: EditText
    private lateinit var etMateri: EditText
    private lateinit var etCatatan: EditText
    private lateinit var btnBatal: Button
    private lateinit var btnLanjut: Button
    private lateinit var progressBar: View
    private lateinit var tvDetailPelajaran: TextView

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var base64Image: String = ""

    private var idKelas: String = ""
    private var idMapel: String = ""
    private var namaKelas: String = ""
    private var namaMapel: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        
        // Transparent Status Bar
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
        
        setContentView(R.layout.activity_jurnal_mengajar)
        
        // Handle insets for root view (bottom navigation bar)
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }
        
        // Handle insets for appBar (status bar padding)
        val appBar = findViewById<View>(R.id.appBar)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val defaultPadding = (16 * resources.displayMetrics.density).toInt()
            view.setPadding(defaultPadding, systemBars.top + defaultPadding, defaultPadding, defaultPadding)
            insets
        }

        // Initialize Intent Data
        idKelas = intent.getStringExtra("id_kelas") ?: ""
        idMapel = intent.getStringExtra("id_mapel") ?: ""
        namaKelas = intent.getStringExtra("nama_kelas") ?: "Kelas"
        namaMapel = intent.getStringExtra("nama_mapel") ?: "Mata Pelajaran"

        viewFinder = findViewById(R.id.viewFinder)
        ivPreview = findViewById(R.id.ivPreview)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnCapture = findViewById(R.id.btnCapture)
        btnRetake = findViewById(R.id.btnRetake)
        
        etJamKe = findViewById(R.id.etJamKe)
        etMateri = findViewById(R.id.etMateri)
        etCatatan = findViewById(R.id.etCatatan)
        btnBatal = findViewById(R.id.btnBatal)
        btnLanjut = findViewById(R.id.btnLanjut)
        tvDetailPelajaran = findViewById(R.id.tvDetailPelajaran)
        progressBar = findViewById(R.id.progressBar)

        val tvTanggal = findViewById<TextView>(R.id.tvTanggal)
        val tvKameraSiap = findViewById<TextView>(R.id.tvKameraSiap)
        val tvCaptureHint = findViewById<TextView>(R.id.tvCaptureHint)
        
        val intentJamKe = intent.getStringExtra("jam_ke")
        if (!intentJamKe.isNullOrEmpty()) {
            etJamKe.setText(intentJamKe)
        }

        tvDetailPelajaran.text = "$namaMapel - $namaKelas"
        
        // Set Date
        val formatter = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("id", "ID"))
        tvTanggal.text = formatter.format(java.util.Date())

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        btnCapture.setOnClickListener { 
            takePhoto() 
            tvKameraSiap.visibility = View.GONE
            tvCaptureHint.visibility = View.GONE
        }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnRetake.setOnClickListener {
            base64Image = ""
            ivPreview.visibility = View.GONE
            viewFinder.visibility = View.VISIBLE
            btnCapture.visibility = View.VISIBLE
            btnSwitchCamera.visibility = View.VISIBLE
            tvKameraSiap.visibility = View.VISIBLE
            tvCaptureHint.visibility = View.VISIBLE
            btnRetake.visibility = View.GONE
        }
        
        btnBatal.setOnClickListener { finish() }
        btnLanjut.setOnClickListener { submitJurnal() }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        loadJurnalInfo()
    }
    
    private fun loadJurnalInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getJurnalInfo(idKelas, idMapel)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            // Update UI
                            val badgeView = findViewById<TextView>(R.id.tvPertemuanBadge)
                            if (badgeView != null) {
                                badgeView.text = "Pertemuan Ke-${data.pertemuan_ke}"
                            }
                            
                            val tvTotalSiswa = findViewById<TextView>(R.id.tvTotalSiswa)
                            if (tvTotalSiswa != null) {
                                tvTotalSiswa.text = "Total: ${data.total_siswa} Siswa"
                                findViewById<View>(R.id.layoutStats)?.visibility = View.VISIBLE
                                
                                findViewById<TextView>(R.id.tvHadir)?.text = "${data.hadir} Hadir"
                                findViewById<TextView>(R.id.tvIzin)?.text = "${data.izin} Izin"
                                findViewById<TextView>(R.id.tvSakit)?.text = "${data.sakit} Sakit"
                                findViewById<TextView>(R.id.tvAlpha)?.text = "${data.alpha} Alfa"
                                
                                // Set Jam Ke if available
                                if (!data.jam_ke.isNullOrEmpty()) {
                                    val etJamKe = findViewById<EditText>(R.id.etJamKe)
                                    if (etJamKe != null && etJamKe.text.toString().isEmpty()) {
                                        etJamKe.setText(data.jam_ke)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (_: Exception) {
                Toast.makeText(this, "Gagal membuka kamera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees)
                    image.close()

                    // Resize to avoid Payload Too Large (413)
                    val maxDim = 1024
                    var finalBitmap = rotatedBitmap
                    if (finalBitmap.width > maxDim || finalBitmap.height > maxDim) {
                        val ratio = finalBitmap.width.toFloat() / finalBitmap.height.toFloat()
                        val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                        val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                        finalBitmap = Bitmap.createScaledBitmap(finalBitmap, newWidth, newHeight, true)
                    }

                    // Convert to Base64
                    val baos = ByteArrayOutputStream()
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    val bytes = baos.toByteArray()
                    base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)

                    // Show preview
                    ivPreview.setImageBitmap(rotatedBitmap)
                    ivPreview.visibility = View.VISIBLE
                    viewFinder.visibility = View.GONE
                    btnCapture.visibility = View.GONE
                    btnSwitchCamera.visibility = View.GONE
                    btnRetake.visibility = View.VISIBLE
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@JurnalMengajarActivity, "Gagal mengambil foto.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        
        // Handle front camera mirroring
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun submitJurnal() {
        val jamKe = etJamKe.text.toString().trim()
        val materi = etMateri.text.toString().trim()
        val catatan = etCatatan.text.toString().trim()

        if (jamKe.isEmpty() || materi.isEmpty()) {
            Toast.makeText(this, "Jam Ke dan Materi wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (base64Image.isEmpty()) {
            Toast.makeText(this, "Foto kegiatan wajib diambil!", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnLanjut.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.submitJurnal(
                    idKelas = idKelas,
                    idMapel = idMapel,
                    jamKe = jamKe,
                    materi = materi,
                    keterangan = catatan,
                    fotoKegiatan = base64Image
                )

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLanjut.isEnabled = true

                    if (response.isSuccessful && response.body()?.status == true) {
                        val idJurnal = response.body()?.data?.id_jurnal ?: 0
                        Toast.makeText(this@JurnalMengajarActivity, "Jurnal disimpan!", Toast.LENGTH_SHORT).show()
                        
                        // Lanjut ke presensi
                        val intent = Intent(this@JurnalMengajarActivity, PresensiKelasActivity::class.java)
                        intent.putExtra("id_jurnal", idJurnal)
                        intent.putExtra("nama_kelas", namaKelas)
                        intent.putExtra("nama_mapel", namaMapel)
                        intent.putExtra("jam_ke", jamKe)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Unknown Error"
                        Toast.makeText(this@JurnalMengajarActivity, "Gagal (${response.code()}): $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLanjut.isEnabled = true
                    Toast.makeText(this@JurnalMengajarActivity, "Error koneksi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
