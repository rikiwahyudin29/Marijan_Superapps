package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
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
            requireActivity().applyEnterTransition()
        }
        

        view.findViewById<View>(R.id.btnJadwalMengajar)?.setOnClickListener {
            val intent = Intent(requireContext(), JadwalMengajarActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvTotalJamMingguIni = view.findViewById<TextView>(R.id.tvTotalJamMingguIni)
        val tvTotalJamMapelHeader = view.findViewById<TextView>(R.id.tvTotalJamMapelHeader)
        val tvSiswaBelumAbsen = view.findViewById<TextView>(R.id.tvSiswaBelumAbsen)
        val containerJadwal = view.findViewById<LinearLayout>(R.id.containerJadwalHariIni)
        val pbJadwal = view.findViewById<View>(R.id.pbJadwal)
        val tvEmptyJadwal = view.findViewById<TextView>(R.id.tvEmptyJadwal)
        val containerMapelDiampu = view.findViewById<LinearLayout>(R.id.containerMapelDiampu)
        val sectionWaliKelas = view.findViewById<View>(R.id.sectionWaliKelas)
        val tvWaliKelasBadge = view.findViewById<TextView>(R.id.tvWaliKelasBadge)
        val btnWaliAbsenHarian = view.findViewById<View>(R.id.btnWaliAbsenHarian)
        val btnWaliRekapKehadiran = view.findViewById<View>(R.id.btnWaliRekapKehadiran)
        val btnWaliKeuanganKelas = view.findViewById<View>(R.id.btnWaliKeuanganKelas)

        view.findViewById<View>(R.id.btnLihatSemuaJadwal)?.setOnClickListener {
            val intent = Intent(requireContext(), JadwalMengajarActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

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
                                
                                val tvLabelSiswaBelumAbsen = view?.findViewById<TextView>(R.id.tvLabelSiswaBelumAbsen)
                                val ivSiswaBelumAbsenIcon = view?.findViewById<android.widget.ImageView>(R.id.ivSiswaBelumAbsenIcon)
                                if (data.is_libur == true) {
                                    tvSiswaBelumAbsen?.text = "0"
                                    tvLabelSiswaBelumAbsen?.text = "Libur"
                                    tvSiswaBelumAbsen?.setTextColor(android.graphics.Color.parseColor("#059669"))
                                    tvLabelSiswaBelumAbsen?.setTextColor(android.graphics.Color.parseColor("#10B981"))
                                    ivSiswaBelumAbsenIcon?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#ECFDF5"))
                                    ivSiswaBelumAbsenIcon?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981"))
                                } else {
                                    val belumAbsen = data.siswa_belum_absen ?: 0
                                    tvSiswaBelumAbsen?.text = belumAbsen.toString()
                                    tvLabelSiswaBelumAbsen?.text = "Siswa"
                                    if (belumAbsen > 0) {
                                        tvSiswaBelumAbsen?.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                                        tvLabelSiswaBelumAbsen?.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                                        ivSiswaBelumAbsenIcon?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF2F2"))
                                        ivSiswaBelumAbsenIcon?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444"))
                                    } else {
                                        tvSiswaBelumAbsen?.setTextColor(android.graphics.Color.parseColor("#059669"))
                                        tvLabelSiswaBelumAbsen?.setTextColor(android.graphics.Color.parseColor("#10B981"))
                                        ivSiswaBelumAbsenIcon?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#ECFDF5"))
                                        ivSiswaBelumAbsenIcon?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981"))
                                    }
                                }

                                val inflater = LayoutInflater.from(requireContext())

                                // Render Section Wali Kelas (Hanya muncul jika guru adalah wali kelas)
                                if (data.wali_kelas_info != null && !data.wali_kelas_info.nama_kelas.isNullOrEmpty()) {
                                    sectionWaliKelas?.visibility = View.VISIBLE
                                    val namaKelas = data.wali_kelas_info.nama_kelas ?: "-"
                                    val totalSiswa = data.wali_kelas_info.total_siswa ?: 0
                                    
                                    tvWaliKelasBadge?.text = "Kelas: $namaKelas ($totalSiswa Siswa)"

                                    btnWaliAbsenHarian?.setOnClickListener {
                                        val intent = Intent(requireContext(), WaliKelasAbsenHarianActivity::class.java)
                                        intent.putExtra("nama_kelas", namaKelas)
                                        startActivity(intent)
                                        requireActivity().applyEnterTransition()
                                    }

                                    btnWaliRekapKehadiran?.setOnClickListener {
                                        val intent = Intent(requireContext(), WaliKelasKehadiranActivity::class.java)
                                        intent.putExtra("nama_kelas", namaKelas)
                                        intent.putExtra("mode", "rekap")
                                        startActivity(intent)
                                        requireActivity().applyEnterTransition()
                                    }

                                    btnWaliKeuanganKelas?.setOnClickListener {
                                        val intent = Intent(requireContext(), WaliKelasKeuanganActivity::class.java)
                                        intent.putExtra("nama_kelas", namaKelas)
                                        startActivity(intent)
                                        requireActivity().applyEnterTransition()
                                    }
                                } else {
                                    sectionWaliKelas?.visibility = View.GONE
                                }

                                // Render Mata Pelajaran Diampu
                                val cvMapelDiampu = view?.findViewById<View>(R.id.cvMapelDiampu)
                                containerMapelDiampu?.removeAllViews()
                                if (data.mata_pelajaran_diampu != null && data.mata_pelajaran_diampu.isNotEmpty()) {
                                    cvMapelDiampu?.visibility = View.VISIBLE
                                    for (mapel in data.mata_pelajaran_diampu) {
                                        val itemMapel = inflater.inflate(R.layout.item_mapel_diampu, containerMapelDiampu, false)
                                        itemMapel.findViewById<TextView>(R.id.tvNamaMapel).text = mapel.nama_mapel ?: ""
                                        
                                        val kelasStr = mapel.nama_kelas
                                        itemMapel.findViewById<TextView>(R.id.tvNamaKelas).text = if (!kelasStr.isNullOrEmpty()) "Kelas $kelasStr" else "-"
                                        
                                        itemMapel.findViewById<TextView>(R.id.tvTotalJam).text = (mapel.jp_per_minggu ?: 0).toString()
                                        containerMapelDiampu?.addView(itemMapel)
                                    }
                                } else {
                                    cvMapelDiampu?.visibility = View.GONE
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
                                        requireActivity().applyEnterTransition()
                                    }
                                }

                                // Render Jadwal
                                if (data.jadwal_hari_ini != null && data.jadwal_hari_ini.isNotEmpty()) {
                                    for (jadwal in data.jadwal_hari_ini) {
                                        val itemJadwal = inflater.inflate(R.layout.item_jadwal_hari_ini, containerJadwal, false)
                                        
                                        itemJadwal.findViewById<TextView>(R.id.tvJamMulai).text = jadwal.jam_mulai?.substring(0, 5) ?: ""
                                        itemJadwal.findViewById<TextView>(R.id.tvJamSelesai).text = jadwal.jam_selesai?.substring(0, 5) ?: ""

                                        itemJadwal.findViewById<TextView>(R.id.tvMataPelajaran).text = jadwal.nama_mapel ?: ""
                                        val klsNama = jadwal.nama_kelas?.trim().orEmpty()
                                        itemJadwal.findViewById<TextView>(R.id.tvRuang)?.text = if (klsNama.isNotEmpty()) "Ruang $klsNama" else "Ruang Kelas"
                                        itemJadwal.findViewById<TextView>(R.id.tvKelas).text = klsNama
                                        
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
                                                requireActivity().applyEnterTransition()
                                            }
                                        }
                                        
                                        containerJadwal?.addView(itemJadwal)
                                    }
                                } else {
                                    tvEmptyJadwal?.visibility = View.VISIBLE
                                    if (data.is_libur == true) {
                                        val ket = data.keterangan_libur ?: "Libur Akhir Pekan"
                                        tvEmptyJadwal?.text = "Hari Libur ($ket)\nTidak ada kegiatan belajar mengajar hari ini"
                                    } else {
                                        tvEmptyJadwal?.text = "Tidak ada jadwal mengajar hari ini"
                                    }
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
