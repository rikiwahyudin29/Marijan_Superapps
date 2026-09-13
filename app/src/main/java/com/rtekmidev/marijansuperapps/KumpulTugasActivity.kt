package com.rtekmidev.marijansuperapps

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import com.rtekmidev.marijansuperapps.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

class KumpulTugasActivity : AppCompatActivity() {

    private lateinit var tvMapel: TextView
    private lateinit var tvDeadline: TextView
    private lateinit var tvJudulTugas: TextView
    private lateinit var tvDeskripsi: TextView
    private lateinit var tvFileSelectedName: TextView
    private lateinit var tvFileSelectedSub: TextView
    private lateinit var btnDownloadPendukung: Button
    private lateinit var btnKirim: Button
    private lateinit var btnBack: ImageView
    private lateinit var uploadArea: LinearLayout
    private lateinit var etCatatan: EditText
    private lateinit var loadingLayout: RelativeLayout
    private lateinit var ivPreview: ImageView
    private lateinit var ivUploadIcon: ImageView

    private var selectedFileUri: Uri? = null
    private var idTugas: String = ""

    private var currentDownloadId: Long = -1L
    private var isReceiverRegistered: Boolean = false
    private var currentDownloadingFileName: String = ""

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L && id == currentDownloadId) {
                    val dm = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor = dm?.query(query)
                    var isSuccess = false
                    var downloadedTitle = currentDownloadingFileName

                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex != -1) {
                            val status = cursor.getInt(statusIndex)
                            isSuccess = (status == DownloadManager.STATUS_SUCCESSFUL)
                        }
                        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        if (titleIndex != -1) {
                            val t = cursor.getString(titleIndex)
                            if (!t.isNullOrBlank()) downloadedTitle = t
                        }
                        cursor.close()
                    }

                    if (isSuccess) {
                        Toast.makeText(
                            this@KumpulTugasActivity,
                            "✅ Berkas $downloadedTitle sudah selesai diunduh!\nTersimpan di folder Download.",
                            Toast.LENGTH_LONG
                        ).show()
                        btnDownloadPendukung.text = "Unduh Ulang File Pendukung"
                        btnDownloadPendukung.isEnabled = true
                    } else {
                        Toast.makeText(
                            this@KumpulTugasActivity,
                            "❌ Unduhan berkas $downloadedTitle gagal atau dibatalkan.",
                            Toast.LENGTH_SHORT
                        ).show()
                        btnDownloadPendukung.text = "Unduh File Pendukung"
                        btnDownloadPendukung.isEnabled = true
                    }
                }
            }
        }
    }

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
        
        // Setup Edge-to-Edge Transparent Status Bar with Dark Icons
        com.rtekmidev.marijansuperapps.util.StatusBarHelper.setupTranslucentBar(this)

        // Setup Header with User Profile Data
        setupHeader()

        // Register DownloadManager Complete Receiver
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        
        val rawDeskripsi = intent.getStringExtra("DESKRIPSI") ?: ""
        val deskripsi = androidx.core.text.HtmlCompat.fromHtml(rawDeskripsi, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
        if (deskripsi.isNotEmpty() && deskripsi != "null") {
            tvDeskripsi.text = deskripsi
        } else {
            tvDeskripsi.text = "Tidak ada deskripsi tambahan."
        }

        val filePendukung = intent.getStringExtra("FILE_PENDUKUNG") ?: ""
        if (filePendukung.isNotEmpty() && filePendukung != "null") {
            btnDownloadPendukung.visibility = View.VISIBLE
            btnDownloadPendukung.setOnClickListener {
                downloadFilePendukung(filePendukung)
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

        val isPerbarui = intent.getBooleanExtra("IS_PERBARUI", false)
        if (isPerbarui) {
            btnKirim.text = "Perbarui Tugas Sekarang"
        }

        btnKirim.setOnClickListener {
            submitTugas()
        }
    }

    private fun setupHeader() {
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
        val fotoProfil = sharedPref.getString("foto_profil", "")

        val isPerbarui = intent.getBooleanExtra("IS_PERBARUI", false)
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = if (isPerbarui) "Perbarui Tugas" else "Kumpulkan Tugas"
        val ivProfil = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<androidx.cardview.widget.CardView>(R.id.cvProfilPic)
        com.rtekmidev.marijansuperapps.util.AvatarHelper.setAvatar(this, namaSiswa, fotoProfil, ivProfil, tvProfilInisial, cvProfilPic)
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
                        val isPerbarui = intent.getBooleanExtra("IS_PERBARUI", false)
                        val defaultMsg = if (isPerbarui) "Tugas berhasil diperbarui!" else "Tugas berhasil dikumpulkan!"
                        val msg = response.body()?.message?.takeIf { it.isNotBlank() } ?: defaultMsg
                        Toast.makeText(this@KumpulTugasActivity, msg, Toast.LENGTH_LONG).show()
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

    private fun downloadFilePendukung(filePendukung: String) {
        try {
            val rawUrl = filePendukung.trim()
            val downloadUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                rawUrl.replace("http://", "https://")
            } else {
                "https://smkriyadhuljannahjalancagak.sch.id/uploads/tugas/$rawUrl"
            }

            var fileName = try {
                Uri.parse(downloadUrl).lastPathSegment ?: ""
            } catch (_: Exception) {
                downloadUrl.substringAfterLast("/")
            }
            if (fileName.contains("?")) {
                fileName = fileName.substringBefore("?")
            }
            try {
                fileName = java.net.URLDecoder.decode(fileName, "UTF-8")
            } catch (_: Exception) {}

            if (fileName.isBlank()) {
                fileName = "File_Pendukung_${System.currentTimeMillis()}"
            }

            val ext = fileName.substringAfterLast(".", "").lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "doc" -> "application/msword"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "xls" -> "application/vnd.ms-excel"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "ppt" -> "application/vnd.ms-powerpoint"
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "txt" -> "text/plain"
                else -> "*/*"
            }

            val safeUri = Uri.parse(downloadUrl.replace(" ", "%20"))
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

            if (manager != null) {
                val request = DownloadManager.Request(safeUri).apply {
                    setTitle(fileName)
                    setDescription("Sedang mengunduh berkas pendukung tugas...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    try {
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    } catch (_: Exception) {}
                    setMimeType(mimeType)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }

                currentDownloadingFileName = fileName
                currentDownloadId = manager.enqueue(request)
                btnDownloadPendukung.text = "Sedang Mengunduh..."
                btnDownloadPendukung.isEnabled = false

                Toast.makeText(
                    this,
                    "📥 Sedang mengunduh: $fileName\nCek bilah status untuk melihat progress unduhan.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val intent = Intent(Intent.ACTION_VIEW, safeUri)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengunduh: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
            btnDownloadPendukung.text = "Unduh File Pendukung"
            btnDownloadPendukung.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(downloadReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }
}
