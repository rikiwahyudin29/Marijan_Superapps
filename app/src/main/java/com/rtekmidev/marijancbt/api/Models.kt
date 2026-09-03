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
    val nama: String?,
    val kelas: String?,
    val tahun_ajaran: String?,
    val semester: String?,
    val foto_profil: String?,
    val keuangan: Any?,
    val tugas_aktif: Int,
    val poin_disiplin: Int,
    val ujian_hari_ini: com.google.gson.JsonElement?,
    val jadwal_hari_ini: List<JadwalPelajaran>?,
    val rata_rata_nilai: Double?,
    val pesan_baru: Int?,
    val total_materi: Int?,
    val aktivitas_terkini: List<AktivitasTerkini>?
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
    val rata_rata_nilai: String?,
    val trend_nilai: String?,
    val kehadiran_persen: Int?,
    val kehadiran_status: String?,
    val peringkat_kelas: Int?,
    val total_siswa: Int?,
    val peringkat_paralel: Int?,
    val url_download_pdf: String?
)

data class NilaiMapel(
    val mapel: String?,
    val guru: String?,
    val kkm: String?,
    val formatif: String?,
    val sumatif: String?,
    val nilai_akhir: String?,
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
    val siswa_belum_absen: Int,
    val jadwal_hari_ini: List<JadwalGuruHariIni>?,
    val mata_pelajaran_diampu: List<MataPelajaranDiampu>?,
    val wali_kelas_info: WaliKelasInfo?
)

data class MataPelajaranDiampu(
    val nama_mapel: String?,
    val nama_kelas: String?,
    val jp_per_minggu: Int?
)

data class WaliKelasInfo(
    val nama_kelas: String?,
    val total_siswa: Int?,
    val siswa_belum_absen: Int?
)

data class JadwalGuruHariIni(
    val id: String,
    val id_kelas: String?,
    val id_mapel: String?,
    val jam_mulai: String?,
    val jam_selesai: String?,
    val jam_ke: String?,
    val nama_kelas: String?,
    val nama_mapel: String?
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
    val nama_lengkap: String?,
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
