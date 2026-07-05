package com.rtekmidev.marijancbt

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.Soal
import com.rtekmidev.marijancbt.api.SubmitRequest
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

    private lateinit var navAdapter: NavigasiAdapter
    private lateinit var drawerLayout: DrawerLayout

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                val batteryPct = level * 100 / scale.toFloat()
                findViewById<TextView>(R.id.tvBatteryInfo).text = "🔋 ${batteryPct.toInt()}%"
            }
        }
    }

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
        shape.cornerRadius = 32f
        shape.setColor(Color.WHITE)
        shape.setStroke(3, Color.parseColor("#E5E7EB"))
        return shape
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_ujian)

        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawers()
            } else {
                Toast.makeText(this@UjianActivity, "TIDAK BISA KEMBALI! Selesaikan ujian terlebih dahulu.", Toast.LENGTH_SHORT).show()
            }
        }

        try { startLockTask() } catch (e: Exception) {}
        pinCheckHandler.postDelayed(checkPinTask, 1000)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        dbHelper = DatabaseHelper(this)
        drawerLayout = findViewById(R.id.drawerLayout)

        muatDataOffline()

        findViewById<CardView>(R.id.btnNext).setOnClickListener {
            if (currentIndex < daftarSoal.size - 1) { currentIndex++; tampilkanSoal() }
        }
        findViewById<CardView>(R.id.btnPrev).setOnClickListener {
            if (currentIndex > 0) { currentIndex--; tampilkanSoal() }
        }
        findViewById<Button>(R.id.btnSelesai).setOnClickListener { dialogSelesaiUjian() }
        findViewById<CardView>(R.id.btnGridNavigasi).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        val tvTeksSoal = findViewById<TextView>(R.id.tvTeksSoal)
        findViewById<Button>(R.id.btnZoomOut).setOnClickListener {
            if (currentTextSize > 12f) { currentTextSize -= 2f; tvTeksSoal.textSize = currentTextSize }
        }
        findViewById<Button>(R.id.btnZoomIn).setOnClickListener {
            if (currentTextSize < 30f) { currentTextSize += 2f; tvTeksSoal.textSize = currentTextSize }
        }
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
        finishAffinity()
    }

    override fun onPause() { super.onPause() }

    private fun muatDataOffline() {
        val sesi = dbHelper.getDetailSesi()
        idUjianSiswa = sesi["id_ujian"] ?: ""
        val jsonString = sesi["json_soal"] ?: ""
        durasiMilis = (sesi["durasi"] ?: "90").toLong() * 60 * 1000
        minFinishMilis = (sesi["min_finish"] ?: "0").toLong() * 60 * 1000

        if (jsonString.isNotEmpty()) {
            val type = object : TypeToken<List<Soal>>() {}.type
            daftarSoal = Gson().fromJson(jsonString, type)

            // RESTORE JAWABAN DARI BRANKAS HP JIKA ADA BACKUP
            val prefs = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            val backupJson = prefs.getString("BACKUP_JAWABAN_$idUjianSiswa", null)
            if (backupJson != null) {
                try {
                    val typeJawaban = object : TypeToken<List<com.rtekmidev.marijancbt.api.JawabanSiswa>>() {}.type
                    val backupAnswers: List<com.rtekmidev.marijancbt.api.JawabanSiswa> = Gson().fromJson(backupJson, typeJawaban)
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

    // FITUR KUNCI: SINKRONISASI WAKTU & AUTO-RECOVERY JARINGAN
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

        AlertDialog.Builder(this)
            .setTitle("Menunggu Sinyal 📡")
            .setMessage("Jawaban Anda telah diselamatkan secara Offline di HP ini.\n\nSilakan cari jaringan internet yang lebih bagus, lalu masuk kembali ke ujian ini. Sistem akan MENGIRIM JAWABAN ANDA SECARA OTOMATIS tanpa perlu bantuan pengawas.")
            .setPositiveButton("KELUAR") { _, _ ->
                finish()
            }.setCancelable(false).show()
    }

    private fun tampilkanSoal() {
        navAdapter.currentIndex = currentIndex
        navAdapter.notifyDataSetChanged()

        val soal = daftarSoal[currentIndex]
        findViewById<TextView>(R.id.tvNomorSoal).text = "SOAL KE - ${currentIndex + 1}"

        val tvTeks = findViewById<TextView>(R.id.tvTeksSoal)

        // Sembunyikan ivAtas karena engine kita sudah merender gambar INLINE langsung di dalam teks
        findViewById<ImageView>(R.id.ivSoalAtas).visibility = View.GONE

        // Pasang Engine Render Rumus (Base64 + Image Web)
        val imageGetter = URLImageParser(tvTeks)
        tvTeks.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
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

        val btnNext = findViewById<CardView>(R.id.btnNext)
        val btnSelesai = findViewById<Button>(R.id.btnSelesai)
        if (currentIndex == daftarSoal.size - 1) {
            btnNext.visibility = View.GONE
            btnSelesai.visibility = View.VISIBLE
        } else {
            btnNext.visibility = View.VISIBLE
            btnSelesai.visibility = View.GONE
        }
    }

    private fun renderPGBiasa(wadah: LinearLayout, soal: Soal, jawaban: String) {
        val radioButtons = mutableListOf<RadioButton>()
        for (opsi in soal.opsi) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
            card.background = getRoundedBackground()
            card.setPadding(40, 40, 40, 40)

            val rb = RadioButton(this)

            // Render Math Formula & Gambar di Opsi
            val imageGetter = URLImageParser(rb)
            rb.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(opsi.teks_opsi, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(opsi.teks_opsi, imageGetter, null)
            }

            rb.buttonTintList = ColorStateList.valueOf(Color.parseColor("#2563EB"))
            rb.textSize = 15f
            if (opsi.id_opsi == jawaban) rb.isChecked = true
            radioButtons.add(rb)

            val clickListener = View.OnClickListener {
                radioButtons.forEach { it.isChecked = false }
                rb.isChecked = true
                dbHelper.simpanJawaban(soal.id_soal, soal.jenis_soal, opsi.id_opsi)
                navAdapter.notifyDataSetChanged()
            }
            rb.setOnClickListener(clickListener)
            card.setOnClickListener(clickListener)
            card.addView(rb)
            wadah.addView(card)
        }
    }

    private fun renderEsai(wadah: LinearLayout, soal: Soal, jawaban: String, isMulti: Boolean) {
        val et = EditText(this).apply {
            hint = "Ketik jawaban di sini..."
            setText(jawaban)
            background = getRoundedBackground()
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
            if (isMulti) minLines = 4
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

    private fun renderPGKompleks(wadah: LinearLayout, soal: Soal, jawaban: String) {
        val jwbArray = jawaban.split(",")
        val checkBoxes = mutableListOf<Pair<String, CheckBox>>()

        for (opsi in soal.opsi) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
            card.background = getRoundedBackground()
            card.setPadding(40, 40, 40, 40)

            val cb = CheckBox(this)
            val imageGetter = URLImageParser(cb)
            cb.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(opsi.teks_opsi, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(opsi.teks_opsi, imageGetter, null)
            }

            cb.buttonTintList = ColorStateList.valueOf(Color.parseColor("#2563EB"))
            cb.textSize = 15f
            if (jwbArray.contains(opsi.id_opsi)) cb.isChecked = true

            checkBoxes.add(Pair(opsi.id_opsi, cb))

            val updateDb = {
                val listCentang = checkBoxes.filter { it.second.isChecked }.map { it.first }
                dbHelper.simpanJawaban(soal.id_soal, soal.jenis_soal, listCentang.joinToString(","))
                navAdapter.notifyDataSetChanged()
            }
            cb.setOnCheckedChangeListener { _, _ -> updateDb() }
            card.setOnClickListener { cb.isChecked = !cb.isChecked }

            card.addView(cb)
            wadah.addView(card)
        }
    }

    private fun renderBenarSalah(wadah: LinearLayout, soal: Soal, jawaban: String) {
        val mapJwb = jawaban.split(",").associate {
            val part = it.split("-")
            if(part.size == 2) part[0] to part[1] else "" to ""
        }

        for (opsi in soal.opsi) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
            card.background = getRoundedBackground()
            card.setPadding(40, 40, 40, 40)

            val tv = TextView(this)
            val imageGetter = URLImageParser(tv)
            tv.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(opsi.teks_opsi, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(opsi.teks_opsi, imageGetter, null)
            }
            tv.setPadding(0, 0, 0, 16)
            tv.textSize = 15f
            tv.setTextColor(Color.BLACK)
            card.addView(tv)

            val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }

            val rbBenar = RadioButton(this).apply {
                text = "Benar"
                buttonTintList = ColorStateList.valueOf(Color.parseColor("#2563EB"))
                setPadding(16,0,32,0)
            }
            val rbSalah = RadioButton(this).apply {
                text = "Salah"
                buttonTintList = ColorStateList.valueOf(Color.parseColor("#E53935"))
                setPadding(16,0,32,0)
            }

            if (mapJwb[opsi.id_opsi] == "1") rbBenar.isChecked = true
            if (mapJwb[opsi.id_opsi] == "0") rbSalah.isChecked = true

            rg.addView(rbBenar); rg.addView(rbSalah)
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

    private fun renderMenjodohkan(wadah: LinearLayout, soal: Soal, jawaban: String) {
        if (soal.couple == null) return
        val mapJwb = jawaban.split(",").associate {
            val part = it.split("-")
            if(part.size == 2) part[0] to part[1] else "" to ""
        }

        val arrayOpsiId = mutableListOf("")
        val arrayOpsiNama = mutableListOf("-- Pilih Pasangan --")
        for (o in soal.opsi) {
            arrayOpsiId.add(o.id_opsi)
            // Hilangkan tag gambar secara visual untuk spinner saja agar text rapi di dropdown
            val noImgText = o.teks_opsi.replace("<img[^>]*>".toRegex(), "[Gambar/Rumus]")
            arrayOpsiNama.add(Html.fromHtml(noImgText, Html.FROM_HTML_MODE_COMPACT).toString())
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOpsiNama)

        for (c in soal.couple) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
            card.background = getRoundedBackground()
            card.setPadding(40, 40, 40, 40)

            val tv = TextView(this)
            val imageGetter = URLImageParser(tv)
            tv.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(c.teks_couple, Html.FROM_HTML_MODE_COMPACT, imageGetter, null)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(c.teks_couple, imageGetter, null)
            }
            tv.setPadding(0, 0, 0, 16)
            tv.textSize = 15f
            tv.setTextColor(Color.BLACK)
            card.addView(tv)

            val spinner = Spinner(this).apply {
                this.adapter = adapter
                background = getRoundedBackground()
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 32) }
                setPadding(32,32,32,32)
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

    // TIMER ABSOLUT ANTI-TIDUR
    private fun jalankanTimer(waktuMilis: Long) {
        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val targetFinishTime = System.currentTimeMillis() + waktuMilis

        countDownTimer = object : CountDownTimer(waktuMilis, 1000) {
            override fun onTick(milis: Long) {
                // Selalu hitung ulang dari waktu absolut
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
            .setMessage("Apakah Anda yakin ingin menyelesaikan ujian dan mengirim jawaban?")
            .setPositiveButton("YA, KIRIM") { _, _ -> submitJawabanKeServer() }
            .setNegativeButton("BATAL", null)
            .setCancelable(false)
            .show()
    }

    private fun submitJawabanKeServer() {
        if (isSubmitting) return
        isSubmitting = true
        isSafeToLeave = true

        val btnSelesai = findViewById<Button>(R.id.btnSelesai)
        btnSelesai.text = "MENGIRIM..."
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
                        btnSelesai.text = "SELESAI"
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

                    AlertDialog.Builder(this@UjianActivity)
                        .setTitle("Jaringan Terputus! 📡")
                        .setMessage("Jawaban Anda telah diselamatkan secara Offline.\n\nLayar telah dibuka. Silakan cari jaringan internet yang stabil, lalu masuk kembali ke aplikasi.\n\nStatus ujian Anda TERKUNCI dan butuh di-RESET LOG / BUKA KUNCI oleh Pengawas.")
                        .setCancelable(false)
                        .setPositiveButton("KELUAR") { _, _ ->
                            finish()
                        }
                        .show()
                }
            }
        }
    }

    private fun bukaKunciDanKeluar() {
        try { stopLockTask() } catch (e: Exception) {}
        startActivity(Intent(this, JadwalActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        pinCheckHandler.removeCallbacks(checkPinTask)
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
    }

    // ==========================================
    // ENGINE RENDER RUMUS & GAMBAR INLINE (BASE64 + URL)
    // ==========================================
    inner class URLImageParser(private val container: TextView) : android.text.Html.ImageGetter {
        override fun getDrawable(source: String?): android.graphics.drawable.Drawable {
            if (source == null) return android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)

            val levelListDrawable = android.graphics.drawable.LevelListDrawable()

            if (source.startsWith("data:image")) {
                // LOGIKA BACA GAMBAR BASE64 (RUMUS MATEMATIKA CANDY CBT)
                try {
                    val base64String = source.substringAfter(",")
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                    if (bitmap != null) {
                        val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                        // Perbesar 2.5x lipat agar rumus/pecahan lebih terbaca
                        val width = (drawable.intrinsicWidth * 2.5).toInt()
                        val height = (drawable.intrinsicHeight * 2.5).toInt()
                        drawable.setBounds(0, 0, width, height)
                        return drawable // Return langsung agar bounds terdeteksi sinkron
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // LOGIKA BACA GAMBAR URL BIASA
                var fullUrl = source
                if (fullUrl.contains("http")) {
                    // Bersihkan bug dari server (misal: "../https://...") menjadi "https://..."
                    fullUrl = fullUrl.substring(fullUrl.indexOf("http"))
                } else {
                    // URL relatif murni
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
                                container.text = container.text // Force redraw UI
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