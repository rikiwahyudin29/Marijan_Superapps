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
            Toast.makeText(requireContext(), "Sedang memuat data jadwal...", Toast.LENGTH_SHORT).show()
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

                                // Quick Action: Jurnal Mengajar
                                view?.findViewById<View>(R.id.btnJurnalMengajar)?.setOnClickListener {
                                    val jadwalHariIniList = data.jadwal_hari_ini ?: emptyList()
                                    if (jadwalHariIniList.isEmpty()) {
                                        Toast.makeText(requireContext(), "Tidak ada jadwal mengajar hari ini", Toast.LENGTH_SHORT).show()
                                        return@setOnClickListener
                                    }
                                    
                                    val currentTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                    var activeJadwal: com.rtekmidev.marijancbt.api.JadwalGuruHariIni? = null
                                    
                                    for (j in jadwalHariIniList) {
                                        val start = j.jam_mulai?.substring(0, 5) ?: ""
                                        val end = j.jam_selesai?.substring(0, 5) ?: ""
                                        if (start.isNotEmpty() && end.isNotEmpty()) {
                                            if (currentTimeStr >= start && currentTimeStr <= end) {
                                                activeJadwal = j
                                                break
                                            }
                                        }
                                    }
                                    
                                    if (activeJadwal == null) {
                                        Toast.makeText(requireContext(), "Tidak ada jadwal yang sedang berlangsung saat ini", Toast.LENGTH_SHORT).show()
                                    } else if (activeJadwal.is_jurnal_filled == true) {
                                        Toast.makeText(requireContext(), "Jurnal untuk jadwal aktif sudah diisi", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val intent = android.content.Intent(requireContext(), JurnalMengajarActivity::class.java)
                                        intent.putExtra("id_kelas", activeJadwal.id_kelas ?: "")
                                        intent.putExtra("id_mapel", activeJadwal.id_mapel ?: "")
                                        intent.putExtra("nama_kelas", activeJadwal.nama_kelas ?: "")
                                        intent.putExtra("nama_mapel", activeJadwal.nama_mapel ?: "")
                                        if (!activeJadwal.jam_ke.isNullOrEmpty()) {
                                            intent.putExtra("jam_ke", activeJadwal.jam_ke)
                                        } else {
                                            val jmMulai = activeJadwal.jam_mulai?.substring(0, 5) ?: ""
                                            val jmSelesai = activeJadwal.jam_selesai?.substring(0, 5) ?: ""
                                            intent.putExtra("jam_ke", "$jmMulai - $jmSelesai WIB")
                                        }
                                        startActivity(intent)
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
                                        
                                        val ivAction = itemJadwal.findViewById<android.widget.ImageView>(R.id.ivAction)
                                        val llJurnalDetail = itemJadwal.findViewById<LinearLayout>(R.id.llJurnalDetail)
                                        val tvJurnalMateri = itemJadwal.findViewById<TextView>(R.id.tvJurnalMateri)
                                        val tvJurnalPresensi = itemJadwal.findViewById<TextView>(R.id.tvJurnalPresensi)
                                        val cvFotoPreview = itemJadwal.findViewById<androidx.cardview.widget.CardView>(R.id.cvFotoPreview)
                                        val ivFotoPreview = itemJadwal.findViewById<android.widget.ImageView>(R.id.ivFotoPreview)
                                        
                                        if (jadwal.is_jurnal_filled == true) {
                                            // Sudah diisi
                                            ivAction?.setImageResource(android.R.drawable.ic_menu_edit) // Or any check icon
                                            ivAction?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981")) // Green
                                            itemJadwal.alpha = 0.7f // Dim the item to indicate it's done
                                            llJurnalDetail?.visibility = View.VISIBLE
                                            tvJurnalMateri?.text = "Materi: ${jadwal.jurnal_materi ?: "-"}"
                                            tvJurnalPresensi?.text = jadwal.jurnal_presensi ?: "Presensi Belum Tersedia"
                                            
                                            if (!jadwal.jurnal_foto.isNullOrEmpty()) {
                                                cvFotoPreview?.visibility = View.VISIBLE
                                                ivFotoPreview?.let {
                                                    com.bumptech.glide.Glide.with(requireContext())
                                                        .load(jadwal.jurnal_foto)
                                                        .into(it)
                                                }
                                                cvFotoPreview?.setOnClickListener {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                                        intent.data = android.net.Uri.parse(jadwal.jurnal_foto)
                                                        startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(requireContext(), "Tidak dapat membuka foto", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                cvFotoPreview?.visibility = View.GONE
                                            }
                                            
                                            itemJadwal.setOnClickListener {
                                                Toast.makeText(requireContext(), "Jurnal untuk kelas ini sudah diisi hari ini", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            // Belum diisi
                                            ivAction?.setImageResource(android.R.drawable.ic_media_play)
                                            ivAction?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4F46E5")) // Primary Blue
                                            itemJadwal.alpha = 1.0f
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
