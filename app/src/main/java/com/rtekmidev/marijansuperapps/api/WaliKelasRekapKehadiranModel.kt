package com.rtekmidev.marijansuperapps.api

data class WaliKelasRekapKehadiranResponse(
    val status: Boolean,
    val message: String? = null,
    val kelas: String? = null,
    val wali_kelas: String? = null,
    val tahun_ajaran: String? = null,
    val bulan: String? = null,
    val bulan_formatted: String? = null,
    val total_libur_hari: Int? = 0,
    val info_libur: List<String>? = null,
    val summary: RekapKehadiranSummary? = null,
    val dates: List<RekapKehadiranDateItem>? = null,
    val data: List<RekapKehadiranSiswaItem>? = null,
    val cetak_matrix_url: String? = null,
    val export_excel_url: String? = null,
    val cetak_siswa_base_url: String? = null
)

data class RekapKehadiranSummary(
    val total_hadir: Int = 0,
    val total_sakit: Int = 0,
    val total_izin: Int = 0,
    val total_alpha: Int = 0,
    val persentase_hadir: Double = 0.0
)

data class RekapKehadiranDateItem(
    val tanggal: String,
    val tgl_num: String,
    val hari_short: String,
    val is_libur: Boolean = false,
    val keterangan_libur: String? = null
)

data class RekapKehadiranSiswaItem(
    val siswa_id: Int,
    val nomor_absen: Int = 0,
    val nama_siswa: String,
    val nis: String? = null,
    val nisn: String? = null,
    val jenis_kelamin: String? = "Laki-laki",
    val hadir: Int = 0,
    val sakit: Int = 0,
    val izin: Int = 0,
    val alpha: Int = 0,
    val is_perhatian: Boolean = false,
    val harian: Map<String, String>? = null
)
