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

class RekapMengajarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rekap_mengajar)

        val tvContent = findViewById<TextView>(R.id.tvContent)
        
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val identifier = sharedPref.getString("id_user", "") ?: ""
        
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getRekapJurnal(identifier, currentMonth)
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
                                    sb.append("Tanggal: ${item.get("tanggal")?.asString}\n")
                                    sb.append("Kelas: ${item.get("kelas")?.asString}\n")
                                    sb.append("Mapel: ${item.get("mapel")?.asString}\n")
                                    sb.append("Materi: ${item.get("materi")?.asString}\n\n")
                                }
                                tvContent.text = sb.toString()
                                return@withContext
                            }
                        }
                    }
                    tvContent.text = "Gagal memuat rekap."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvContent.text = "Error koneksi."
                }
            }
        }
    }
}
