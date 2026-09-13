package com.rtekmidev.marijansuperapps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.rtekmidev.marijansuperapps.api.ApiClient
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class KameraAbsenActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null
    private var nisnSiswa = ""
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kamera_absen)

        nisnSiswa = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE).getString("nisn", "") ?: ""
        viewFinder = findViewById(R.id.viewFinder)

        // Mulai Kamera Selfie
        startCamera()

        findViewById<View>(R.id.btnBackCamera).setOnClickListener { finish() }
        findViewById<CardView>(R.id.btnCapture).setOnClickListener {
            if (!isSubmitting) takePhotoAndAbsen()
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

            // PENTING: Pilih Lensa Depan (Selfie)
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Toast.makeText(this, "Gagal membuka kamera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission")
    private fun takePhotoAndAbsen() {
        val imageCapture = imageCapture ?: return
        isSubmitting = true
        findViewById<LinearLayout>(R.id.layoutLoading).visibility = View.VISIBLE

        // 1. CEK LOKASI (GPS)
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location == null) {
            Toast.makeText(this, "GPS tidak terbaca! Coba pindah ke area terbuka.", Toast.LENGTH_LONG).show()
            batalkanLoading()
            return
        }

        // 🔥 2. SISTEM ANTI FAKE GPS KETAT 🔥
        val isFake = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

        if (isFake) {
            Toast.makeText(this, "PELANGGARAN: Terdeteksi Fake GPS (Lokasi Palsu)! Absen ditolak.", Toast.LENGTH_LONG).show()
            batalkanLoading()
            return
        }

        val lat = location.latitude.toString()
        val lng = location.longitude.toString()

        // 3. JEPRET FOTO & KONVERSI
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                image.close()

                // Kompres Foto jadi Base64 agar enteng dikirim
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                val fotoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                // 4. KIRIM KE SERVER API (CodeIgniter)
                kirimKeServer(lat, lng, fotoBase64)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@KameraAbsenActivity, "Gagal jepret foto", Toast.LENGTH_SHORT).show()
                batalkanLoading()
            }
        })
    }

    private fun kirimKeServer(lat: String, lng: String, fotoBase64: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.submitAbsen(nisnSiswa, lat, lng, fotoBase64)
                withContext(Dispatchers.Main) {
                    batalkanLoading()
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@KameraAbsenActivity, response.body()?.message, Toast.LENGTH_LONG).show()
                        finish() // Selesai & Tutup Kamera
                    } else {
                        val msg = response.body()?.message ?: "Gagal absen! Di luar radius sekolah."
                        Toast.makeText(this@KameraAbsenActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    batalkanLoading()
                    Toast.makeText(this@KameraAbsenActivity, "Koneksi Error / Timeout.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun batalkanLoading() {
        isSubmitting = false
        findViewById<LinearLayout>(R.id.layoutLoading).visibility = View.GONE
    }
}
