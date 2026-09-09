package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import android.widget.ImageView

class KumpulTugasActivity : AppCompatActivity() {

    private lateinit var tvMapel: TextView
    private lateinit var tvDeadline: TextView
    private lateinit var tvJudulTugas: TextView
    private lateinit var tvDeskripsi: TextView
    private lateinit var tvFileSelectedName: TextView
    private lateinit var tvFileSelectedSub: TextView
    private lateinit var btnDownloadPendukung: Button
    private lateinit var btnKirim: Button
    private lateinit var btnBack: LinearLayout
    private lateinit var uploadArea: LinearLayout
    private lateinit var etCatatan: EditText
    private lateinit var loadingLayout: RelativeLayout
    private lateinit var ivPreview: ImageView
    private lateinit var ivUploadIcon: ImageView

    private var selectedFileUri: Uri? = null
    private var idTugas: String = ""

    // File picker khusus untuk PDF, DOCX, JPG, PNG
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                selectedFileUri = uri
                val fileName = getFileName(uri) ?: "upload_file"
                tvFileSelectedName.text = fileName
                tvFileSelectedSub.text = "Siap untuk diunggah"

                // Handle Preview
                ivUploadIcon.visibility = View.GONE
                ivPreview.visibility = View.VISIBLE
                
                val mimeType = contentResolver.getType(uri) ?: ""
                val lowerName = fileName.lowercase()

                if (mimeType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg")) {
                    ivPreview.setImageURI(uri)
                } else if (mimeType == "application/pdf" || lowerName.endsWith(".pdf")) {
                    ivPreview.setImageResource(R.drawable.ic_pdf_document)
                } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
                    ivPreview.setImageResource(R.drawable.ic_word_document)
                } else {
                    ivPreview.setImageResource(R.drawable.ic_upload_cloud) // Fallback generic
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kumpul_tugas)

        // Terapkan bottom inset secara global agar konten tidak tertutup navigasi bawaan HP
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                // Jika view sudah punya padding top (dari header dsb), pertahankan. Hanya tambah bottom.
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }

        
        // Setup Edge-to-Edge Transparent Status Bar with Dark Icons
        com.rtekmidev.smkrjsuperapps.util.StatusBarHelper.setupTranslucentBar(this)

        // Init Views
        tvMapel = findViewById(R.id.tvMapel)
        tvDeadline = findViewById(R.id.tvDeadline)
        tvJudulTugas = findViewById(R.id.tvJudulTugas)
        tvDeskripsi = findViewById(R.id.tvDeskripsi)
        tvFileSelectedName = findViewById(R.id.tvFileSelectedName)
        tvFileSelectedSub = findViewById(R.id.tvFileSelectedSub)
        btnDownloadPendukung = findViewById(R.id.btnDownloadPendukung)
        btnKirim = findViewById(R.id.btnKirim)
        btnBack = findViewById(R.id.btnBack)
        uploadArea = findViewById(R.id.uploadArea)
        etCatatan = findViewById(R.id.etCatatan)
        loadingLayout = findViewById(R.id.loadingLayout)
        ivPreview = findViewById<ImageView>(R.id.ivPreview)
        ivUploadIcon = findViewById<ImageView>(R.id.ivUploadIcon)

        // Get Data From Intent
        idTugas = intent.getStringExtra("ID_TUGAS") ?: ""
        tvMapel.text = intent.getStringExtra("MAPEL") ?: "Mapel"
        tvJudulTugas.text = intent.getStringExtra("JUDUL") ?: "Judul Tugas"
        tvDeadline.text = intent.getStringExtra("DEADLINE") ?: "-"
        
        val deskripsi = intent.getStringExtra("DESKRIPSI") ?: ""
        if (deskripsi.isNotEmpty() && deskripsi != "null") {
            tvDeskripsi.text = deskripsi
        } else {
            tvDeskripsi.text = "Tidak ada deskripsi tambahan."
        }

        val filePendukung = intent.getStringExtra("FILE_PENDUKUNG") ?: ""
        if (filePendukung.isNotEmpty() && filePendukung != "null") {
            btnDownloadPendukung.visibility = View.VISIBLE
            btnDownloadPendukung.setOnClickListener {
                val viewerIntent = Intent(this, FileViewerActivity::class.java)
                viewerIntent.putExtra("FILE_URL", filePendukung)
                viewerIntent.putExtra("TITLE", "File Pendukung")
                startActivity(viewerIntent)
            }
        } else {
            btnDownloadPendukung.visibility = View.GONE
        }

        // Listeners
        btnBack.setOnClickListener { finish() }

        uploadArea.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "image/jpeg",
                    "image/png"
                ))
            }
            filePickerLauncher.launch(intent)
        }

        btnKirim.setOnClickListener {
            submitTugas()
        }
    }

    private fun submitTugas() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Silakan pilih file tugas terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", "") ?: ""

        if (nisn.isEmpty() || idTugas.isEmpty()) {
            Toast.makeText(this, "Data tidak valid (NISN atau ID Tugas kosong)", Toast.LENGTH_SHORT).show()
            return
        }

        loadingLayout.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Prepare File
                val file = getFileFromUri(selectedFileUri!!)
                if (file == null) {
                    withContext(Dispatchers.Main) {
                        loadingLayout.visibility = View.GONE
                        Toast.makeText(this@KumpulTugasActivity, "Gagal memproses file", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Prepare RequestBody parts
                val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
                val multipartFile = MultipartBody.Part.createFormData("file_jawaban", file.name, requestFile)
                
                val reqNisn = RequestBody.create(MediaType.parse("text/plain"), nisn)
                val reqTugasId = RequestBody.create(MediaType.parse("text/plain"), idTugas)
                val reqCatatan = RequestBody.create(MediaType.parse("text/plain"), etCatatan.text.toString())

                val response = ApiClient.instance.submitTugas(
                    nisn = reqNisn,
                    tugasId = reqTugasId,
                    catatan = reqCatatan,
                    file = multipartFile
                )

                withContext(Dispatchers.Main) {
                    loadingLayout.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@KumpulTugasActivity, "Tugas berhasil dikumpulkan!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@KumpulTugasActivity, response.body()?.message ?: "Gagal mengirim tugas", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingLayout.visibility = View.GONE
                    Toast.makeText(this@KumpulTugasActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(uri) ?: "upload_file"
            val tempFile = File(cacheDir, fileName)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }
}
