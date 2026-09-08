@file:Suppress("unused")
package com.rtekmidev.marijancbt.api

// --- MODEL UNTUK LOGIN ---
data class LoginResponse(
    val status: com.google.gson.JsonElement,
    val message: String,
    val data: DataLogin?
)

data class DataLogin(
    val id_user: String?,
    val username: String?,
    val nama_lengkap: String?,
    val role: String?,
    val token: String?,
    val detail_siswa: DetailSiswa?,
    val detail_guru: DetailGuru?
)

data class DetailSiswa(
    val id: String?,
    val nisn: String?,
    val nama_lengkap: String?,
    val kelas_id: String?
)

data class DetailGuru(
    val id: String?,
    val nama_guru: String?,
    val foto: String?
)

// --- MODEL UNTUK JADWAL UJIAN ---
data class JadwalResponse(
    val status: Boolean,
    val message: String,
    val data: List<JadwalUjian>
)

data class JadwalUjian(
    val id_ujian_siswa: String,
    val status_pengerjaan: String,
    val nilai_pg: String?,
    val nilai_esai: String?,
    val id_jadwal: String,
    val waktu_mulai: String,
    val waktu_berakhir: String?,
    val waktu_selesai: String?,
    val durasi: String,
    val token: String,
    val setting_token: String,
    val judul_ujian: String,
    val nama_mapel: String?,
    val pengawas: String?
)

// --- MODEL UNTUK DOWNLOAD SOAL ---
data class DownloadResponse(
    val status: Boolean,
    val message: String,
    val id_ujian_siswa: String,
    val durasi: String,
    val min_finish: String?,
    val data_soal: List<Soal>
)

data class Soal(
    val id_soal: String,
    val jenis_soal: String,
    val teks_soal: String,
    val opsi: List<Opsi>,
    val couple: List<Couple>?
)

data class Opsi(
    val id_opsi: String,
    val teks_opsi: String
)

data class Couple(
    val id_couple: String,
    val teks_couple: String
)

// --- MODEL UNTUK SUBMIT JAWABAN ---
data class SubmitRequest(
    val id_ujian_siswa: String,
    val data_jawaban: List<JawabanSiswa>
)

data class JawabanSiswa(
    val soal_id: String,
    val jenis_soal: String,
    val jawaban: String
)

data class SubmitResponse(
    val status: Boolean,
    val message: String
)
// --- DATA CLASS UNTUK DASHBOARD SUPERAPP ---
data class DashboardResponse(
    val status: Boolean,
    val message: String?,
    val data: DashboardData?
)

data class DashboardData(
    val nisn: String?,
    val nis: String?,
    val nama: String?,
    val kelas: String?,
    val tingkat: String?,
    val nama_jurusan: String?,
    val jurusan_singkat: String?,
    val wali_kelas: String?,
    val status_siswa: String?,
    val tahun_ajaran: String?,
    val semester: String?,
    val foto_profil: String?,
    val tanggal_hari_ini: String?,
    val hari_ini_nama: String?,
    val pekan_kbm_current: Int?,
    val pekan_kbm_total: Int?,
    val pekan_kbm_text: String?,
    val pekan_kbm_persen: Int?,
    val pekan_kbm_persen_text: String?,
    val keuangan: Any?,
    val tugas_aktif: Int?,
    val tugas_urgent_count: Int?,
    val poin_disiplin: Int?,
    val poin_disiplin_max: Int?,
    val predikat_disiplin: String?,
    val kehadiran_persen: Double?,
    val kehadiran_sub: String?,
    val hadir_count: Int?,
    val izin_count: Int?,
    val tugas_tertunda_count: Int?,
    val tugas_tertunda_sub: String?,
    val ujian_cbt_count: Int?,
    val ujian_cbt_sub: String?,
    val total_tagihan: Any?,
    val total_tagihan_formatted: String?,
    val tagihan_sub: String?,
    val has_active_exam: Boolean?,
    val active_exam: ActiveExamDetail?,
    val ujian_hari_ini: com.google.gson.JsonElement?,
    val has_active_kbm: Boolean?,
    val active_kbm: ActiveKbmDetail?,
    val next_kbm: ActiveKbmDetail?,
    val jadwal_hari_ini: List<JadwalPelajaranDetail>?,
    val presensi_sekolah: PresensiHariIniDetail?,
    val tugas_preview: List<TugasPreviewItem>?,
    val materi_preview: List<MateriPreviewItem>?,
    val tagihan_preview: List<TagihanItemPreview>?,
    val rata_rata_nilai: Double?,
    val rata_rata_badge: String?,
    val pesan_baru: Int?,
    val total_materi: Int?,
    val materi_baru_count: Int?,
    val aktivitas_terkini: List<AktivitasTerkini>?
)

data class ActiveExamDetail(
    val id_ujian: Any?,
    val nama_mapel: String?,
    val waktu: String?,
    val ruang: String?
)

data class ActiveKbmDetail(
    val nama_mapel: String?,
    val jam_ke: Int?,
    val jam_mulai: String?,
    val jam_selesai: String?,
    val waktu: String?,
    val guru: String?,
    val ruang: String?,
    val is_active: Boolean?
)

data class JadwalPelajaranDetail(
    val nama_mapel: String?,
    val jam_ke: Int?,
    val jam_mulai: String?,
    val jam_selesai: String?,
    val waktu: String?,
    val guru: String?,
    val ruang: String?,
    val is_active: Boolean?
)

data class PresensiHariIniDetail(
    val lokasi_sekolah: String?,
    val radius_info: String?,
    val jam_masuk: String?,
    val jam_masuk_status: String?,
    val jam_pulang: String?,
    val jam_pulang_status: String?,
    val is_hadir: Boolean?,
    val is_pulang: Boolean?,
    val jadwal_masuk: String? = null,
    val jadwal_pulang: String? = null
)

data class TugasPreviewItem(
    val id: Any?,
    val judul: String?,
    val mapel: String?,
    val guru: String?,
    val deadline: String?,
    val deadline_label: String? = null,
    val is_urgent: Boolean? = null,
    val jurusan_badge: String? = null,
    val is_selesai: Boolean?,
    val status: String?
)

data class MateriPreviewItem(
    val id: Any?,
    val judul: String?,
    val mapel: String?,
    val guru: String?,
    val guru_singkat: String?,
    val file_type: String?,
    val file_size: String?,
    val waktu: String?,
    val info_sub: String?,
    val download_url: String?
)

data class TagihanItemPreview(
    val id: Any?,
    val nama_pos: String?,
    val sisa_nominal: Double?,
    val nominal_formatted: String?,
    val status: String?,
    val jatuh_tempo: String?
)

data class AktivitasTerkini(
    val judul: String?,
    val waktu: String?,
    val status: String?,
    val icon: String?
)

data class UjianHariIni(
    val id_ujian: String?,
    val nama_mapel: String?,
    val waktu: String?,
    val ruang: String?
)

data class JadwalPelajaran(
    val nama_mapel: String?,
    val waktu: String?,
    val guru: String?,
    val ruang: String?
)

data class JadwalPelajaranResponse(
    val status: Boolean,
    val message: String?,
    val data: JadwalPelajaranData?
)

data class JadwalPelajaranData(
    val kelas: String?,
    val jurusan: String?,
    val wali_kelas: String?,
    val hari_ini: String?,
    val jadwal: Map<String, List<JadwalPelajaranItem>>?
)

data class JadwalPelajaranItem(
    val nama_mapel: String?,
    val nama_guru: String?,
    val jam_mulai: String?,
    val jam_selesai: String?,
    val waktu: String?,
    val ruang: String?,
    val is_active: Boolean? = false
)

// --- DATA CLASS PRESENSI ---
data class PresensiRiwayatResponse(
    val status: Boolean,
    val data: List<RiwayatData>?
)

data class RiwayatData(
    val tanggal: String,
    val jam_masuk: String?,
    val jam_pulang: String?,
    val status_kehadiran: String
)

data class SubmitAbsenResponse(
    val status: Boolean,
    val message: String
)

// --- DATA CLASS UNTUK REKAP & IZIN ---
data class RekapAbsenResponse(
    val status: Boolean,
    val bulan: String?,
    val summary: SummaryAbsen?,
    val data: List<DataRekap>?
)

data class SummaryAbsen(
    val hadir: Int, val sakit: Int, val izin: Int, val alpha: Int, val terlambat: Int, val cuti: Int?, val dinas_luar: Int?
)

data class DataRekap(
    val tanggal: String, val status_kehadiran: String, val jam_masuk: String?, val jam_pulang: String?
)

data class SubmitIzinResponse(
    val status: Boolean, val message: String
)
data class SettingResponse(
    val status: com.google.gson.JsonElement,
    val data: SettingData?
)

data class SettingData(
    val latitude: String,
    val longitude: String,
    val radius: Int,
    val qr_token: String
)

// --- MODUL KEUANGAN ---
data class TagihanResponse(
    val status: Boolean,
    val message: String?,
    val tagihan: List<DataTagihan>?,
    val riwayat: List<DataRiwayat>? // Ã°Å¸â€Â¥ Tambahkan Penangkap Riwayat
)

data class DataTagihan(
    val id: String?,
    val nama_pos: String?,
    val nominal_tagihan: String?,
    val nominal_terbayar: String?,
    val status_bayar: String?
)

// Ã°Å¸â€Â¥ Data Class Baru Untuk Riwayat (Sesuai output KeuanganApi.php)
data class DataRiwayat(
    val id: String?,
    val nama_pos: String?,
    val keterangan: String?,
    val total_bayar: String?,
    val payment_type: String?,
    val status_transaksi: String?,
    val created_at: String?,
    val checkout_url: String?
)

data class TripayCheckoutResponse(
    val status: Boolean,
    val checkout_url: String?,
    val message: String
)

// --- MODUL MATERI & TUGAS ---
data class MateriResponse(
    val status: Boolean,
    val data: com.google.gson.JsonElement?
)

data class DataMateri(
    val id_materi: String?,
    val id: String?,
    val judul: String?,
    val judul_materi: String?,
    val nama_mapel: String?,
    val mapel: String?,
    val nama_guru: String?,
    val guru: String?,
    val tanggal: String?,
    val created_at: String?,
    val file_materi: String?,
    val file: String?,
    val deskripsi: String?,
    val status: String?,
    val jenis_file: String?,
    val link_youtube: String?,
    val ukuran_file: String?
)

data class TugasResponse(
    val status: Boolean,
    val data: TugasDataList?
)

data class TugasDataList(
    val berlangsung: List<DataTugas>?,
    val selesai: List<DataTugas>?
)

data class DataTugas(
    val id_tugas: Int?,
    val mapel: String?,
    val judul: String?,
    val deskripsi: String?,
    val file_pendukung: String?,
    val deadline: String?,
    val status: String?,
    val nilai: String?,
    val komentar_guru: String?,
    val file_jawaban: String?
)

data class SubmitTugasResponse(
    val status: Boolean,
    val message: String
)

// --- MODEL STATUS ABSEN GURU ---
data class PresensiStatusResponse(
    val status: Boolean,
    val lokasi_sekolah: SettingData?,
    val data_absen: RiwayatData?
)
data class CekWaktuResponse(
    val status: Boolean,
    val sisa_waktu_milis: Long,
    val status_pengerjaan: Int,
    val is_unlocked: Boolean? // Ã°Å¸â€Â¥ WAJIB DITAMBAHKAN
)


data class RaportResponse(
    val status: Boolean,
    val pesan: String?,
    val data: RaportData?
)

data class RaportData(
    val info_akademik: InfoAkademik?,
    val ringkasan: RingkasanRaport?,
    val daftar_nilai: List<NilaiMapel>?
)

data class InfoAkademik(
    val semester_tahun: String?,
    val deskripsi_kelas: String?
)

data class RingkasanRaport(
    val rata_rata_nilai: Any?,
    val trend_nilai: String?,
    val kehadiran_persen: Any?,
    val kehadiran_status: String?,
    val peringkat_kelas: Any?,
    val total_siswa: Any?,
    val peringkat_paralel: Any?,
    val url_download_pdf: String?
)

data class NilaiMapel(
    val mapel: String?,
    val guru: String?,
    val kkm: Any?,
    val formatif: Any?,
    val sumatif: Any?,
    val nilai_akhir: Any?,
    val deskripsi: String?
)
// --- DATA CLASS PRESENSI STATISTIK ---
data class PresensiStatistikResponse(
    val status: com.google.gson.JsonElement,
    val data: StatistikData?
)

data class StatistikData(
    val total_percentage: Int,
    val hadir: Int,
    val alfa: Int,
    val sakit: Int,
    val terlambat: Int
)

// --- MODEL UNTUK AKADEMIK GURU DASHBOARD ---
data class DashboardGuruResponse(
    val status: Boolean,
    val data: DashboardGuruData?
)

data class DashboardGuruData(
    val total_jam_minggu_ini: Int,
    val target_jam: Int? = 24,
    val persen_target_jam: Int? = 100,
    val badge_jjm: String? = "Sesuai JJM",
    val total_rombel: Int? = 0,
    val rombel_summary: String? = null,
    val total_siswa_diajar: Int? = 0,
    val status_siswa_diajar: String? = "Semua Aktif",
    val status_kbm: String? = "Aktif Mengajar",
    val minggu_efektif_ke: Int? = 7,
    val total_minggu_efektif: Int? = 18,
    val progress_minggu: Int? = 39,
    val kbm_aktif: KbmAktifInfo? = null,
    val kurikulum_text: String? = "Kurikulum Merdeka SMK PK",
    val siswa_belum_absen: Int,
    val is_libur: Boolean? = false,
    val keterangan_libur: String? = null,
    val jadwal_hari_ini: List<JadwalGuruHariIni>?,
    val mata_pelajaran_diampu: List<MataPelajaranDiampu>?,
    val wali_kelas_info: WaliKelasInfo?,
    val tahun_ajaran_aktif: String? = null,
    val tanggal_hari_ini_formatted: String? = null,
    val guru_info: GuruInfo? = null,
    val presensi_guru_hari_ini: PresensiGuruHariIni? = null,
    val presensi_binaan_summary: PresensiBinaanSummary? = null,
    val progres_jurnal_bulan_ini: ProgresJurnal? = null,
    val keuangan_kelas_summary: KeuanganKelasSummary? = null
)

data class KbmAktifInfo(
    val is_active: Boolean = false,
    val id_jadwal: Int? = null,
    val id_kelas: String? = null,
    val id_mapel: String? = null,
    val id_jurnal: Int? = null,
    val nama_mapel: String? = null,
    val nama_kelas: String? = null,
    val ruang: String? = null,
    val jam_ke: String? = null,
    val is_jurnal_filled: Boolean = false,
    val materi: String? = null,
    val status_badge: String? = "Sedang Berlangsung"
)

data class PresensiGuruHariIni(
    val shift_nama: String? = null,
    val shift_jam: String? = null,
    val jam_pulang_mulai_badge: String? = null,
    val is_libur: Boolean = false,
    val keterangan_libur: String? = null,
    val is_sudah_masuk: Boolean = false,
    val is_sudah_pulang: Boolean = false,
    val jam_masuk: String? = null,
    val jam_pulang: String? = null,
    val status_masuk_badge: String? = null,
    val status_masuk_sub: String? = null,
    val status_pulang_badge: String? = null,
    val status_pulang_sub: String? = null,
    val school_lat: Double = 0.0,
    val school_lng: Double = 0.0,
    val school_radius: Int = 100,
    val stat_hadir: String? = null,
    val stat_terlambat: String? = null,
    val stat_izin_sakit: String? = null,
    val stat_kehadiran: String? = null
)

data class GuruInfo(
    val nama: String? = null,
    val nip: String? = null,
    val mapel_utama: String? = null,
    val is_wali_kelas: Boolean? = false,
    val kelas_wali: String? = null
)

data class PresensiBinaanSummary(
    val total_siswa: Int = 0,
    val hadir: Int = 0,
    val sakit: Int = 0,
    val izin: Int = 0,
    val alpha: Int = 0,
    val catatan_izin: String? = null,
    val is_libur: Boolean = false,
    val keterangan_libur: String? = null
)

data class ProgresJurnal(
    val persentase: Int = 0,
    val sesi_terisi: Int = 0,
    val total_sesi: Int = 24,
    val sesi_menanti: Int = 0
)

data class KeuanganKelasSummary(
    val total_tagihan: Long = 0,
    val total_terbayar: Long = 0,
    val sisa_tunggakan: Long = 0,
    val siswa_belum_lunas: Int = 0
)

data class MataPelajaranDiampu(
    val id_mapel: Any? = null,
    val nama_mapel: String?,
    val nama_kelas: String?,
    val jp_per_minggu: Int?,
    val modul_info: String? = null,
    val progress_persen: Int? = 0
)

data class WaliKelasInfo(
    val nama_kelas: String?,
    val total_siswa: Int?,
    val siswa_belum_absen: Int?,
    val is_libur: Boolean? = false,
    val keterangan_libur: String? = null
)

data class JadwalGuruHariIni(
    val id: String,
    val id_kelas: String?,
    val id_mapel: String?,
    val jam_mulai: String?,
    val jam_selesai: String?,
    val jam_ke: String?,
    val nama_kelas: String?,
    val nama_mapel: String?,
    val is_jurnal_filled: Boolean?,
    val jurnal_materi: String?,
    val jurnal_foto: String?,
    val jurnal_presensi: String?
)

// --- MODEL UNTUK JURNAL DAN PRESENSI GURU ---
data class SubmitJurnalResponse(
    val status: Boolean,
    val message: String,
    val data: JurnalData?
)

data class JurnalData(
    val id_jurnal: Int
)

data class GetSiswaJurnalResponse(
    val status: Boolean,
    val data: List<SiswaJurnal>
)

data class SiswaJurnal(
    val id: Int,
    val nis: String?,
    val nisn: String? = null,
    val nama_lengkap: String?,
    val jenis_kelamin: String?,
    var status_absen: String?
)

data class SubmitAbsenJurnalRequest(
    val id_jurnal: Int,
    val data_absen: List<DataAbsenItem>
)

data class DataAbsenItem(
    val id_siswa: Int,
    val status: String
)

data class SimpleResponse(
    val status: Boolean,
    val message: String
)

data class JurnalInfoResponse(
    val status: Boolean,
    val data: JurnalInfoData?
)

data class JurnalInfoData(
    val total_siswa: Int,
    val pertemuan_ke: Int,
    val jam_ke: String?,
    val hadir: Int,
    val izin: Int,
    val sakit: Int,
    val alpha: Int
)

data class RekapJurnalResponse(
    val status: Boolean,
    val data: RekapJurnalData?
)

data class RekapJurnalData(
    val semester_info: String?,
    val summary: RekapJurnalSummary?,
    val list: List<RekapJurnalItem>?
)

data class RekapJurnalSummary(
    val terisi: Int,
    val perlu_diisi: Int,
    val total_sesi: Int,
    val rata_presensi: Double
)

data class RekapJurnalItem(
    val tanggal: String?,
    val nama_hari: String?,
    val jam_mulai: String?,
    val jam_selesai: String?,
    val jam_ke: String?,
    val nama_kelas: String?,
    val nama_mapel: String?,
    val id_kelas: String?,
    val id_mapel: String?,
    val id_jurnal: Int?,
    val status: String?,
    val materi: String?,
    val presensi_summary: String?,
    val foto_kegiatan: String? = null,
    val total_alfa: Int? = 0,
    val siswa_alfa: List<String>? = null,
    val alfa_names: String? = null
)

// --- MODEL UNTUK JADWAL MENGAJAR GURU LENGKAP ---
data class JadwalMengajarResponse(
    val status: Boolean,
    val message: String?,
    val data: JadwalMengajarData?
)

data class JadwalMengajarData(
    val guru: GuruJadwalInfo?,
    val stats: JadwalStats?,
    val hari_list: List<HariJadwalData>?,
    val semua_jadwal: List<com.google.gson.JsonElement>?,
    val legenda_list: List<LegendaItemModel>?,
    val matriks_rows: List<MatriksRowModel>?
)

data class LegendaItemModel(
    val type: String?,
    val title: String?,
    val badge: String?,
    val color_hex: String?,
    val bg_hex: String?,
    val border_hex: String?,
    val text_hex: String?
)

data class MatriksRowModel(
    val urutan: Int?,
    val nama_jam: String?,
    val waktu: String?,
    val is_istirahat: Boolean?,
    val cells: Map<String, MatriksCellModel>?
)

data class MatriksCellModel(
    val type: String?, // "pelajaran", "istirahat", "empty"
    val id_jadwal: Int?,
    val id_kelas: String?,
    val id_mapel: String?,
    val nama_mapel: String?,
    val nama_kelas: String?,
    val jp: Int?,
    val position: String?, // "single", "start", "middle", "end"
    val is_start: Boolean?,
    val color_hex: String?,
    val bg_hex: String?,
    val border_hex: String?,
    val text_hex: String?,
    val text: String?,
    val nama_jam: String?
)

data class GuruJadwalInfo(
    val id: Int?,
    val nama_lengkap: String?,
    val nik: String?,
    val nip: String?,
    val foto: String?,
    val role: String?,
    val jabatan: String?,
    val semester_info: String?,
    val nama_sekolah: String?
)

data class JadwalStats(
    val total_jam_minggu: Int?,
    val total_siswa: Int?,
    val total_rombel: Int?,
    val total_mapel: Int?,
    val kelas_ringkasan: String?
)

data class HariJadwalData(
    val nama_hari: String?,
    val singkatan: String?,
    val total_jam: Int?,
    val is_libur: Boolean?,
    val is_hari_ini: Boolean?,
    val items: List<JadwalItemModel>?
)

data class JadwalItemModel(
    val type: String?, // "pelajaran" atau "istirahat"
    val id_jadwal: Int?,
    val id_kelas: String?,
    val id_mapel: String?,
    val nama_mapel: String?,
    val nama_kelas: String?,
    val jam_mulai: String?,
    val jam_selesai: String?,
    val jam_ke: String?,
    val durasi_jp: Int?,
    val ruang: String?,
    val kategori: String?,
    val is_jurnal_filled: Boolean?,
    val id_jurnal: Int?
)

// --- PORTAL PRESENSI GURU (RESPONS REAL-TIME) ---
data class PortalPresensiGuruResponse(
    val status: Boolean = false,
    val message: String? = null,
    val guru_info: PortalGuruInfo? = null,
    val shift_info: PortalShiftInfo? = null,
    val lokasi_sekolah: PortalLokasiSekolah? = null,
    val presensi_hari_ini: PortalPresensiHariIni? = null,
    val statistik_kehadiran: PortalStatistikKehadiran? = null,
    val riwayat_terakhir: List<PortalRiwayatItem>? = null,
    val unduh_rekap_url: String? = null
)

data class PortalGuruInfo(
    val id_guru: Any? = null,
    val nama: String? = null,
    val nip: String? = null,
    val mapel: String? = null,
    val is_wali_kelas: Boolean = false,
    val nama_kelas_wali: String? = null,
    val tahun_ajaran: String? = null,
    val tanggal_hari_ini: String? = null
)

data class PortalShiftInfo(
    val nama: String? = null,
    val jam_kerja: String? = null,
    val jam_masuk_mulai: String? = null,
    val jam_masuk_selesai: String? = null,
    val jam_pulang_mulai: String? = null,
    val jam_pulang_selesai: String? = null
)

data class PortalLokasiSekolah(
    val nama_sekolah: String? = null,
    val alamat: String? = null,
    val latitude: Double = -6.5714,
    val longitude: Double = 107.7587,
    val radius: Int = 200
)

data class PortalPresensiHariIni(
    val is_libur: Boolean = false,
    val keterangan_libur: String? = null,
    val sudah_masuk: Boolean = false,
    val jam_masuk: String? = null,
    val status_masuk_badge: String? = null,
    val status_masuk_sub: String? = null,
    val sudah_pulang: Boolean = false,
    val jam_pulang: String? = null,
    val status_pulang_badge: String? = null,
    val status_pulang_sub: String? = null,
    val max_radius_text: String? = null
)

data class PortalStatistikKehadiran(
    val periode_text: String? = null,
    val label_performa: String? = null,
    val persentase: Int = 0,
    val total_hari_kerja: Int = 0,
    val total_hadir_kerja: Int = 0,
    val hari_kerja_text: String? = null,
    val hadir: Int = 0,
    val terlambat: Int = 0,
    val izin_sakit: Int = 0,
    val alfa: Int = 0
)

data class PortalRiwayatItem(
    val tanggal: String? = null,
    val hari_tanggal: String? = null,
    val jam_text: String? = null,
    val status_badge: String? = null,
    val status_tipe: String? = null
)

// --- PROFIL GURU MODELS ---
data class ProfilGuruResponse(
    val status: Boolean,
    val message: String? = null,
    val data: ProfilGuruData? = null
)

data class ProfilGuruData(
    val hero: ProfilGuruHero? = null,
    val metrics: ProfilGuruMetrics? = null,
    val informasi_personal: ProfilInformasiPersonal? = null,
    val kepegawaian: ProfilKepegawaian? = null,
    val administrasi_wali_kelas: ProfilAdministrasiWaliKelas? = null,
    val keamanan: ProfilKeamanan? = null,
    val app_info: ProfilAppInfo? = null
)

data class ProfilGuruHero(
    val nama: String? = null,
    val nip: String? = null,
    val nuptk: String? = null,
    val foto_url: String? = null,
    val status_kepegawaian_badge: String? = null,
    val is_2fa_active: Boolean = true,
    val mapel_badge: String? = null,
    val wali_kelas_badge: String? = null
)

data class ProfilGuruMetrics(
    val beban_mengajar: MetricItem? = null,
    val presensi_bulan_ini: MetricItem? = null,
    val siswa_binaan: MetricItem? = null
)

data class MetricItem(
    val value: String? = null,
    val title: String? = null,
    val badge: String? = null
)

data class ProfilInformasiPersonal(
    val no_whatsapp: String? = null,
    val email: String? = null,
    val tempat_tgl_lahir: String? = null,
    val jenis_kelamin: String? = null,
    val alamat_lengkap: String? = null
)

data class ProfilKepegawaian(
    val status_kepegawaian: String? = null,
    val status_badge: String? = null,
    val pendidikan_terakhir: String? = null,
    val sertifikasi: String? = null,
    val sk_beban_mengajar: ProfilSkMengajar? = null
)

data class ProfilSkMengajar(
    val judul: String? = null,
    val keterangan: String? = null,
    val download_url: String? = null
)

data class ProfilAdministrasiWaliKelas(
    val is_wali_kelas: Boolean = false,
    val nama_kelas: String? = null,
    val jurusan: String? = null,
    val total_siswa: Int = 0,
    val status_semester: String? = null
)

data class ProfilKeamanan(
    val terakhir_ganti_password: String? = null,
    val two_factor_auth: String? = null,
    val device_name: String? = null,
    val device_status: String? = null
)

data class ProfilAppInfo(
    val app_name: String? = null,
    val portal_desc: String? = null
)


