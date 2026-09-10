package com.rtekmidev.smkrjsuperapps.api

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    // 1. Endpoint Login (🔥 FIXED: Pakai "username" bukan "nisn")
    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("device_id") deviceId: String,
        @Field("device_name") deviceName: String,
        @Field("latitude") latitude: String = "0.0",
        @Field("longitude") longitude: String = "0.0",
        @Field("otp_code") otpCode: String? = null,
        @Field("fcm_token") fcmToken: String? = null
    ): Response<LoginResponse>

    // 1b. Endpoint Register/Update Token FCM Mobile
    @FormUrlEncoded
    @POST("api/device/register-fcm")
    suspend fun registerFcmToken(
        @Field("fcm_token") fcmToken: String,
        @Field("device_id") deviceId: String? = null,
        @Field("device_name") deviceName: String? = null,
        @Field("latitude") latitude: String? = null,
        @Field("longitude") longitude: String? = null
    ): Response<JsonElement>

    // 1c. Endpoint Update Lokasi Perangkat Berkala / On Demand
    @FormUrlEncoded
    @POST("api/device/update-location")
    suspend fun updateLocation(
        @Field("device_id") deviceId: String,
        @Field("latitude") latitude: String,
        @Field("longitude") longitude: String,
        @Field("is_mock") isMock: Boolean = false
    ): Response<JsonElement>

    // 2. Endpoint Ambil Jadwal
    @GET("api/ujian/jadwal")
    suspend fun getJadwal(
        @Query("nisn") nisn: String
    ): Response<JadwalResponse>

    // 3. Endpoint Download Soal (Kirim Raw JSON)
    @POST("api/ujian/download")
    suspend fun downloadSoal(
        @Body requestBody: Map<String, String>
    ): Response<DownloadResponse>

    // 4. Endpoint Submit Jawaban Akhir (Kirim Raw JSON)
    @POST("api/ujian/submit")
    suspend fun submitJawaban(
        @Body requestBody: SubmitRequest
    ): Response<SubmitResponse>

    @GET("api/akademik/dashboard")
    suspend fun getDashboard(@Query("nisn") nisn: String): Response<DashboardResponse>

    @GET("api/akademik/jadwal")
    suspend fun getJadwalPelajaran(@Query("nisn") nisn: String): Response<JadwalPelajaranResponse>

    @GET("api/presensi/portal-dashboard")
    suspend fun getPresensiSiswaPortalDashboard(
        @Query("nisn") nisn: String
    ): Response<PortalPresensiSiswaResponse>

    @GET("api/presensi/riwayat")
    suspend fun getRiwayatAbsen(@Query("nisn") nisn: String): Response<JsonElement>

    @FormUrlEncoded
    @POST("api/presensi/submit")
    suspend fun submitAbsen(
        @Field("nisn") nisn: String,
        @Field("latitude") lat: String,
        @Field("longitude") lng: String,
        @Field("qr_token") qrToken: String // 🔥 GANTI JADI INI
    ): Response<SubmitAbsenResponse>

    @GET("api/presensi/rekap")
    suspend fun getRekapAbsen(
        @Query("nisn") nisn: String,
        @Query("bulan") bulan: String
    ): Response<RekapAbsenResponse>

    @FormUrlEncoded
    @POST("api/presensi/ajukan_izin")
    suspend fun ajukanIzin(
        @Field("nisn") nisn: String,
        @Field("tanggal") tanggal: String,
        @Field("status") status: String,
        @Field("keterangan") keterangan: String,
        @Field("file_bukti") fileBuktiBase64: String
    ): Response<SubmitIzinResponse>

    @GET("api/presensi/setting")
    suspend fun getSettingPresensi(): Response<SettingResponse>


    @GET("api/keuangan/tagihan")
    suspend fun getTagihan(@Query("nisn") nisn: String): Response<TagihanResponse>

    @FormUrlEncoded
    @POST("api/keuangan/bayarTripay")
    suspend fun bayarTagihan(
        @Field("nisn") nisn: String,
        @Field("id_tagihan") idTagihan: String,
        @Field("method") method: String,
        @Field("nominal_bayar") nominalBayar: Long? = null
    ): Response<TripayCheckoutResponse>

    @FormUrlEncoded
    @POST("api/keuangan/sanggahan")
    suspend fun submitSanggahan(
        @Field("nisn") nisn: String,
        @Field("id_tagihan") idTagihan: String,
        @Field("nominal") nominal: Long,
        @Field("keterangan") keterangan: String?,
        @Field("bukti_pembayaran") buktiBase64: String
    ): Response<SubmitSanggahanResponse>

    @GET("api/akademik/materi")
    suspend fun getMateri(@Query("nisn") nisn: String): Response<MateriResponse>

        @GET("api/akademik/tugas")
    suspend fun getTugas(@Query("nisn") nisn: String): Response<TugasResponse>

    @GET("api/akademik/raport")
    suspend fun getNilaiRaport(@Query("nisn") nisn: String): Response<RaportResponse>

    @Multipart
    @POST("api/akademik/submit_tugas")
    suspend fun submitTugas(
        @Part("nisn") nisn: RequestBody,
        @Part("tugas_id") tugasId: RequestBody,
        @Part("catatan_siswa") catatan: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<SubmitTugasResponse>

    // --- AKADEMIK GURU JURNAL & WALI KELAS ---
    @GET("api/akademik-guru/get-jurnal-info")
    suspend fun getJurnalInfo(
        @Query("id_kelas") idKelas: String,
        @Query("id_mapel") idMapel: String,
        @Query("tanggal") tanggal: String? = null
    ): retrofit2.Response<JurnalInfoResponse>

    // --- API KHUSUS GURU ---
    @FormUrlEncoded
    @POST("api/presensi-guru/submit")
    suspend fun submitAbsenGuru(
        @Field("id_user") idUser: String,
        @Field("latitude") lat: String,
        @Field("longitude") lon: String,
        @Field("qr_token") qrToken: String
    ): Response<SubmitAbsenResponse> // Menggunakan model yang sama dengan siswa

    // --- LAYANAN REKAP & IZIN GURU ---
    @GET("api/presensi-guru/portal-dashboard")
    suspend fun getPresensiGuruPortalDashboard(
        @Query("id_user") idUser: String
    ): Response<PortalPresensiGuruResponse>

    @GET("api/presensi-guru/rekap")
    suspend fun getRekapGuru(
        @Query("id_user") idUser: String,
        @Query("bulan") bulan: String
    ): Response<RekapAbsenResponse>

    @FormUrlEncoded
    @POST("api/presensi-guru/izin")
    suspend fun ajukanIzinGuru(
        @Field("id_user") idUser: String,
        @Field("tanggal") tanggal: String,
        @Field("status") status: String,
        @Field("keterangan") keterangan: String,
        @Field("bukti") base64Image: String
    ): Response<SubmitIzinResponse>

    @GET("api/ujian/cek_waktu")
    suspend fun cekWaktuUjian(@Query("id_ujian_siswa") id: String): Response<CekWaktuResponse>

    // --- API AKADEMIK GURU ---
    @GET("api/akademik-guru/dashboard")
    suspend fun getAkademikGuruDashboard(@Query("id_user") idUser: String): Response<DashboardGuruResponse>

    @FormUrlEncoded
    @POST("api/akademik-guru/jurnal")
    suspend fun submitJurnal(
        @Field("id_kelas") idKelas: String,
        @Field("id_mapel") idMapel: String,
        @Field("jam_ke") jamKe: String,
        @Field("materi") materi: String,
        @Field("keterangan") keterangan: String,
        @Field("foto_kegiatan") fotoKegiatan: String,
        @Field("tanggal") tanggal: String? = null
    ): Response<SubmitJurnalResponse>

    @GET("api/akademik-guru/get-siswa-jurnal")
    suspend fun getSiswaJurnal(@Query("id_jurnal") idJurnal: Int): Response<GetSiswaJurnalResponse>

    @POST("api/akademik-guru/submit-absen-jurnal")
    suspend fun submitAbsenJurnal(@Body request: SubmitAbsenJurnalRequest): Response<SimpleResponse>

    @GET("api/akademik-guru/rekap-jurnal")
    suspend fun getRekapJurnal(
        @Query("bulan") bulan: String
    ): Response<RekapJurnalResponse>

    @GET("api/walikelas/absen-harian")
    suspend fun getWaliKelasAbsenHarian(
        @Query("id_user") idUser: String,
        @Query("tanggal") tanggal: String? = null
    ): Response<AbsenHarianResponse>

    @POST("api/walikelas/absen-harian/simpan")
    suspend fun simpanWaliKelasAbsenHarian(
        @Body request: SimpanAbsenHarianRequest
    ): Response<SimpleResponse>

    @GET("api/walikelas/kehadiran")
    suspend fun getWaliKelasRekapKehadiran(
        @Query("id_user") idUser: String,
        @Query("bulan") bulan: String
    ): Response<WaliKelasRekapKehadiranResponse>

    @GET("api/walikelas/keuangan")
    suspend fun getWaliKelasRekapTagihan(
        @Query("id_user") idUser: String,
        @Query("pos") pos: Int? = null
    ): Response<WaliKelasKeuanganResponse>

    @GET("api/akademik-guru/jadwal")
    suspend fun getJadwalMengajar(
        @Query("id_user") idUser: String? = null
    ): Response<JadwalMengajarResponse>

    @GET("api/akademik-guru/profil")
    suspend fun getProfilGuru(
        @Query("id_user") idUser: String? = null
    ): Response<ProfilGuruResponse>

    @FormUrlEncoded
    @POST("api/akademik-guru/update-profil")
    suspend fun updateProfilGuru(
        @Field("id_user") idUser: String,
        @Field("no_whatsapp") noWhatsapp: String,
        @Field("email") email: String,
        @Field("alamat") alamat: String
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("api/akademik-guru/ganti-password")
    suspend fun gantiPasswordGuru(
        @Field("id_user") idUser: String,
        @Field("password_lama") passwordLama: String,
        @Field("password_baru") passwordBaru: String
    ): Response<SimpleResponse>
}
