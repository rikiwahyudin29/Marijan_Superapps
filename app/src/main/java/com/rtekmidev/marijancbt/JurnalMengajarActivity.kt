package com.rtekmidev.marijancbt

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JurnalMengajarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jurnal_mengajar)

        val btnSimpan = findViewById<Button>(R.id.btnSimpanJurnal)
        
        btnSimpan.setOnClickListener {
            val kelas = findViewById<EditText>(R.id.etKelas).text.toString()
            val mapel = findViewById<EditText>(R.id.etMapel).text.toString()
            val materi = findViewById<EditText>(R.id.etMateri).text.toString()

            if (kelas.isEmpty() || mapel.isEmpty() || materi.isEmpty()) {
                Toast.makeText(this, "Semua bidang harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            val identifier = sharedPref.getString("id_user", "") ?: ""

            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.instance.submitJurnalMengajar(identifier, kelas, mapel, materi)
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Simpan Jurnal"
                        
                        if (response.isSuccessful) {
                            Toast.makeText(this@JurnalMengajarActivity, "Jurnal Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@JurnalMengajarActivity, "Gagal menyimpan jurnal", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Simpan Jurnal"
                        Toast.makeText(this@JurnalMengajarActivity, "Error koneksi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
