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

class WaliKelasKeuanganActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wali_kelas_keuangan)

        val tvContent = findViewById<TextView>(R.id.tvContentWaliKeuangan)
        
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val identifier = sharedPref.getString("id_user", "") ?: ""
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getWaliKelasRekapTagihan(identifier)
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
                                    sb.append("Total Tagihan: Rp. ${item.get("total_tagihan")?.asInt}\n")
                                    sb.append("Status: ${item.get("status")?.asString}\n\n")
                                }
                                tvContent.text = sb.toString()
                                return@withContext
                            }
                        }
                    }
                    tvContent.text = "Gagal memuat rekap tagihan keuangan."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvContent.text = "Error koneksi."
                }
            }
        }
    }
}
