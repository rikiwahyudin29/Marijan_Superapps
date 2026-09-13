package com.rtekmidev.marijansuperapps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.ProfilGuruData
import com.rtekmidev.marijansuperapps.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetTextI18n")
class ProfilGuruFragment : Fragment(), com.rtekmidev.marijansuperapps.util.RefreshableFragment {

    private var cachedProfilData: ProfilGuruData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil_guru, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickListeners(view)
        loadProfilGuru(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadProfilGuru(it) }
    }

    override fun refreshData() {
        view?.let { loadProfilGuru(it) }
    }

    private fun initClickListeners(view: View) {
        // Tombol Edit Foto Profil
        view.findViewById<View>(R.id.btnEditFoto)?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Foto profil dapat disinkronkan melalui bagian Tata Usaha / Admin IT MA RJ.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Tombol Edit Informasi Personal
        view.findViewById<View>(R.id.btnEditProfil)?.setOnClickListener {
            showEditProfilDialog()
        }

        // Tombol Kirim / Buka WhatsApp
        view.findViewById<View>(R.id.btnOpenWhatsApp)?.setOnClickListener {
            val phoneText = view.findViewById<TextView>(R.id.tvInfoNoWa)?.text?.toString() ?: ""
            bukaWhatsApp(phoneText)
        }

        // Tombol Unduh SK Beban Mengajar
        val downloadSkAction = View.OnClickListener {
            val url = cachedProfilData?.kepegawaian?.sk_beban_mengajar?.download_url
            unduhSkMengajar(url)
        }
        view.findViewById<View>(R.id.btnUnduhSk)?.setOnClickListener(downloadSkAction)
        view.findViewById<View>(R.id.cardDownloadSk)?.setOnClickListener(downloadSkAction)

        // Administrasi Wali Kelas Quick Action: Rekap Kehadiran
        view.findViewById<View>(R.id.btnWaliRekapKehadiran)?.setOnClickListener {
            val intent = Intent(requireContext(), WaliKelasKehadiranActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

        // Ganti Password Akun
        val gantiPwAction = View.OnClickListener {
            showGantiPasswordDialog()
        }
        view.findViewById<View>(R.id.cardGantiPassword)?.setOnClickListener(gantiPwAction)
        view.findViewById<View>(R.id.btnGantiPassword)?.setOnClickListener(gantiPwAction)

        // Tes Notifikasi & Getar Panjang Pengingat Mengajar
        view.findViewById<View>(R.id.btnTesPengingat)?.setOnClickListener {
            Toast.makeText(requireContext(), "Menguji pengingat KBM & getar panjang...", Toast.LENGTH_SHORT).show()
            PengingatMengajarManager.testNotifikasiGetarPanjang(requireContext())
        }

        // Tombol Keluar Akun (Logout)
        view.findViewById<View>(R.id.btnKeluarAkun)?.setOnClickListener {
            konfirmasiKeluarAkun()
        }

        // Auto-Hide Bottom Nav on Scroll
        (view as? androidx.core.widget.NestedScrollView)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardGuruActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardGuruActivity)?.showBottomNav()
            }
        }
    }

    private fun loadProfilGuru(view: View) {
        val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", null)

        if (idUser.isNullOrEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getProfilGuru(idUser)
                withContext(Dispatchers.Main) {
                    if (isAdded && response.isSuccessful && response.body()?.status == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            cachedProfilData = data
                            renderProfilGuru(view, data)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore network error on background refresh
            }
        }
    }

    private fun renderProfilGuru(view: View, data: ProfilGuruData) {
        // 1. HERO SECTION
        val hero = data.hero
        val ivFoto = view.findViewById<ShapeableImageView>(R.id.ivProfilFoto)
        if (ivFoto != null && !hero?.foto_url.isNullOrEmpty()) {
            val fotoUrl = if (hero?.foto_url!!.startsWith("http")) {
                hero.foto_url
            } else {
                ApiClient.BASE_URL.trimEnd('/') + "/" + hero.foto_url.trimStart('/')
            }
            Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.ic_modern_profile)
                .error(R.drawable.ic_modern_profile)
                .into(ivFoto)
        }

        view.findViewById<TextView>(R.id.tvHeroNamaGuru)?.text = hero?.nama ?: "Bapak/Ibu Guru"
        view.findViewById<TextView>(R.id.tvHeroNip)?.text = "NIP: ${hero?.nip ?: "-"}"
        view.findViewById<TextView>(R.id.tvHeroNuptk)?.text = "NUPTK: ${hero?.nuptk ?: "-"}"
        view.findViewById<TextView>(R.id.tvHeroStatusKepegawaianBadge)?.text =
            hero?.status_kepegawaian_badge ?: "GTY Tetap"

        val tvMapel = view.findViewById<TextView>(R.id.tvHeroBadgeMapel)
        val badgeMapel = view.findViewById<View>(R.id.badgeHeroMapel)
        if (!hero?.mapel_badge.isNullOrEmpty()) {
            badgeMapel?.visibility = View.VISIBLE
            tvMapel?.text = hero?.mapel_badge
        } else {
            badgeMapel?.visibility = View.GONE
        }

        val tvWali = view.findViewById<TextView>(R.id.tvHeroBadgeWali)
        val badgeWali = view.findViewById<View>(R.id.badgeHeroWali)
        if (!hero?.wali_kelas_badge.isNullOrEmpty()) {
            badgeWali?.visibility = View.VISIBLE
            tvWali?.text = hero?.wali_kelas_badge
        } else {
            badgeWali?.visibility = View.GONE
        }

        // 2. METRICS ROW
        val metrics = data.metrics
        view.findViewById<TextView>(R.id.tvMetricJamValue)?.text =
            metrics?.beban_mengajar?.value ?: "26 Jam/Mg"
        view.findViewById<TextView>(R.id.tvMetricJamBadge)?.text =
            metrics?.beban_mengajar?.badge ?: "Target 100%"

        view.findViewById<TextView>(R.id.tvMetricPresensiValue)?.text =
            metrics?.presensi_bulan_ini?.value ?: "96%"
        view.findViewById<TextView>(R.id.tvMetricPresensiBadge)?.text =
            metrics?.presensi_bulan_ini?.badge ?: "Sangat Baik"

        view.findViewById<TextView>(R.id.tvMetricSiswaValue)?.text =
            metrics?.siswa_binaan?.value ?: "34 Siswa"
        view.findViewById<TextView>(R.id.tvMetricSiswaBadge)?.text =
            metrics?.siswa_binaan?.badge ?: "12 TKJT 1"

        // 3. INFORMASI PERSONAL
        val info = data.informasi_personal
        view.findViewById<TextView>(R.id.tvInfoNoWa)?.text = info?.no_whatsapp ?: "-"
        view.findViewById<TextView>(R.id.tvInfoEmail)?.text = info?.email ?: "-"
        view.findViewById<TextView>(R.id.tvInfoTglLahir)?.text = info?.tempat_tgl_lahir ?: "-"
        view.findViewById<TextView>(R.id.tvInfoJk)?.text = info?.jenis_kelamin ?: "-"
        view.findViewById<TextView>(R.id.tvInfoAlamat)?.text = info?.alamat_lengkap ?: "-"

        // 4. DATA KEPEGAWAIAN & SERTIFIKASI
        val kepeg = data.kepegawaian
        view.findViewById<TextView>(R.id.tvKepegawaianStatus)?.text =
            kepeg?.status_badge ?: "GTY (Guru Tetap Yayasan)"
        view.findViewById<TextView>(R.id.tvKepegawaianPendidikan)?.text =
            kepeg?.pendidikan_terakhir ?: "D4 / S1 Teknik Komputer & Jaringan"
        val sk = kepeg?.sk_beban_mengajar
        if (sk != null) {
            view.findViewById<TextView>(R.id.tvSkJudul)?.text = sk.judul ?: "SK Beban Mengajar 2026/2027"
            view.findViewById<TextView>(R.id.tvSkKeterangan)?.text = sk.keterangan ?: "Format PDF • Resmi"
        }

        // 5. ADMINISTRASI WALI KELAS
        val wali = data.administrasi_wali_kelas
        val sectionWali = view.findViewById<View>(R.id.sectionWaliKelas)
        if (wali?.is_wali_kelas == true) {
            sectionWali?.visibility = View.VISIBLE
            val namaKls = wali.nama_kelas ?: "Kelas Binaan"
            view.findViewById<TextView>(R.id.tvWaliKelasHeaderSubtitle)?.text = namaKls
            view.findViewById<TextView>(R.id.tvWaliKelasNamaKelas)?.text = namaKls
            view.findViewById<TextView>(R.id.tvWaliKelasJurusan)?.text =
                wali.jurusan ?: "Kompetensi Keahlian MA RJ"
            view.findViewById<TextView>(R.id.tvWaliKelasTotalSiswa)?.text =
                (wali.total_siswa).toString()
            view.findViewById<TextView>(R.id.tvWaliKelasStatusBadge)?.text =
                wali.status_semester ?: "Aktif Semester Ini"
        } else {
            sectionWali?.visibility = View.GONE
        }

        // 6. KEAMANAN & PENGATURAN AKUN
        val aman = data.keamanan
        view.findViewById<TextView>(R.id.tvTerakhirGantiPw)?.text =
            aman?.terakhir_ganti_password ?: "Terakhir diperbarui beberapa waktu lalu"
        view.findViewById<TextView>(R.id.tvTwoFactorStatus)?.text =
            aman?.two_factor_auth ?: "Google Authenticator • Status Aktif"

        // Nama Device (prioritaskan nama perangkat saat ini jika dinamis)
        val currentDevice = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        view.findViewById<TextView>(R.id.tvDeviceName)?.text =
            aman?.device_name ?: currentDevice
        view.findViewById<TextView>(R.id.tvDeviceStatus)?.text =
            aman?.device_status ?: "Perangkat Ini • Aktif Sekarang"

        // 7. FOOTER
        val appInfo = data.app_info
        if (appInfo != null) {
            view.findViewById<TextView>(R.id.tvFooterAppName)?.text =
                appInfo.app_name ?: "MA Riyadhul Jannah SuperApps v2.4.1"
            view.findViewById<TextView>(R.id.tvFooterPortalDesc)?.text =
                appInfo.portal_desc ?: "Portal Sistem Informasi Akademik & Manajemen Pendidik"
        }
    }

    private fun showEditProfilDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profil_guru, null)
        dialog.setContentView(dialogView)

        val etWa = dialogView.findViewById<EditText>(R.id.etEditNoWa)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEditEmail)
        val etAlamat = dialogView.findViewById<EditText>(R.id.etEditAlamat)
        val btnBatal = dialogView.findViewById<TextView>(R.id.btnBatalEdit)
        val btnSimpan = dialogView.findViewById<TextView>(R.id.btnSimpanEdit)

        val currentInfo = cachedProfilData?.informasi_personal
        etWa.setText(currentInfo?.no_whatsapp ?: "")
        etEmail.setText(currentInfo?.email ?: "")
        etAlamat.setText(currentInfo?.alamat_lengkap ?: "")

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnSimpan.setOnClickListener {
            val wa = etWa.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val alamat = etAlamat.text.toString().trim()

            if (wa.isEmpty()) {
                etWa.error = "No WhatsApp tidak boleh kosong"
                etWa.requestFocus()
                return@setOnClickListener
            }

            val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            val idUser = sharedPref.getString("id_user", null) ?: return@setOnClickListener

            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val resp = ApiClient.instance.updateProfilGuru(
                        idUser = idUser,
                        noWhatsapp = wa,
                        email = email,
                        alamat = alamat
                    )
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Simpan"
                        if (resp.isSuccessful && resp.body()?.status == true) {
                            Toast.makeText(
                                requireContext(),
                                resp.body()?.message ?: "Profil berhasil diperbarui",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            view?.let { loadProfilGuru(it) }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                resp.body()?.message ?: "Gagal memperbarui profil",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Simpan"
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showGantiPasswordDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_ganti_password_guru, null)
        dialog.setContentView(dialogView)

        val etLama = dialogView.findViewById<EditText>(R.id.etPasswordLama)
        val etBaru = dialogView.findViewById<EditText>(R.id.etPasswordBaru)
        val etKonf = dialogView.findViewById<EditText>(R.id.etPasswordBaruKonfirmasi)
        val btnBatal = dialogView.findViewById<TextView>(R.id.btnBatalPw)
        val btnSimpan = dialogView.findViewById<TextView>(R.id.btnSimpanPw)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnSimpan.setOnClickListener {
            val pwLama = etLama.text.toString().trim()
            val pwBaru = etBaru.text.toString().trim()
            val pwKonf = etKonf.text.toString().trim()

            if (pwLama.isEmpty()) {
                etLama.error = "Masukkan password lama"
                etLama.requestFocus()
                return@setOnClickListener
            }

            if (pwBaru.length < 6) {
                etBaru.error = "Password baru minimal 6 karakter"
                etBaru.requestFocus()
                return@setOnClickListener
            }

            if (pwBaru != pwKonf) {
                etKonf.error = "Konfirmasi password tidak cocok"
                etKonf.requestFocus()
                return@setOnClickListener
            }

            val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            val idUser = sharedPref.getString("id_user", null) ?: return@setOnClickListener

            btnSimpan.isEnabled = false
            btnSimpan.text = "Memproses..."

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val resp = ApiClient.instance.gantiPasswordGuru(
                        idUser = idUser,
                        passwordLama = pwLama,
                        passwordBaru = pwBaru
                    )
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Perbarui"
                        if (resp.isSuccessful && resp.body()?.status == true) {
                            Toast.makeText(
                                requireContext(),
                                resp.body()?.message ?: "Password berhasil diubah",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            view?.let { loadProfilGuru(it) }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                resp.body()?.message ?: "Gagal mengubah password",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Perbarui"
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun bukaWhatsApp(phoneText: String) {
        val cleanPhone = phoneText.replace("[^0-9]".toRegex(), "")
        if (cleanPhone.isEmpty()) {
            Toast.makeText(requireContext(), "Nomor WhatsApp belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val formatted = if (cleanPhone.startsWith("0")) {
            "62" + cleanPhone.substring(1)
        } else if (cleanPhone.startsWith("8")) {
            "62$cleanPhone"
        } else {
            cleanPhone
        }

        try {
            val url = "https://api.whatsapp.com/send?phone=$formatted"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unduhSkMengajar(rawUrl: String?) {
        val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", "") ?: ""

        val finalUrl = if (!rawUrl.isNullOrEmpty()) {
            if (rawUrl.startsWith("http")) rawUrl else ApiClient.BASE_URL.trimEnd('/') + "/" + rawUrl.trimStart('/')
        } else {
            ApiClient.BASE_URL.trimEnd('/') + "/api/akademik-guru/download-sk-mengajar?id_user=" + idUser
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka link unduhan SK", Toast.LENGTH_SHORT).show()
        }
    }

    private fun konfirmasiKeluarAkun() {
        SessionManager.konfirmasiLogout(
            requireActivity(),
            "Apakah Anda yakin ingin keluar dari akun Portal MA Riyadhul Jannah?"
        )
    }
}
