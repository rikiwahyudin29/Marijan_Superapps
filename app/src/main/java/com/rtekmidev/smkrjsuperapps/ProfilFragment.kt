package com.rtekmidev.smkrjsuperapps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.ProfilSiswaData
import com.rtekmidev.smkrjsuperapps.util.LoadingDialogHelper
import com.rtekmidev.smkrjsuperapps.util.RefreshableFragment
import com.rtekmidev.smkrjsuperapps.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetTextI18n")
class ProfilFragment : Fragment(), RefreshableFragment {

    private var cachedProfilData: ProfilSiswaData? = null
    private var nisnSiswa = ""
    private var loadingDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""

        initClickListeners(view)
        loadProfilSiswa(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadProfilSiswa(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = null
    }

    override fun refreshData() {
        view?.let { loadProfilSiswa(it) }
    }

    private fun initClickListeners(view: View) {
        // Tombol Edit Foto Profil
        view.findViewById<View>(R.id.btnEditFotoSiswa)?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Foto profil siswa dapat disinkronkan melalui bagian Tata Usaha / Wali Kelas SMK RJ.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Tombol Edit Informasi Personal Siswa
        view.findViewById<View>(R.id.btnEditProfilSiswa)?.setOnClickListener {
            showEditProfilDialog()
        }

        // Tombol Kirim / Buka WhatsApp Siswa
        view.findViewById<View>(R.id.btnOpenWhatsAppSiswa)?.setOnClickListener {
            val phoneText = view.findViewById<TextView>(R.id.tvInfoNoWaSiswa)?.text?.toString() ?: ""
            bukaWhatsApp(phoneText)
        }

        // Tombol Kartu Pelajar Digital (Lihat QR & Identitas Resmi)
        val kartuAction = View.OnClickListener {
            showKartuPelajarDialog()
        }
        view.findViewById<View>(R.id.btnLihatKartuPelajar)?.setOnClickListener(kartuAction)
        view.findViewById<View>(R.id.cardKartuPelajarDigital)?.setOnClickListener(kartuAction)

        // Ganti Password Akun Siswa
        val gantiPwAction = View.OnClickListener {
            showGantiPasswordDialog()
        }
        view.findViewById<View>(R.id.cardGantiPasswordSiswa)?.setOnClickListener(gantiPwAction)
        view.findViewById<View>(R.id.btnGantiPasswordSiswa)?.setOnClickListener(gantiPwAction)

        // Tombol Keluar Akun Siswa (Logout)
        view.findViewById<View>(R.id.btnKeluarAkunSiswa)?.setOnClickListener {
            konfirmasiKeluarAkun()
        }

        // Auto-Hide Bottom Nav on Scroll di DashboardActivity
        (view as? androidx.core.widget.NestedScrollView)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardActivity)?.showBottomNav()
            }
        }
    }

    private fun loadProfilSiswa(view: View) {
        if (nisnSiswa.isEmpty()) {
            val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        }
        if (nisnSiswa.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getProfilSiswa(nisnSiswa)
                withContext(Dispatchers.Main) {
                    if (isAdded && response.isSuccessful && response.body()?.status == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            cachedProfilData = data
                            renderProfilSiswa(view, data)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore silent background refresh error
            }
        }
    }

    private fun renderProfilSiswa(view: View, data: ProfilSiswaData) {
        // 1. HERO SECTION
        val hero = data.hero
        val ivFoto = view.findViewById<ShapeableImageView>(R.id.ivProfilFotoSiswa)
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

        view.findViewById<TextView>(R.id.tvHeroNamaSiswa)?.text = hero?.nama ?: "Siswa"
        view.findViewById<TextView>(R.id.tvHeroNisn)?.text = hero?.nisn ?: "NISN: -"
        view.findViewById<TextView>(R.id.tvHeroNis)?.text = hero?.nis ?: "NIS: -"
        view.findViewById<TextView>(R.id.tvHeroStatusBadge)?.text = hero?.status_badge ?: "Siswa Aktif"

        val tvKelasJurusan = view.findViewById<TextView>(R.id.tvHeroBadgeKelasJurusan)
        val badgeKelasJurusan = view.findViewById<View>(R.id.badgeHeroKelasJurusan)
        val kelasTxt = hero?.kelas_badge ?: ""
        val jurTxt = hero?.jurusan_badge ?: ""
        if (kelasTxt.isNotEmpty() || jurTxt.isNotEmpty()) {
            badgeKelasJurusan?.visibility = View.VISIBLE
            tvKelasJurusan?.text = if (jurTxt.isNotEmpty()) "$kelasTxt • $jurTxt" else kelasTxt
        } else {
            badgeKelasJurusan?.visibility = View.GONE
        }

        val tvWali = view.findViewById<TextView>(R.id.tvHeroBadgeWaliSiswa)
        val badgeWali = view.findViewById<View>(R.id.badgeHeroWaliSiswa)
        if (!hero?.wali_kelas_badge.isNullOrEmpty()) {
            badgeWali?.visibility = View.VISIBLE
            tvWali?.text = hero?.wali_kelas_badge
        } else {
            badgeWali?.visibility = View.GONE
        }

        // 2. METRICS ROW
        val metrics = data.metrics
        view.findViewById<TextView>(R.id.tvMetricPresensiValue)?.text =
            metrics?.presensi?.value ?: "98%"
        view.findViewById<TextView>(R.id.tvMetricPresensiBadge)?.text =
            metrics?.presensi?.badge ?: "Sangat Baik"

        view.findViewById<TextView>(R.id.tvMetricMapelValue)?.text =
            metrics?.mapel?.value ?: "14 Mapel"
        view.findViewById<TextView>(R.id.tvMetricMapelBadge)?.text =
            metrics?.mapel?.badge ?: "Kurikulum M"

        view.findViewById<TextView>(R.id.tvMetricDisiplinValue)?.text =
            metrics?.poin_disiplin?.value ?: "100 Poin"
        view.findViewById<TextView>(R.id.tvMetricDisiplinBadge)?.text =
            metrics?.poin_disiplin?.badge ?: "Sangat Baik"

        // 3. INFORMASI PERSONAL
        val info = data.informasi_personal
        view.findViewById<TextView>(R.id.tvInfoNoWaSiswa)?.text = info?.no_whatsapp ?: "-"
        view.findViewById<TextView>(R.id.tvInfoEmailSiswa)?.text = info?.email ?: "-"
        view.findViewById<TextView>(R.id.tvInfoTglLahirSiswa)?.text = info?.tempat_tgl_lahir ?: "-"
        view.findViewById<TextView>(R.id.tvInfoJkSiswa)?.text = info?.jenis_kelamin ?: "-"
        view.findViewById<TextView>(R.id.tvInfoAlamatSiswa)?.text = info?.alamat_lengkap ?: "-"

        // 4. DATA AKADEMIK & KESISWAAN
        val akademik = data.akademik_kesiswaan
        val kelasJurusanDisplay = "${akademik?.nama_kelas ?: "-"} • ${akademik?.jurusan ?: "-"}"
        view.findViewById<TextView>(R.id.tvAkademikKelasJurusan)?.text = kelasJurusanDisplay
        view.findViewById<TextView>(R.id.tvAkademikWaliKelas)?.text = akademik?.wali_kelas ?: "-"

        val kartu = akademik?.kartu_pelajar
        if (kartu != null) {
            view.findViewById<TextView>(R.id.tvKartuPelajarJudul)?.text =
                kartu.judul ?: "Kartu Pelajar Digital 2026/2027"
            view.findViewById<TextView>(R.id.tvKartuPelajarKet)?.text =
                kartu.keterangan ?: "Format Digital • QR Code Resmi • Aktif"
        }

        // 5. KEAMANAN & PENGATURAN AKUN
        val keamanan = data.keamanan
        view.findViewById<TextView>(R.id.tvTerakhirGantiPwSiswa)?.text =
            keamanan?.terakhir_ganti_password ?: "Terakhir diperbarui beberapa waktu lalu"
        view.findViewById<TextView>(R.id.tvTwoFactorStatusSiswa)?.text =
            keamanan?.two_factor_auth ?: "Autentikasi Akun Terverifikasi"

        val currentDevice = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        view.findViewById<TextView>(R.id.tvDeviceNameSiswa)?.text = currentDevice
        view.findViewById<TextView>(R.id.tvDeviceStatusSiswa)?.text =
            keamanan?.device_status ?: "Perangkat Ini • Aktif Sekarang"

        // 6. APP INFO
        val appInfo = data.app_info
        if (appInfo != null) {
            view.findViewById<TextView>(R.id.tvFooterAppNameSiswa)?.text =
                appInfo.app_name ?: "SMK Riyadhul Jannah SuperApps v2.4.1"
            view.findViewById<TextView>(R.id.tvFooterPortalDescSiswa)?.text =
                appInfo.portal_desc ?: "Portal Kesiswaan & Manajemen Akademik Digital"
        }
    }

    private fun showEditProfilDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profil_siswa, null)
        dialog.setContentView(dialogView)

        val etNoWa = dialogView.findViewById<EditText>(R.id.etEditNoWaSiswa)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEditEmailSiswa)
        val etAlamat = dialogView.findViewById<EditText>(R.id.etEditAlamatSiswa)
        val btnBatal = dialogView.findViewById<TextView>(R.id.btnBatalEditSiswa)
        val btnSimpan = dialogView.findViewById<TextView>(R.id.btnSimpanEditSiswa)

        val currentInfo = cachedProfilData?.informasi_personal
        val rawWa = currentInfo?.no_whatsapp?.takeIf { it != "-" } ?: ""
        val rawEmail = currentInfo?.email?.takeIf { it != "-" } ?: ""
        val rawAlamat = currentInfo?.alamat_lengkap?.takeIf { it != "-" } ?: ""

        etNoWa?.setText(rawWa)
        etEmail?.setText(rawEmail)
        etAlamat?.setText(rawAlamat)

        btnBatal?.setOnClickListener { dialog.dismiss() }

        btnSimpan?.setOnClickListener {
            val newWa = etNoWa?.text?.toString()?.trim() ?: ""
            val newEmail = etEmail?.text?.toString()?.trim() ?: ""
            val newAlamat = etAlamat?.text?.toString()?.trim() ?: ""

            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val resp = ApiClient.instance.updateProfilSiswa(
                        nisn = nisnSiswa,
                        noWhatsapp = newWa,
                        email = newEmail,
                        alamat = newAlamat
                    )
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Simpan"
                        if (resp.isSuccessful && resp.body()?.status == true) {
                            Toast.makeText(
                                requireContext(),
                                resp.body()?.message ?: "Profil siswa berhasil diperbarui!",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            view?.let { loadProfilSiswa(it) }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                resp.body()?.message ?: "Gagal memperbarui profil siswa",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Simpan"
                        Toast.makeText(requireContext(), "Koneksi terganggu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showGantiPasswordDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_ganti_password_siswa, null)
        dialog.setContentView(dialogView)

        val etLama = dialogView.findViewById<EditText>(R.id.etPasswordLamaSiswa)
        val etBaru = dialogView.findViewById<EditText>(R.id.etPasswordBaruSiswa)
        val etKonf = dialogView.findViewById<EditText>(R.id.etKonfirmasiPasswordBaruSiswa)
        val btnBatal = dialogView.findViewById<TextView>(R.id.btnBatalGantiPwSiswa)
        val btnSimpan = dialogView.findViewById<TextView>(R.id.btnSimpanGantiPwSiswa)

        btnBatal?.setOnClickListener { dialog.dismiss() }

        btnSimpan?.setOnClickListener {
            val pwLama = etLama?.text?.toString()?.trim() ?: ""
            val pwBaru = etBaru?.text?.toString()?.trim() ?: ""
            val pwKonf = etKonf?.text?.toString()?.trim() ?: ""

            if (pwLama.isEmpty()) {
                etLama?.error = "Masukkan password lama"
                etLama?.requestFocus()
                return@setOnClickListener
            }

            if (pwBaru.length < 6) {
                etBaru?.error = "Password baru minimal 6 karakter"
                etBaru?.requestFocus()
                return@setOnClickListener
            }

            if (pwBaru != pwKonf) {
                etKonf?.error = "Konfirmasi password tidak cocok"
                etKonf?.requestFocus()
                return@setOnClickListener
            }

            btnSimpan.isEnabled = false
            btnSimpan.text = "Memproses..."

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val resp = ApiClient.instance.gantiPasswordSiswa(
                        nisn = nisnSiswa,
                        passwordLama = pwLama,
                        passwordBaru = pwBaru
                    )
                    withContext(Dispatchers.Main) {
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Ganti Password"
                        if (resp.isSuccessful && resp.body()?.status == true) {
                            Toast.makeText(
                                requireContext(),
                                resp.body()?.message ?: "Password berhasil diubah!",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            view?.let { loadProfilSiswa(it) }
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
                        btnSimpan.text = "Ganti Password"
                        Toast.makeText(requireContext(), "Koneksi terganggu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showKartuPelajarDialog() {
        val hero = cachedProfilData?.hero
        val akademik = cachedProfilData?.akademik_kesiswaan

        val nama = hero?.nama ?: "Siswa"
        val nisn = akademik?.nisn?.takeIf { it != "-" } ?: nisnSiswa
        val kelasJurusan = "${akademik?.nama_kelas ?: "-"} • ${akademik?.jurusan ?: "-"}"

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_kartu_pelajar, null)
        dialog.setContentView(dialogView)

        dialogView.findViewById<TextView>(R.id.tvKartuNamaSiswa)?.text = nama
        dialogView.findViewById<TextView>(R.id.tvKartuNisnNis)?.text = "NISN: $nisn • NIS: ${akademik?.nis ?: "-"}"
        dialogView.findViewById<TextView>(R.id.tvKartuKelasJurusan)?.text = kelasJurusan
        dialogView.findViewById<TextView>(R.id.tvKartuWaliKelas)?.text = "Wali Kelas: ${akademik?.wali_kelas ?: "-"}"

        // Generate barcode QR Code
        val ivQr = dialogView.findViewById<ImageView>(R.id.ivKartuQrCode)
        try {
            val qrText = "SMKRJ-SISWA-$nisn"
            val encoder = BarcodeEncoder()
            val bitmap = encoder.encodeBitmap(qrText, BarcodeFormat.QR_CODE, 450, 450)
            ivQr?.setImageBitmap(bitmap)
        } catch (_: Exception) {}

        dialogView.findViewById<View>(R.id.btnUnduhKartuPelajarWeb)?.setOnClickListener {
            val kartuUrl = ApiClient.BASE_URL.trimEnd('/') + "/kartu-pelajar/" + nisn
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kartuUrl))
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Gagal membuka link Kartu Pelajar", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<View>(R.id.btnCloseKartuPelajar)?.setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btnTutupKartuPelajar)?.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun bukaWhatsApp(phone: String) {
        val clean = phone.replace(Regex("[^0-9+]"), "")
        val formatted = when {
            clean.startsWith("0") -> "+62" + clean.substring(1)
            clean.startsWith("62") -> "+$clean"
            clean.startsWith("+62") -> clean
            else -> clean
        }

        if (formatted.length < 9) {
            Toast.makeText(requireContext(), "Nomor WhatsApp belum terdaftar di profil", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val url = "https://api.whatsapp.com/send?phone=$formatted"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun konfirmasiKeluarAkun() {
        SessionManager.konfirmasiLogout(
            requireActivity(),
            "Apakah Anda yakin ingin keluar dari akun Portal SMK Riyadhul Jannah?"
        )
    }
}
