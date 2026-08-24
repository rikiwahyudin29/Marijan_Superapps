package com.rtekmidev.marijancbt

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@android.annotation.SuppressLint("SetTextI18n")
class AkademikGuruFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_akademik_guru, container, false)
        
        view.findViewById<View>(R.id.btnJurnalMengajar)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), JurnalMengajarActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<View>(R.id.btnRekapMengajar)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), RekapMengajarActivity::class.java)
            startActivity(intent)
        }
        
        view.findViewById<View>(R.id.btnWaliKelasKehadiran)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), WaliKelasKehadiranActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<View>(R.id.btnWaliKelasKeuangan)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), WaliKelasKeuanganActivity::class.java)
            startActivity(intent)
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvTotalJamMingguIni = view.findViewById<TextView>(R.id.tvTotalJamMingguIni)
        val tvSiswaBelumAbsen = view.findViewById<TextView>(R.id.tvSiswaBelumAbsen)
        val containerJadwal = view.findViewById<LinearLayout>(R.id.containerJadwalHariIni)
        val pbJadwal = view.findViewById<ProgressBar>(R.id.pbJadwal)
        val tvEmptyJadwal = view.findViewById<TextView>(R.id.tvEmptyJadwal)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", null)

        if (!idUser.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiClient.instance.getAkademikGuruDashboard(idUser)
                    
                    withContext(Dispatchers.Main) {
                        pbJadwal?.visibility = View.GONE
                        
                        if (response.isSuccessful && response.body()?.status == true) {
                            val data = response.body()?.data
                            if (data != null) {
                                tvTotalJamMingguIni?.text = data.total_jam_minggu_ini.toString()
                                tvSiswaBelumAbsen?.text = data.siswa_belum_absen.toString()

                                // Render Jadwal
                                val inflater = LayoutInflater.from(requireContext())
                                if (data.jadwal_hari_ini.isNotEmpty()) {
                                    for (jadwal in data.jadwal_hari_ini) {
                                        val itemJadwal = inflater.inflate(R.layout.item_jadwal_hari_ini, containerJadwal, false)
                                        
                                        itemJadwal.findViewById<TextView>(R.id.tvJamMulai).text = jadwal.jam_mulai?.substring(0, 5) ?: ""
                                        itemJadwal.findViewById<TextView>(R.id.tvJamSelesai).text = jadwal.jam_selesai?.substring(0, 5) ?: ""
                                        itemJadwal.findViewById<TextView>(R.id.tvMataPelajaran).text = jadwal.nama_mapel ?: ""
                                        itemJadwal.findViewById<TextView>(R.id.tvKelas).text = "Kelas ${jadwal.nama_kelas ?: ""}"
                                        
                                        // Play button can open JurnalMengajar directly for that class maybe
                                        itemJadwal.setOnClickListener {
                                            val intent = android.content.Intent(requireContext(), JurnalMengajarActivity::class.java)
                                            intent.putExtra("id_kelas", jadwal.id_kelas ?: "")
                                            intent.putExtra("id_mapel", jadwal.id_mapel ?: "")
                                            intent.putExtra("nama_kelas", jadwal.nama_kelas ?: "")
                                            intent.putExtra("nama_mapel", jadwal.nama_mapel ?: "")
                                            startActivity(intent)
                                        }
                                        
                                        containerJadwal?.addView(itemJadwal)
                                    }
                                } else {
                                    tvEmptyJadwal?.visibility = View.VISIBLE
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Gagal memuat dashboard", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        pbJadwal?.visibility = View.GONE
                        tvEmptyJadwal?.visibility = View.VISIBLE
                        tvEmptyJadwal?.text = "Koneksi bermasalah"
                    }
                }
            }
        } else {
            pbJadwal?.visibility = View.GONE
            tvEmptyJadwal?.visibility = View.VISIBLE
            tvEmptyJadwal?.text = "Silakan login ulang"
        }
    }
}
