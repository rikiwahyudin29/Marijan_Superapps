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
        

        view.findViewById<View>(R.id.btnJadwalMengajar)?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Jadwal Mengajar belum tersedia", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvTotalJamMingguIni = view.findViewById<TextView>(R.id.tvTotalJamMingguIni)
        val tvTotalJamMapelHeader = view.findViewById<TextView>(R.id.tvTotalJamMapelHeader)
        val tvSiswaBelumAbsen = view.findViewById<TextView>(R.id.tvSiswaBelumAbsen)
        val containerJadwal = view.findViewById<LinearLayout>(R.id.containerJadwalHariIni)
        val pbJadwal = view.findViewById<ProgressBar>(R.id.pbJadwal)
        val tvEmptyJadwal = view.findViewById<TextView>(R.id.tvEmptyJadwal)
        val containerMapelDiampu = view.findViewById<LinearLayout>(R.id.containerMapelDiampu)
        val btnWaliKelas = view.findViewById<View>(R.id.btnWaliKelas)
        val tvWaliKelasTitle = view.findViewById<TextView>(R.id.tvWaliKelasTitle)
        val tvWaliKelasSiswa = view.findViewById<TextView>(R.id.tvWaliKelasSiswa)

        val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
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
                                tvTotalJamMapelHeader?.text = "Total: ${data.total_jam_minggu_ini}\nJam/Minggu"
                                tvSiswaBelumAbsen?.text = data.siswa_belum_absen.toString()

                                val inflater = LayoutInflater.from(requireContext())

                                // Render Wali Kelas
                                if (data.wali_kelas_info != null) {
                                    btnWaliKelas?.visibility = View.VISIBLE
                                    tvWaliKelasTitle?.text = "Wali Kelas (${data.wali_kelas_info.nama_kelas})"
                                    tvWaliKelasSiswa?.text = "${data.wali_kelas_info.total_siswa} Siswa"
                                    
                                    btnWaliKelas?.setOnClickListener {
                                        val intent = android.content.Intent(requireContext(), WaliKelasKehadiranActivity::class.java)
                                        startActivity(intent)
                                    }
                                } else {
                                    btnWaliKelas?.visibility = View.GONE
                                }

                                // Render Mata Pelajaran Diampu
                                containerMapelDiampu?.removeAllViews()
                                if (data.mata_pelajaran_diampu != null && data.mata_pelajaran_diampu.isNotEmpty()) {
                                    for (mapel in data.mata_pelajaran_diampu) {
                                        val itemMapel = inflater.inflate(R.layout.item_mapel_diampu, containerMapelDiampu, false)
                                        itemMapel.findViewById<TextView>(R.id.tvNamaMapel).text = mapel.nama_mapel ?: ""
                                        
                                        val kelasStr = mapel.nama_kelas
                                        itemMapel.findViewById<TextView>(R.id.tvNamaKelas).text = if (!kelasStr.isNullOrEmpty()) "Kelas $kelasStr" else "-"
                                        
                                        itemMapel.findViewById<TextView>(R.id.tvTotalJam).text = (mapel.jp_per_minggu ?: 0).toString()
                                        containerMapelDiampu?.addView(itemMapel)
                                    }
                                }

                                // Render Jadwal
                                if (data.jadwal_hari_ini != null && data.jadwal_hari_ini.isNotEmpty()) {
                                    for (jadwal in data.jadwal_hari_ini) {
                                        val itemJadwal = inflater.inflate(R.layout.item_jadwal_hari_ini, containerJadwal, false)
                                        
                                        itemJadwal.findViewById<TextView>(R.id.tvJamMulai).text = jadwal.jam_mulai?.substring(0, 5) ?: ""
                                        itemJadwal.findViewById<TextView>(R.id.tvJamSelesai).text = jadwal.jam_selesai?.substring(0, 5) ?: ""

                                        itemJadwal.findViewById<TextView>(R.id.tvMataPelajaran).text = jadwal.nama_mapel ?: ""
                                        itemJadwal.findViewById<TextView>(R.id.tvRuang)?.text = "Ruang Kelas"
                                        itemJadwal.findViewById<TextView>(R.id.tvKelas).text = jadwal.nama_kelas ?: ""
                                        
                                        itemJadwal.setOnClickListener {
                                            val intent = android.content.Intent(requireContext(), JurnalMengajarActivity::class.java)
                                            intent.putExtra("id_kelas", jadwal.id_kelas ?: "")
                                            intent.putExtra("id_mapel", jadwal.id_mapel ?: "")
                                            intent.putExtra("nama_kelas", jadwal.nama_kelas ?: "")
                                            intent.putExtra("nama_mapel", jadwal.nama_mapel ?: "")
                                            
                                            // Pass jam untuk di-autofill
                                            if (!jadwal.jam_ke.isNullOrEmpty()) {
                                                intent.putExtra("jam_ke", jadwal.jam_ke)
                                            } else {
                                                val jmMulai = jadwal.jam_mulai?.substring(0, 5) ?: ""
                                                val jmSelesai = jadwal.jam_selesai?.substring(0, 5) ?: ""
                                                if (jmMulai.isNotEmpty() && jmSelesai.isNotEmpty()) {
                                                    intent.putExtra("jam_ke", "$jmMulai - $jmSelesai WIB")
                                                }
                                            }
                                            startActivity(intent)
                                        }
                                        
                                        containerJadwal?.addView(itemJadwal)
                                    }
                                } else {
                                    tvEmptyJadwal?.visibility = View.VISIBLE
                                }
                            }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Gagal memuat dashboard (HTTP ${response.code()})"
                            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
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
