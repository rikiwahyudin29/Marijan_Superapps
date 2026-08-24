package com.rtekmidev.marijancbt

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.DataAbsenItem
import com.rtekmidev.marijancbt.api.SubmitAbsenJurnalRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@android.annotation.SuppressLint("SetTextI18n")
class PresensiKelasActivity : AppCompatActivity() {

    private lateinit var rvSiswa: RecyclerView
    private lateinit var adapter: PresensiSiswaAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvDetailPresensi: TextView
    private lateinit var btnSimpan: Button
    
    private lateinit var rgSetSemua: RadioGroup
    private lateinit var rbSetHadir: RadioButton
    private lateinit var rbSetSakit: RadioButton
    private lateinit var rbSetIzin: RadioButton
    private lateinit var rbSetAlpa: RadioButton

    private var idJurnal: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presensi_kelas)

        idJurnal = intent.getIntExtra("id_jurnal", 0)
        val namaKelas = intent.getStringExtra("nama_kelas") ?: ""
        val namaMapel = intent.getStringExtra("nama_mapel") ?: ""

        tvDetailPresensi = findViewById(R.id.tvDetailPresensi)
        rvSiswa = findViewById(R.id.rvSiswa)
        progressBar = findViewById(R.id.progressBar)
        btnSimpan = findViewById(R.id.btnSimpan)
        
        rgSetSemua = findViewById(R.id.rgSetSemua)
        rbSetHadir = findViewById(R.id.rbSetHadir)
        rbSetSakit = findViewById(R.id.rbSetSakit)
        rbSetIzin = findViewById(R.id.rbSetIzin)
        rbSetAlpa = findViewById(R.id.rbSetAlpa)

        tvDetailPresensi.text = "$namaMapel - $namaKelas"

        rvSiswa.layoutManager = LinearLayoutManager(this)
        
        // Listeners for "Set Semua"
        rgSetSemua.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSetHadir -> {
                    if (::adapter.isInitialized) adapter.setSemuaStatus("H")
                }
                R.id.rbSetSakit -> {
                    if (::adapter.isInitialized) adapter.setSemuaStatus("S")
                }
                R.id.rbSetIzin -> {
                    if (::adapter.isInitialized) adapter.setSemuaStatus("I")
                }
                R.id.rbSetAlpa -> {
                    if (::adapter.isInitialized) adapter.setSemuaStatus("A")
                }
            }
        }

        btnSimpan.setOnClickListener {
            submitPresensi()
        }

        loadDataSiswa()
    }

    private fun loadDataSiswa() {
        if (idJurnal == 0) {
            Toast.makeText(this, "ID Jurnal tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getSiswaJurnal(idJurnal)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.status == true) {
                        val listSiswa = response.body()?.data ?: emptyList()
                        adapter = PresensiSiswaAdapter(listSiswa)
                        rvSiswa.adapter = adapter
                    } else {
                        Toast.makeText(this@PresensiKelasActivity, "Gagal memuat daftar siswa", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@PresensiKelasActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun submitPresensi() {
        if (!::adapter.isInitialized) return

        val listSiswa = adapter.getData()
        val dataAbsen = mutableListOf<DataAbsenItem>()

        for (siswa in listSiswa) {
            if (siswa.status_absen == null) {
                Toast.makeText(this, "Ada siswa yang belum diisi presensinya (${siswa.nama_lengkap})", Toast.LENGTH_SHORT).show()
                return
            }
            dataAbsen.add(DataAbsenItem(id_siswa = siswa.id, status = siswa.status_absen!!))
        }

        val request = SubmitAbsenJurnalRequest(
            id_jurnal = idJurnal,
            data_absen = dataAbsen
        )

        progressBar.visibility = View.VISIBLE
        btnSimpan.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.submitAbsenJurnal(request)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSimpan.isEnabled = true
                    
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@PresensiKelasActivity, "Presensi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        finish() // Kembali ke dashboard
                    } else {
                        Toast.makeText(this@PresensiKelasActivity, "Gagal: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSimpan.isEnabled = true
                    Toast.makeText(this@PresensiKelasActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
