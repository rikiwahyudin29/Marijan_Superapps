package com.rtekmidev.marijancbt

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class BerandaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_beranda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", null)
        val userName = sharedPref.getString("nama_lengkap", "Siswa")
        val kelas = sharedPref.getString("kelas", "Memuat Kelas...")

        // Views from Global Header (in Activity)
        val tvNamaDashboard = requireActivity().findViewById<TextView>(R.id.tvNamaDashboard)
        val tvKelas = requireActivity().findViewById<TextView>(R.id.tvKelas)
        val ivProfilPhoto = requireActivity().findViewById<ImageView>(R.id.ivProfilPhoto)

        // Views from Fragment
        val tvStatusKeuangan = view.findViewById<TextView>(R.id.tvStatusKeuangan)
        val tvTugasAktif = view.findViewById<TextView>(R.id.tvTugasAktif)
        val tvPoinDisiplin = view.findViewById<TextView>(R.id.tvPoinDisiplin)
        val pbDisiplin = view.findViewById<ProgressBar>(R.id.pbDisiplin)

        val containerJadwal = view.findViewById<LinearLayout>(R.id.containerJadwal)
        val containerUjian = view.findViewById<LinearLayout>(R.id.containerUjian)
        
        val cardUjianHariIni = view.findViewById<View>(R.id.cardUjianHariIni)
        val cardJadwalPelajaran = view.findViewById<View>(R.id.cardJadwalPelajaran)

        tvNamaDashboard?.text = "Selamat Datang, $userName!"
        if (kelas != null && kelas != "Memuat Kelas..." && kelas.isNotEmpty()) {
            tvKelas?.text = "Kelas $kelas"
        } else {
            tvKelas?.text = "Memuat Kelas..."
        }

        if (!nisn.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val deferredDashboard = async { ApiClient.instance.getDashboard(nisn) }
                    val deferredTugas = async { ApiClient.instance.getTugas(nisn) }
                    
                    val response = deferredDashboard.await()
                    val responseTugas = deferredTugas.await()
                    
                    val activeTugasCount = if (responseTugas.isSuccessful) {
                        responseTugas.body()?.data?.berlangsung?.count { 
                            it.status?.equals("Belum Selesai", ignoreCase = true) == true 
                        } ?: -1
                    } else -1

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.status == true && response.body()?.data != null) {
                            val data = response.body()?.data!!
                            
                            val nameToDisplay = data.nama ?: userName
                            tvNamaDashboard?.text = "Selamat Datang, $nameToDisplay!"
                            if (data.kelas != null) {
                                val fullKelas = if (data.tahun_ajaran != null && data.semester != null) {
                                    "Kelas ${data.kelas} • ${data.tahun_ajaran} • ${data.semester}"
                                } else {
                                    "Kelas ${data.kelas}"
                                }
                                tvKelas?.text = fullKelas
                                sharedPref.edit().putString("kelas", data.kelas)
                                    .putString("kelas_lengkap", fullKelas)
                                    .putString("nama_siswa", nameToDisplay).apply()
                            }
                            
                            // Format Rupiah untuk Tunggakan Sekolah
                            if (data.keuangan != null) {
                                try {
                                    val amount = data.keuangan.toString().toDouble()
                                    val formatRupiah = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("id").setRegion("ID").build())
                                    val formatted = formatRupiah.format(amount).replace("Rp", "Rp ").replace(",00", "")
                                    tvStatusKeuangan?.text = formatted
                                } catch (e: Exception) {
                                    tvStatusKeuangan?.text = "Rp " + data.keuangan.toString()
                                }
                            } else {
                                tvStatusKeuangan?.text = "Rp 0"
                            }
                            
                            // Poin Kedisiplinan & Tugas Aktif
                            val poin = data.poin_disiplin ?: 0
                            tvPoinDisiplin?.text = "$poin/100"
                            pbDisiplin?.progress = poin
                            
                            val tugasAktifToShow = if (activeTugasCount != -1) activeTugasCount else data.tugas_aktif ?: 0
                            tvTugasAktif?.text = tugasAktifToShow.toString()

                            // Load Profile Image
                            if (!data.foto_profil.isNullOrEmpty()) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val baseUrl = "https://app.marijancbt.com/"
                                        val imageUrl = if (data.foto_profil.startsWith("http")) data.foto_profil else baseUrl + data.foto_profil
                                        
                                        // Save to shared preferences
                                        sharedPref.edit().putString("foto_profil", imageUrl).apply()
                                        
                                        withContext(Dispatchers.Main) {
                                            if (isAdded && ivProfilPhoto != null) {
                                                Glide.with(this@BerandaFragment)
                                                    .load(imageUrl)
                                                    .placeholder(android.R.drawable.ic_menu_myplaces)
                                                    .error(android.R.drawable.ic_menu_myplaces)
                                                    .circleCrop()
                                                    .into(ivProfilPhoto)
                                            }
                                        }
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                            
                            // Ujian Hari Ini
                            if (data.ujian_hari_ini != null && data.ujian_hari_ini.isJsonArray) {
                                val ujianArray = data.ujian_hari_ini.asJsonArray
                                if (ujianArray.size() > 0) {
                                    containerUjian?.removeAllViews()
                                    for (i in 0 until ujianArray.size()) {
                                        val ujianObj = ujianArray.get(i).asJsonObject
                                        val namaMapel = ujianObj.get("nama_mapel")?.asString ?: "-"
                                        val waktu = ujianObj.get("waktu")?.asString ?: "-"
                                        val ruang = ujianObj.get("ruang")?.asString ?: "-"
                                        
                                        val viewUjian = layoutInflater.inflate(R.layout.item_ujian_hari_ini, containerUjian, false)
                                        viewUjian.findViewById<TextView>(R.id.tvMapelUjian).text = namaMapel
                                        viewUjian.findViewById<TextView>(R.id.tvWaktuUjian).text = waktu
                                        viewUjian.findViewById<TextView>(R.id.tvRuangUjian).text = ruang
                                        containerUjian?.addView(viewUjian)
                                    }
                                    cardUjianHariIni?.visibility = View.VISIBLE
                                } else {
                                    cardUjianHariIni?.visibility = View.GONE
                                }
                            } else if (data.ujian_hari_ini != null && data.ujian_hari_ini.isJsonObject) {
                                val ujianObj = data.ujian_hari_ini.asJsonObject
                                val namaMapel = ujianObj.get("nama_mapel")?.asString ?: "-"
                                val waktu = ujianObj.get("waktu")?.asString ?: "-"
                                val ruang = ujianObj.get("ruang")?.asString ?: "-"
                                
                                containerUjian?.removeAllViews()
                                val viewUjian = layoutInflater.inflate(R.layout.item_ujian_hari_ini, containerUjian, false)
                                viewUjian.findViewById<TextView>(R.id.tvMapelUjian).text = namaMapel
                                viewUjian.findViewById<TextView>(R.id.tvWaktuUjian).text = waktu
                                viewUjian.findViewById<TextView>(R.id.tvRuangUjian).text = ruang
                                containerUjian?.addView(viewUjian)
                                
                                cardUjianHariIni?.visibility = View.VISIBLE
                            } else {
                                cardUjianHariIni?.visibility = View.GONE
                            }
                            
                            // Jadwal Pelajaran
                            if (data.jadwal_hari_ini != null && data.jadwal_hari_ini.isNotEmpty()) {
                                containerJadwal?.removeAllViews()
                                for (jadwal in data.jadwal_hari_ini) {
                                    val viewJadwal = layoutInflater.inflate(R.layout.item_jadwal_pelajaran, containerJadwal, false)
                                    viewJadwal.findViewById<TextView>(R.id.tvMapelJadwal).text = jadwal.nama_mapel ?: "-"
                                    viewJadwal.findViewById<TextView>(R.id.tvGuruJadwal).text = jadwal.guru ?: "-"
                                    viewJadwal.findViewById<TextView>(R.id.tvJamJadwal).text = jadwal.waktu ?: "-"
                                    containerJadwal?.addView(viewJadwal)
                                }
                                cardJadwalPelajaran?.visibility = View.VISIBLE
                            } else {
                                cardJadwalPelajaran?.visibility = View.GONE
                            }

                        } else {
                            Toast.makeText(requireContext(), "Gagal memuat data dashboard", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error koneksi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
