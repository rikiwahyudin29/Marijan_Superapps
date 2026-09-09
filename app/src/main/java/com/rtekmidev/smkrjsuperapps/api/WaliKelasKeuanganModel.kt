package com.rtekmidev.smkrjsuperapps.api

data class WaliKelasKeuanganResponse(
    val status: Boolean,
    val message: String? = null,
    val kelas: String? = null,
    val wali_kelas: String? = null,
    val tahun_ajaran: String? = null,
    val summary: KeuanganSummary? = null,
    val pos_list: List<KeuanganPosItem>? = null,
    val filter_pos: Int? = null,
    val data: List<KeuanganSiswaItem>? = null,
    val cetak_rekap_url: String? = null,
    val export_excel_url: String? = null,
    val cetak_tagihan_base_url: String? = null
)

data class KeuanganSummary(
    val total_tagihan: Long = 0,
    val total_terbayar: Long = 0,
    val sisa_tunggakan: Long = 0,
    val persentase_selesai: String = "0%",
    val total_siswa: Int = 0,
    val total_lunas: Int = 0,
    val total_belum_lunas: Int = 0
)

data class KeuanganPosItem(
    val id: Int,
    val pos_id: Int? = null,
    val nama_pos: String,
    val tahun_ajaran: String? = null,
    val semester: String? = null
)

data class KeuanganSiswaItem(
    val siswa_id: Int,
    val nomor_absen: Int = 0,
    val nama_siswa: String,
    val nis: String? = null,
    val nisn: String? = null,
    val jenis_kelamin: String? = "Laki-laki",
    val no_hp_ortu: String? = null,
    val no_hp_siswa: String? = null,
    val total_tagihan: Long = 0,
    val total_terbayar: Long = 0,
    val sisa_tunggakan: Long = 0,
    val status: String = "Belum Lunas",
    val total_pos: Int = 0,
    val rincian: List<KeuanganRincianPosItem>? = null
)

data class KeuanganRincianPosItem(
    val tagihan_id: Int,
    val pos_id: Int,
    val jenis_bayar_id: Int? = null,
    val nama_pos: String,
    val tahun_ajaran: String? = null,
    val semester: String? = null,
    val tahun_ajaran_lengkap: String? = null,
    val tipe_bayar: String? = "BEBAS",
    val keterangan: String? = "",
    val bulan_ke: Int? = null,
    val nominal_tagihan: Long = 0,
    val nominal_terbayar: Long = 0,
    val sisa: Long = 0,
    val status: String = "BELUM LUNAS"
)
