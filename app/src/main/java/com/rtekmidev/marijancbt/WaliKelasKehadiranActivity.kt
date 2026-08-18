package com.rtekmidev.marijancbt

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaliKelasKehadiranActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wali_kelas_kehadiran)

        val tvContent = findViewById<TextView>(R.id.tvContentWaliKehadiran)
        
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val identifier = sharedPref.getString("id_user", "") ?: ""
        
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getWaliKelasRekapKehadiran(identifier, currentMonth)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.isJsonObject) {
                            val data = body.asJsonObject.get("data")
                            if (data != null && data.isJsonArray) {
                                val list = data.asJsonArray
                                val sb = StringBuilder()
                                for (i in 0 until list.size()) {
                                    val item = list[i].asJsonObject
                                    sb.append("Nama Siswa: ${item.get("nama_siswa")?.asString}\n")
                                    sb.append("Hadir: ${item.get("hadir")?.asInt} | Sakit: ${item.get("sakit")?.asInt}\n")
                                    sb.append("Izin: ${item.get("izin")?.asInt} | Alpha: ${item.get("alpha")?.asInt}\n\n")
                                }
                                tvContent.text = sb.toString()
                                return@withContext
                            }
                        }
                    }
                    tvContent.text = "Gagal memuat rekap kehadiran wali kelas."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvContent.text = "Error koneksi."
                }
            }
        }
    }
}
