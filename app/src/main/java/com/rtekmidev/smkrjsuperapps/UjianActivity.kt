@file:OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
package com.rtekmidev.smkrjsuperapps

import android.app.ActivityManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.Soal
import com.rtekmidev.smkrjsuperapps.api.SubmitRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UjianActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var daftarSoal: List<Soal> = emptyList()
    private var currentIndex = 0
    private var idUjianSiswa = ""
    private var durasiMilis: Long = 0
    private var minFinishMilis: Long = 0
    private var countDownTimer: CountDownTimer? = null

    // Status Ujian
    private var isSubmitting = false
    private var isSafeToLeave = false
    private var sisaWaktuMilis: Long = 0
    private var currentTextSize = 16f
    private var pinGracePeriod = 8
    private var lastOverlayWarningTime = 0L

    private lateinit var navAdapter: NavigasiAdapter
    private lateinit var drawerLayout: DrawerLayout

    // Dialogs Keamanan
    private var dialogBluetooth: AlertDialog? = null
    private var dialogHeadset: AlertDialog? = null

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // 1. MONITOR BATERAI (REAL-TIME)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateRealtimeBattery(intent)
        }
    }

    private fun updateRealtimeBattery(intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        }
        val tvBattery = findViewById<TextView>(R.id.tvBatteryInfo)
        tvBattery?.text = "$batteryPct%"
        if (batteryPct <= 15) {
            tvBattery?.setTextColor(Color.parseColor("#EF4444"))
        } else {
            tvBattery?.setTextColor(Color.parseColor("#0F172A"))
        }
    }

    // MONITOR SINYAL / JARINGAN (REAL-TIME)
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun initRealtimeSignalMonitor() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        updateNetworkStatus()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread { updateNetworkStatus() }
                }
                override fun onLost(network: Network) {
                    runOnUiThread {
                        val tvSignal = findViewById<TextView>(R.id.tvSignalStatus)
                        tvSignal?.text = "Terputus"
                        tvSignal?.setTextColor(Color.parseColor("#EF4444"))
                    }
                }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    runOnUiThread { updateNetworkStatus() }
                }
            }
            try {
                connectivityManager?.registerDefaultNetworkCallback(networkCallback!!)
            } catch (e: Exception) {}
        }
    }

    private fun updateNetworkStatus() {
        val cm = connectivityManager ?: return
        val tvSignal = findViewById<TextView>(R.id.tvSignalStatus) ?: return

        val activeNetwork = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(activeNetwork)

        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            tvSignal.text = "Terputus"
            tvSignal.setTextColor(Color.parseColor("#EF4444"))
            return
        }

        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        if (!isValidated) {
            tvSignal.text = "Lambat"
            tvSignal.setTextColor(Color.parseColor("#F59E0B"))
            return
        }

        if (isWifi) {
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val rssi = wifiManager?.connectionInfo?.rssi ?: -60
                @Suppress("DEPRECATION")
                val level = WifiManager.calculateSignalLevel(rssi, 5)
                when {
                    level >= 3 -> {
                        tvSignal.text = "Stabil"
                        tvSignal.setTextColor(Color.parseColor("#0D9488"))
                    }
                    level == 2 -> {
                        tvSignal.text = "Cukup"
                        tvSignal.setTextColor(Color.parseColor("#0284C7"))
                    }
                    else -> {
                        tvSignal.text = "Lemah"
                        tvSignal.setTextColor(Color.parseColor("#F59E0B"))
                    }
                }
            } catch (e: Exception) {
                tvSignal.text = "Stabil"
                tvSignal.setTextColor(Color.parseColor("#0D9488"))
            }
        } else if (isCellular) {
            tvSignal.text = "Stabil"
            tvSignal.setTextColor(Color.parseColor("#0D9488"))
        } else {
            tvSignal.text = "Aktif"
            tvSignal.setTextColor(Color.parseColor("#0D9488"))
        }
    }

    // 2. MONITOR BLUETOOTH (WAJIB MATI)
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_TURNING_ON) {
                    tampilkanDialogBluetoothWajibMati()
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    dialogBluetooth?.dismiss()
                    dialogBluetooth = null
                }
            }
        }
    }

    // 3. MONITOR HEADSET KABEL (WAJIB DILEPAS)
    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) {
                    tampilkanDialogHeadset()
                } else if (state == 0) {
                    dialogHeadset?.dismiss()
                    dialogHeadset = null
                }
            }
        }
    }

    // 4. SCREEN PINNING / LOCK TASK (PENALTI AUTO-SUBMIT JIKA DILEPAS PAKSA)
    private val pinCheckHandler = Handler(Looper.getMainLooper())
    private val checkPinTask = object : Runnable {
        override fun run() {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isPinned = activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

            if (!isPinned && !isSubmitting && !isSafeToLeave) {
                if (pinGracePeriod > 0) {
                    if (pinGracePeriod == 7) {
                        Toast.makeText(this@UjianActivity, "Tunggu sebentar... Mohon Izinkan/Paham pada popup Sematkan Layar!", Toast.LENGTH_LONG).show()
                    }
                    pinGracePeriod--
                    try { startLockTask() } catch (e: Exception) {}
                } else {
                    penaltiKecurangan("Layar tidak disematkan atau sematan dilepas paksa!")
                }
            } else if (isPinned) {
                pinGracePeriod = 0
            }
            pinCheckHandler.postDelayed(this, 1000)
        }
    }

    private fun getRoundedBackground(): GradientDrawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 28f
        shape.setColor(Color.WHITE)
        shape.setStroke(3, Color.parseColor("#E2E8F0"))
        return shape
    }

    // 5. ANTI-OVERLAY / FLOATING WINDOW BLOCKER
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // HANYA cek FLAG_WINDOW_IS_OBSCURED (jangan gunakan PARTIALLY_OBSCURED karena notch kamera,
        // gesture navigation pill, dan dialog sematkan layar terhitung sebagai partially obscured)
        val isObscured = (ev.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0

        if (isObscured) {
            val now = System.currentTimeMillis()
            if (now - lastOverlayWarningTime > 3000) {
                lastOverlayWarningTime = now
                Toast.makeText(
                    this,
                    "⚠️ PERINGATAN KEAMANAN: Layar tertutup aplikasi mengambang / overlay! Sentuhan diblokir.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false // Blokir sentuhan yang tertutup overlay
        }
        return super.dispatchTouchEvent(ev)
    }

    // 6. MODE JANGAN GANGGU (DND)
    private fun enableDndMode() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } catch (e: Exception) {}
    }

    private fun disableDndMode() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {}
    }

    private fun cekBluetoothWajibMati() {
        try {
            @Suppress("DEPRECATION")
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                tampilkanDialogBluetoothWajibMati()
            }
        } catch (e: Exception) {}
    }

    private fun tampilkanDialogBluetoothWajibMati() {
        if (isFinishing || isDestroyed) return
        if (dialogBluetooth?.isShowing == true) return

        val builder = AlertDialog.Builder(this)
            .setTitle("⚠️ BLUETOOTH WAJIB MATI!")
            .setMessage("Demi keamanan ujian dan mencegah kecurangan nirkabel, Bluetooth WAJIB dimatikan.\n\nHarap nonaktifkan Bluetooth untuk melanjutkan ujian.")
            .setCancelable(false)
            .setPositiveButton("Matikan Bluetooth") { _, _ ->
                try {
                    @Suppress("DEPRECATION")
                    BluetoothAdapter.getDefaultAdapter()?.disable()
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (_: Exception) {}
                }
            }

        dialogBluetooth = builder.show()
    }

    private fun cekHeadsetKabel() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            if (audioManager.isWiredHeadsetOn) {
                tampilkanDialogHeadset()
            }
        } catch (e: Exception) {}
    }

    private fun tampilkanDialogHeadset() {
        if (isFinishing || isDestroyed) return
        if (dialogHeadset?.isShowing == true) return

        val builder = AlertDialog.Builder(this)
            .setTitle("🎧 HEADSET TERDETEKSI!")
            .setMessage("Dilarang menggunakan headset atau earphone selama ujian berlangsung.\n\nHarap cabut headset/earphone untuk melanjutkan.")
            .setCancelable(false)

        dialogHeadset = builder.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_ujian)

        // Aktifkan DND otomatis
        enableDndMode()

        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawers()
            } else {
                Toast.makeText(this@UjianActivity, "TIDAK BISA KEMBALI! Selesaikan ujian terlebih dahulu.", Toast.LENGTH_SHORT).show()
            }
        }

        try { startLockTask() } catch (e: Exception) {}
        pinCheckHandler.postDelayed(checkPinTask, 1000)

        // Daftarkan Broadcast Receivers & Monitor Realtime
        val stickyBattery = registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        updateRealtimeBattery(stickyBattery)
        initRealtimeSignalMonitor()

        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(headsetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

        dbHelper = DatabaseHelper(this)
        drawerLayout = findViewById(R.id.drawerLayout)

        // Inisialisasi Header & Watermark dari Sesi Siswa
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val namaSiswa = sharedPref.getString("nama_siswa", null) ?: sharedPref.getString("nama", "SISWA") ?: "SISWA"
        val nisnSiswa = sharedPref.getString("nis", null) ?: sharedPref.getString("nisn", "") ?: ""
        val tvWatermark = findViewById<TextView>(R.id.tvSecurityWatermark)
        tvWatermark.text = "${namaSiswa.uppercase()} • NIS: $nisnSiswa\nSMK RIYADHUL JANNAH CBT"

        val mapelIntent = intent.getStringExtra("MAPEL")
            ?: sharedPref.getString("CURRENT_MAPEL_UJIAN", "Mata Pelajaran Ujian")
        val jenisUjianIntent = intent.getStringExtra("JENIS_UJIAN")
            ?: sharedPref.getString("CURRENT_JENIS_UJIAN", "Ujian Berlangsung")

        findViewById<TextView>(R.id.tvJudulUjianHeader).text = jenisUjianIntent
        findViewById<TextView>(R.id.tvSubJudulUjian).text = mapelIntent

        muatDataOffline()

        // Tombol Navigasi Bawah
        findViewById<View>(R.id.btnNext).setOnClickListener {
            if (currentIndex < daftarSoal.size - 1) { currentIndex++; tampilkanSoal() }
        }
        findViewById<View>(R.id.btnPrev).setOnClickListener {
            if (currentIndex > 0) { currentIndex--; tampilkanSoal() }
        }
        findViewById<View>(R.id.btnSelesai).setOnClickListener { dialogSelesaiUjian() }

        // Buka & Tutup Drawer Navigasi
        findViewById<View>(R.id.btnGridNavigasi).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.btnCloseDrawer).setOnClickListener {
            drawerLayout.closeDrawers()
        }

        // Kontrol Ukuran Font Soal (A- / A+)
        val tvTeksSoal = findViewById<TextView>(R.id.tvTeksSoal)
        findViewById<View>(R.id.btnZoomOut).setOnClickListener {
            if (currentTextSize > 12f) {
                currentTextSize -= 2f
                tvTeksSoal.textSize = currentTextSize
            }
        }
        findViewById<View>(R.id.btnZoomIn).setOnClickListener {
            if (currentTextSize < 30f) {
                currentTextSize += 2f
                tvTeksSoal.textSize = currentTextSize
            }
        }

        // Tap container ragu-ragu
        findViewById<View>(R.id.layoutRaguContainer).setOnClickListener {
            val cb = findViewById<CheckBox>(R.id.cbRaguRagu)
            cb.isChecked = !cb.isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        cekBluetoothWajibMati()
        cekHeadsetKabel()
    }

    private fun penaltiKecurangan(alasan: String) {
        if (isSubmitting || isSafeToLeave) return
        isSubmitting = true
        isSafeToLeave = true

        Toast.makeText(this, "PELANGGARAN: $alasan\nUjian dikumpulkan otomatis!", Toast.LENGTH_LONG).show()

        val payload = SubmitRequest(idUjianSiswa, dbHelper.getAllJawaban())
        dbHelper.clearSemuaData()

        GlobalScope.launch(Dispatchers.IO) {
            try { ApiClient.instance.submitJawaban(payload) } catch (e: Exception) {}
        }
        try { stopLockTask() } catch (e: Exception) {}
        disableDndMode()
        finishAffinity()
    }

    private fun muatDataOffline() {
        val sesi = dbHelper.getDetailSesi()
        idUjianSiswa = sesi["id_ujian"] ?: ""
        val jsonString = sesi["json_soal"] ?: ""
        durasiMilis = (sesi["durasi"] ?: "90").toLong() * 60 * 1000
        minFinishMilis = (sesi["min_finish"] ?: "0").toLong() * 60 * 1000

        // Set ID Sesi di Footer
        findViewById<TextView>(R.id.tvSessionId).text = "ID Sesi: CBT-$idUjianSiswa"

        if (jsonString.isNotEmpty()) {
            val type = object : TypeToken<List<Soal>>() {}.type
            daftarSoal = Gson().fromJson(jsonString, type)

            // RESTORE JAWABAN DARI BRANKAS HP JIKA ADA BACKUP
            val prefs = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            val backupJson = prefs.getString("BACKUP_JAWABAN_$idUjianSiswa", null)
            if (backupJson != null) {
                try {
                    val typeJawaban = object : TypeToken<List<com.rtekmidev.smkrjsuperapps.api.JawabanSiswa>>() {}.type
                    val backupAnswers: List<com.rtekmidev.smkrjsuperapps.api.JawabanSiswa> = Gson().fromJson(backupJson, typeJawaban)
                    backupAnswers.forEach {
                        dbHelper.simpanJawaban(it.soal_id, it.jenis_soal, it.jawaban)
                    }
                } catch (e: Exception) {}
            }

            navAdapter = NavigasiAdapter(this, daftarSoal, dbHelper, currentIndex)
            val gridNavigasi = findViewById<GridView>(R.id.gridNavigasi)
            gridNavigasi.adapter = navAdapter
            gridNavigasi.setOnItemClickListener { _, _, position, _ ->
                currentIndex = position
                tampilkanSoal()
                drawerLayout.closeDrawers()
            }

            tampilkanSoal()
            sinkronisasiWaktuServer()
        } else {
            Toast.makeText(this, "Gagal memuat soal!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sinkronisasiWaktuServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.cekWaktuUjian(idUjianSiswa)

                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        val prefs = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)

                        if (prefs.getBoolean("LOCK_$idUjianSiswa", false)) {
                            Toast.makeText(this@UjianActivity, "Koneksi Pulih! Mengirim otomatis jawaban yang tertunda...", Toast.LENGTH_LONG).show()
                            submitJawabanKeServer()
                        } else {
                            val sisaMilisServer = resp.body()?.sisa_waktu_milis ?: durasiMilis
                            jalankanTimer(sisaMilisServer)
                        }
                    } else {
                        jalankanTimer(durasiMilis)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (getSharedPreferences("SesiUjian", Context.MODE_PRIVATE).getBoolean("LOCK_$idUjianSiswa", false)) {
                        tampilkanDialogTerkunci()
                    } else {
                        jalankanTimer(durasiMilis)
                    }
                }
            }
        }
    }

    private fun tampilkanDialogTerkunci() {
        isSafeToLeave = true
        try { stopLockTask() } catch (e: Exception) {}
        disableDndMode()

        AlertDialog.Builder(this)
            .setTitle("Menunggu Sinyal 📡")
            .setMessage("Jawaban Anda telah diselamatkan secara Offline di HP ini.\n\nSilakan cari jaringan internet yang lebih bagus, lalu masuk kembali ke ujian ini. Sistem akan MENGIRIM JAWABAN ANDA SECARA OTOMATIS tanpa perlu bantuan pengawas.")
            .setPositiveButton("KELUAR") { _, _ ->
                val intent = Intent(this@UjianActivity, DashboardActivity::class.java).apply {
                    putExtra("TARGET_TAB", 2)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }.setCancelable(false).show()
    }

    private fun tampilkanSoal() {
        navAdapter.currentIndex = currentIndex
        navAdapter.notifyDataSetChanged()

        val soal = daftarSoal[currentIndex]
        findViewById<TextView>(R.id.tvNomorSoal).text = "SOAL KE\n- ${currentIndex + 1}"
        findViewById<TextView>(R.id.tvTotalSoalLabel).text = "dari ${daftarSoal.size}\nSoal"

        val tvTeks = findViewById<TextView>(R.id.tvTeksSoal)
        findViewById<ImageView>(R.id.ivSoalAtas).visibility = View.GONE

        // Pasang Engine Render Rumus (Base64 + Image Web)
        val imageGetter = URLImageParser(tvTeks)
        tvTeks.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(soal.teks_soal, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(soal.teks_soal, imageGetter, null)
        }
        tvTeks.textSize = currentTextSize

        val wadah = findViewById<LinearLayout>(R.id.wadahJawaban)
        wadah.removeAllViews()
        val jwb = dbHelper.getJawabanSiswa(soal.id_soal)

        when (soal.jenis_soal) {
            "1" -> renderPGBiasa(wadah, soal, jwb)
            "2" -> renderEsai(wadah, soal, jwb, true)
            "5" -> renderEsai(wadah, soal, jwb, false)
            "3" -> renderPGKompleks(wadah, soal, jwb)
            "6" -> renderBenarSalah(wadah, soal, jwb)
            "4" -> renderMenjodohkan(wadah, soal, jwb)
        }

        val cbRagu = findViewById<CheckBox>(R.id.cbRaguRagu)
        cbRagu.setOnCheckedChangeListener(null)
        cbRagu.isChecked = dbHelper.getRaguStatus(soal.id_soal)
        cbRagu.setOnCheckedChangeListener { _, isChecked ->
            dbHelper.setRaguStatus(soal.id_soal, soal.jenis_soal, isChecked)
            navAdapter.notifyDataSetChanged()
        }

        val btnNext = findViewById<View>(R.id.btnNext)
        val btnSelesai = findViewById<View>(R.id.btnSelesai)
        if (currentIndex == daftarSoal.size - 1) {
            btnNext.visibility = View.GONE
            btnSelesai.visibility = View.VISIBLE
        } else {
            btnNext.visibility = View.VISIBLE
            btnSelesai.visibility = View.GONE
        }
    }

    // ==============================================================
    // REDESAIN OPSI PILIHAN GANDA BIASA (SESUAI MOCKUP PERSIS)
    // Lingkaran Huruf A/B/C/D/E di kiri, Teks di tengah, Indikator di kanan
    // ==============================================================
    private fun renderPGBiasa(wadah: LinearLayout, soal: Soal, jawaban: String) {
        for ((index, opsi) in soal.opsi.withIndex()) {
            val letter = ('A'.code + index).toChar().toString()
            val isSelected = (opsi.id_opsi == jawaban)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(10))
                }
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_opsi_jawaban_selected
                    else R.drawable.bg_opsi_jawaban_normal
                )
                clipToOutline = true
                foreground = androidx.core.content.ContextCompat.getDrawable(this@UjianActivity, R.drawable.ripple_rounded_14dp)
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                isClickable = true
                isFocusable = true
            }

            val tvLetter = TextView(this).apply {
                text = letter
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34))
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_opsi_letter_selected
                    else R.drawable.bg_opsi_letter_normal
                )
                setTextColor(
                    if (isSelected) Color.WHITE
                    else Color.parseColor("#475569")
                )
            }

            val tvTeksOpsi = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(12)
                    marginEnd = dpToPx(10)
                }
                textSize = 14.5f
                setTextColor(Color.parseColor("#0F172A"))
                setLineSpacing(dpToPx(3).toFloat(), 1.0f)
                setTextIsSelectable(false)

                val imageGetter = URLImageParser(this)
                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(opsi.teks_opsi, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(opsi.teks_opsi, imageGetter, null)
                }
            }

            val ivIndicator = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22))
                setImageResource(
                    if (isSelected) R.drawable.ic_option_selected_check
                    else R.drawable.ic_option_unselected_circle
                )
            }

            card.addView(tvLetter)
            card.addView(tvTeksOpsi)
            card.addView(ivIndicator)

            card.setOnClickListener {
                // Reset semua child card di wadah
                for (i in 0 until wadah.childCount) {
                    val childCard = wadah.getChildAt(i) as? LinearLayout ?: continue
                    val cLetter = childCard.getChildAt(0) as? TextView
                    val cIndicator = childCard.getChildAt(2) as? ImageView

                    childCard.setBackgroundResource(R.drawable.bg_opsi_jawaban_normal)
                    cLetter?.setBackgroundResource(R.drawable.bg_opsi_letter_normal)
                    cLetter?.setTextColor(Color.parseColor("#475569"))
                    cIndicator?.setImageResource(R.drawable.ic_option_unselected_circle)
                }

                // Aktifkan card yang dipilih
                card.setBackgroundResource(R.drawable.bg_opsi_jawaban_selected)
                tvLetter.setBackgroundResource(R.drawable.bg_opsi_letter_selected)
                tvLetter.setTextColor(Color.WHITE)
                ivIndicator.setImageResource(R.drawable.ic_option_selected_check)

                dbHelper.simpanJawaban(soal.id_soal, soal.jenis_soal, opsi.id_opsi)
                navAdapter.notifyDataSetChanged()
            }

            wadah.addView(card)
        }
    }

    // ==============================================================
    // REDESAIN OPSI PILIHAN GANDA KOMPLEKS (MULTI-SELECT)
    // ==============================================================
    private fun renderPGKompleks(wadah: LinearLayout, soal: Soal, jawaban: String) {
        val selectedIds = jawaban.split(",").filter { it.isNotBlank() }.toMutableSet()

        for ((index, opsi) in soal.opsi.withIndex()) {
            val letter = ('A'.code + index).toChar().toString()
            val isSelected = selectedIds.contains(opsi.id_opsi)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(10))
                }
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_opsi_jawaban_selected
                    else R.drawable.bg_opsi_jawaban_normal
                )
                clipToOutline = true
                foreground = androidx.core.content.ContextCompat.getDrawable(this@UjianActivity, R.drawable.ripple_rounded_14dp)
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                isClickable = true
                isFocusable = true
            }

            val tvLetter = TextView(this).apply {
                text = letter
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34))
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_opsi_letter_selected
                    else R.drawable.bg_opsi_letter_normal
                )
                setTextColor(
                    if (isSelected) Color.WHITE
                    else Color.parseColor("#475569")
                )
            }

            val tvTeksOpsi = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(12)
                    marginEnd = dpToPx(10)
                }
                textSize = 14.5f
                setTextColor(Color.parseColor("#0F172A"))
                setLineSpacing(dpToPx(3).toFloat(), 1.0f)
                setTextIsSelectable(false)

                val imageGetter = URLImageParser(this)
                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(opsi.teks_opsi, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(opsi.teks_opsi, imageGetter, null)
                }
            }

            val ivIndicator = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22))
                setImageResource(
                    if (isSelected) R.drawable.ic_option_selected_check
                    else R.drawable.ic_option_unselected_circle
                )
            }

            card.addView(tvLetter)
            card.addView(tvTeksOpsi)
            card.addView(ivIndicator)

            card.setOnClickListener {
                if (selectedIds.contains(opsi.id_opsi)) {
                    selectedIds.remove(opsi.id_opsi)
                    card.setBackgroundResource(R.drawable.bg_opsi_jawaban_normal)
                    tvLetter.setBackgroundResource(R.drawable.bg_opsi_letter_normal)
                    tvLetter.setTextColor(Color.parseColor("#475569"))
                    ivIndicator.setImageResource(R.drawable.ic_option_unselected_circle)
                } else {
                    selectedIds.add(opsi.id_opsi)
                    card.setBackgroundResource(R.drawable.bg_opsi_jawaban_selected)
                    tvLetter.setBackgroundResource(R.drawable.bg_opsi_letter_selected)
                    tvLetter.setTextColor(Color.WHITE)
                    ivIndicator.setImageResource(R.drawable.ic_option_selected_check)
                }

                dbHelper.simpanJawaban(soal.id_soal, soal.jenis_soal, selectedIds.joinToString(","))
                navAdapter.notifyDataSetChanged()
            }

            wadah.addView(card)
        }
    }

    // ==============================================================
    // REDESAIN SOAL ESAI / ISIAN
    // ==============================================================
    private fun renderEsai(wadah: LinearLayout, soal: Soal, jawaban: String, isMulti: Boolean) {
        val et = EditText(this).apply {
            hint = "Ketik jawaban di sini..."
            setText(jawaban)
            background = getRoundedBackground()
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            setTextColor(Color.parseColor("#0F172A"))
            setHintTextColor(Color.parseColor("#94A3B8"))
            textSize = 14.5f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dpToPx(16)) }
            if (isMulti) minLines = 5
        }
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                dbHelper.simpanJawaban(soal.id_soal, soal.jenis_soal, s.toString())
                navAdapter.notifyDataSetChanged()
            }
        })
        wadah.addView(et)
    }

    // ==============================================================
    // REDESAIN SOAL BENAR / SALAH
    // ==============================================================
    private fun renderBenarSalah(wadah: LinearLayout, soal: Soal, jawaban: String) {
        val mapJwb = jawaban.split(",").associate {
            val part = it.split("-")
            if (part.size == 2) part[0] to part[1] else "" to ""
        }

        for (opsi in soal.opsi) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dpToPx(12)) }
                background = getRoundedBackground()
                setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
            }

            val tv = TextView(this).apply {
                val imageGetter = URLImageParser(this)
                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(opsi.teks_opsi, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(opsi.teks_opsi, imageGetter, null)
                }
                setPadding(0, 0, 0, dpToPx(10))
                textSize = 14.5f
                setTextColor(Color.parseColor("#0F172A"))
                setTextIsSelectable(false)
            }
            card.addView(tv)

            val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }

            val rbBenar = RadioButton(this).apply {
                text = "Benar"
                buttonTintList = ColorStateList.valueOf(Color.parseColor("#2563EB"))
                setPadding(dpToPx(6), 0, dpToPx(24), 0)
                textSize = 14f
                setTextColor(Color.parseColor("#1E293B"))
            }
            val rbSalah = RadioButton(this).apply {
                text = "Salah"
                buttonTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                setPadding(dpToPx(6), 0, dpToPx(24), 0)
                textSize = 14f
                setTextColor(Color.parseColor("#1E293B"))
            }

            if (mapJwb[opsi.id_opsi] == "1") rbBenar.isChecked = true
            if (mapJwb[opsi.id_opsi] == "0") rbSalah.isChecked = true

            rg.addView(rbBenar)
            rg.addView(rbSalah)

            rg.setOnCheckedChangeListener { _, _ ->
                val listBs = mutableListOf<String>()
                for (i in 0 until wadah.childCount) {
                    val cardChild = wadah.getChildAt(i) as LinearLayout
                    val rgChild = cardChild.getChildAt(cardChild.childCount - 1) as RadioGroup
                    val rbTrue = rgChild.getChildAt(0) as RadioButton
                    val rbFalse = rgChild.getChildAt(1) as RadioButton

                    if (rbTrue.isChecked) listBs.add("${soal.opsi[i].id_opsi}-1")
                    else if (rbFalse.isChecked) listBs.add("${soal.opsi[i].id_opsi}-0")
                }
                dbHelper.simpanJawaban(soal.id_soal, soal.jenis_soal, listBs.joinToString(","))
                navAdapter.notifyDataSetChanged()
            }
            card.addView(rg)
            wadah.addView(card)
        }
    }

    // ==============================================================
    // REDESAIN SOAL MENJODOHKAN
    // ==============================================================
    private fun renderMenjodohkan(wadah: LinearLayout, soal: Soal, jawaban: String) {
        if (soal.couple == null) return
        val mapJwb = jawaban.split(",").associate {
            val part = it.split("-")
            if (part.size == 2) part[0] to part[1] else "" to ""
        }

        val arrayOpsiId = mutableListOf("")
        val arrayOpsiNama = mutableListOf("-- Pilih Pasangan --")
        for (o in soal.opsi) {
            arrayOpsiId.add(o.id_opsi)
            val noImgText = o.teks_opsi.replace("<img[^>]*>".toRegex(), "[Gambar/Rumus]")
            arrayOpsiNama.add(Html.fromHtml(noImgText, Html.FROM_HTML_MODE_COMPACT).toString())
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOpsiNama)

        for (c in soal.couple) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dpToPx(12)) }
                background = getRoundedBackground()
                setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
            }

            val tv = TextView(this).apply {
                val imageGetter = URLImageParser(this)
                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(c.teks_couple, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(c.teks_couple, imageGetter, null)
                }
                setPadding(0, 0, 0, dpToPx(10))
                textSize = 14.5f
                setTextColor(Color.parseColor("#0F172A"))
                setTextIsSelectable(false)
            }
            card.addView(tv)

            val spinner = Spinner(this).apply {
                this.adapter = adapter
                background = getRoundedBackground()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, dpToPx(4), 0, dpToPx(4)) }
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            val savedOpsiId = mapJwb[c.id_couple]
            if (savedOpsiId != null && arrayOpsiId.contains(savedOpsiId)) {
                spinner.setSelection(arrayOpsiId.indexOf(savedOpsiId))
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val listPasangan = mutableListOf<String>()
                    for (i in 0 until wadah.childCount) {
                        val cardChild = wadah.getChildAt(i) as LinearLayout
                        val spinChild = cardChild.getChildAt(cardChild.childCount - 1) as Spinner
                        val pos = spinChild.selectedItemPosition
                        if (pos > 0) listPasangan.add("${soal.couple[i].id_couple}-${arrayOpsiId[pos]}")
                    }
                    dbHelper.simpanJawaban(soal.id_soal, soal.jenis_soal, listPasangan.joinToString(","))
                    navAdapter.notifyDataSetChanged()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            card.addView(spinner)
            wadah.addView(card)
        }
    }

    // ==============================================================
    // TIMER ABSOLUT ANTI-TIDUR
    // ==============================================================
    private fun jalankanTimer(waktuMilis: Long) {
        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val targetFinishTime = System.currentTimeMillis() + waktuMilis

        countDownTimer = object : CountDownTimer(waktuMilis, 1000) {
            override fun onTick(milis: Long) {
                val realSisa = targetFinishTime - System.currentTimeMillis()
                sisaWaktuMilis = realSisa

                if (realSisa <= 0) {
                    onFinish()
                    return
                }
                val jam = (realSisa / (1000 * 60 * 60)) % 24
                val menit = (realSisa / (1000 * 60)) % 60
                val detik = (realSisa / 1000) % 60
                tvTimer.text = String.format("%02d:%02d:%02d", jam, menit, detik)
                val sisaMenitTotal = realSisa / (1000 * 60)
                findViewById<TextView>(R.id.tvSisaMenit)?.text = "$sisaMenitTotal Menit"
            }
            override fun onFinish() {
                sisaWaktuMilis = 0
                tvTimer.text = "00:00:00"
                Toast.makeText(this@UjianActivity, "WAKTU HABIS!", Toast.LENGTH_LONG).show()
                submitJawabanKeServer()
            }
        }.start()
    }

    private fun dialogSelesaiUjian() {
        val waktuBerjalanMilis = durasiMilis - sisaWaktuMilis

        if (waktuBerjalanMilis < minFinishMilis) {
            val sisaNungguMilis = minFinishMilis - waktuBerjalanMilis
            val menitNunggu = (sisaNungguMilis / (1000 * 60)).toInt() + 1
            Toast.makeText(this, "Belum bisa selesai! Tunggu sekitar $menitNunggu menit lagi.", Toast.LENGTH_LONG).show()
            return
        }

        if (dbHelper.isAdaRaguRagu()) {
            Toast.makeText(this, "PERINGATAN: Masih ada soal Ragu-Ragu!", Toast.LENGTH_LONG).show()
            drawerLayout.openDrawer(GravityCompat.END)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Selesai Ujian?")
            .setMessage("Apakah Anda yakin ingin menyelesaikan ujian dan mengirim jawaban sekarang?")
            .setPositiveButton("YA, KIRIM") { _, _ -> submitJawabanKeServer() }
            .setNegativeButton("BATAL", null)
            .setCancelable(false)
            .show()
    }

    private fun submitJawabanKeServer() {
        if (isSubmitting) return
        isSubmitting = true
        isSafeToLeave = true

        val btnSelesai = findViewById<View>(R.id.btnSelesai)
        val tvBtnSelesaiText = findViewById<TextView>(R.id.tvBtnSelesaiText)
        tvBtnSelesaiText.text = "MENGIRIM..."
        btnSelesai.isEnabled = false

        val payload = SubmitRequest(idUjianSiswa, dbHelper.getAllJawaban())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.submitJawaban(payload)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@UjianActivity, "Ujian Selesai!", Toast.LENGTH_LONG).show()
                        dbHelper.clearSemuaData()

                        getSharedPreferences("SesiUjian", Context.MODE_PRIVATE).edit()
                            .remove("LOCK_$idUjianSiswa")
                            .remove("BACKUP_JAWABAN_$idUjianSiswa")
                            .apply()

                        bukaKunciDanKeluar()
                    } else {
                        isSubmitting = false
                        isSafeToLeave = false
                        tvBtnSelesaiText.text = "Selesai Ujian"
                        btnSelesai.isEnabled = true
                        Toast.makeText(this@UjianActivity, "Gagal: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    isSafeToLeave = true

                    val jawabanSiswa = dbHelper.getAllJawaban()
                    val backupJson = Gson().toJson(jawabanSiswa)

                    getSharedPreferences("SesiUjian", Context.MODE_PRIVATE).edit()
                        .putBoolean("LOCK_$idUjianSiswa", true)
                        .putString("BACKUP_JAWABAN_$idUjianSiswa", backupJson)
                        .apply()

                    try { stopLockTask() } catch (e: Exception) {}
                    disableDndMode()

                    AlertDialog.Builder(this@UjianActivity)
                        .setTitle("Jaringan Terputus! 📡")
                        .setMessage("Jawaban Anda telah diselamatkan secara Offline.\n\nLayar telah dibuka. Silakan cari jaringan internet yang stabil, lalu masuk kembali ke aplikasi.\n\nStatus ujian Anda TERKUNCI dan butuh di-RESET LOG / BUKA KUNCI oleh Pengawas.")
                        .setCancelable(false)
                        .setPositiveButton("KELUAR") { _, _ ->
                            val intent = Intent(this@UjianActivity, DashboardActivity::class.java).apply {
                                putExtra("TARGET_TAB", 2)
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(intent)
                            finish()
                        }
                        .show()
                }
            }
        }
    }

    private fun bukaKunciDanKeluar() {
        try { stopLockTask() } catch (e: Exception) {}
        disableDndMode()
        val intent = Intent(this, DashboardActivity::class.java).apply {
            putExtra("TARGET_TAB", 2)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        disableDndMode()
        countDownTimer?.cancel()
        pinCheckHandler.removeCallbacks(checkPinTask)
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(bluetoothReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(headsetReceiver) } catch (e: Exception) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
                connectivityManager?.unregisterNetworkCallback(networkCallback!!)
            }
        } catch (e: Exception) {}
        try { dialogBluetooth?.dismiss() } catch (e: Exception) {}
        try { dialogHeadset?.dismiss() } catch (e: Exception) {}
    }

    // ==============================================================
    // ENGINE RENDER RUMUS & GAMBAR INLINE (BASE64 + URL)
    // ==============================================================
    inner class URLImageParser(private val container: TextView) : android.text.Html.ImageGetter {
        override fun getDrawable(source: String?): android.graphics.drawable.Drawable {
            if (source == null) return android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)

            val levelListDrawable = android.graphics.drawable.LevelListDrawable()

            if (source.startsWith("data:image")) {
                try {
                    val base64String = source.substringAfter(",")
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                    if (bitmap != null) {
                        val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                        val width = (drawable.intrinsicWidth * 2.5).toInt()
                        val height = (drawable.intrinsicHeight * 2.5).toInt()
                        drawable.setBounds(0, 0, width, height)
                        return drawable
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                var fullUrl = source
                if (fullUrl.contains("http")) {
                    fullUrl = fullUrl.substring(fullUrl.indexOf("http"))
                } else {
                    val baseUrl = "https://smkriyadhuljannahjalancagak.sch.id/"
                    fullUrl = if (fullUrl.startsWith("/")) baseUrl + fullUrl.substring(1) else baseUrl + fullUrl
                }

                val emptyDrawable = android.graphics.drawable.ColorDrawable(Color.LTGRAY)
                levelListDrawable.addLevel(0, 0, emptyDrawable)
                levelListDrawable.setBounds(0, 0, 100, 100)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val stream = java.net.URL(fullUrl).openStream()
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                            withContext(Dispatchers.Main) {
                                val width = (drawable.intrinsicWidth * 2.0).toInt()
                                val height = (drawable.intrinsicHeight * 2.0).toInt()
                                drawable.setBounds(0, 0, width, height)
                                levelListDrawable.addLevel(1, 1, drawable)
                                levelListDrawable.setBounds(0, 0, width, height)
                                levelListDrawable.level = 1
                                container.text = container.text
                            }
                        }
                    } catch (e: Exception) {}
                }
                return levelListDrawable
            }
            return levelListDrawable
        }
    }
}
