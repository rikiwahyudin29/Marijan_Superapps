package com.rtekmidev.marijansuperapps.api

data class AbsenHarianResponse(
    val status: Boolean,
    val message: String?,
    val data: AbsenHarianData?
)

data class AbsenHarianData(
    val kelas_id: Int?,
    val nama_kelas: String?,
    val wali_kelas: String?,
    val tahun_ajaran: String?,
    val tanggal: String?,
    val tanggal_formatted: String?,
    val is_libur: Boolean? = false,
    val keterangan_libur: String? = null,
    val summary: AbsenSummary?,
    val siswa: List<SiswaAbsenItem>?
)

data class AbsenSummary(
    val total: Int,
    val hadir: Int,
    val sakit: Int,
    val izin: Int,
    val alpha: Int
)

data class SiswaAbsenItem(
    val id: Int,
    val nomor_absen: Int,
    val nis: String?,
    val nisn: String? = null,
    val nama_lengkap: String,
    val jenis_kelamin: String?,
    var status_kehadiran: String, // "Hadir", "Sakit", "Izin", "Alpha"
    var keterangan: String?,
    val jam_masuk: String?,
    val sudah_diabsen: Boolean?
)

data class SimpanAbsenHarianRequest(
    val id_user: String,
    val tanggal: String,
    val absensi: List<SiswaAbsenSubmitItem>
)

data class SiswaAbsenSubmitItem(
    val siswa_id: Int,
    val status: String,
    val keterangan: String? = null
)
