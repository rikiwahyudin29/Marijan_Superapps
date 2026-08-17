package com.rtekmidev.marijancbt.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    // 1. Endpoint Login (ðŸ”¥ FIXED: Pakai "username" bukan "nisn")
    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("device_id") deviceId: String,
        @Field("device_name") deviceName: String
    ): Response<LoginResponse>

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
    suspend fun getDashboard(@Query("nisn") nisn: String): retrofit2.Response<DashboardResponse>

    @GET("api/presensi/riwayat")
    suspend fun getRiwayatAbsen(@Query("nisn") nisn: String): retrofit2.Response<com.google.gson.JsonElement>

    @FormUrlEncoded
    @POST("api/presensi/submit")
    suspend fun submitAbsen(
        @Field("nisn") nisn: String,
        @Field("latitude") lat: String,
        @Field("longitude") lng: String,
        @Field("qr_token") qrToken: String // ðŸ”¥ GANTI JADI INI
    ): retrofit2.Response<SubmitAbsenResponse>

    @GET("api/presensi/rekap")
    suspend fun getRekapAbsen(
        @Query("nisn") nisn: String,
        @Query("bulan") bulan: String
    ): retrofit2.Response<RekapAbsenResponse>

    @FormUrlEncoded
    @POST("api/presensi/izin")
    suspend fun ajukanIzin(
        @Field("nisn") nisn: String,
        @Field("tanggal") tanggal: String,
        @Field("status") status: String,
        @Field("keterangan") keterangan: String,
        @Field("file_bukti") fileBuktiBase64: String
    ): retrofit2.Response<SubmitIzinResponse>

    @GET("api/presensi/setting")
    suspend fun getSettingPresensi(): retrofit2.Response<SettingResponse>


    @GET("api/keuangan/tagihan")
    suspend fun getTagihan(@Query("nisn") nisn: String): retrofit2.Response<TagihanResponse>

    @FormUrlEncoded
    @POST("api/keuangan/bayarTripay") // Sesuaikan dengan route CodeIgniter bos!
    suspend fun bayarTagihan(
        @Field("nisn") nisn: String,
        @Field("id_tagihan") idTagihan: String,
        @Field("method") method: String // WAJIB ADA SESUAI KEINGINAN API BOS
    ): retrofit2.Response<TripayCheckoutResponse>

    @GET("api/akademik/materi")
    suspend fun getMateri(@Query("nisn") nisn: String): retrofit2.Response<MateriResponse>

        @GET("api/akademik/tugas")
    suspend fun getTugas(@Query("nisn") nisn: String): retrofit2.Response<TugasResponse>

    @GET("api/akademik/raport")
    suspend fun getNilaiRaport(@Query("nisn") nisn: String): retrofit2.Response<RaportResponse>

    @retrofit2.http.Multipart
    @POST("api/akademik/submit_tugas")
    suspend fun submitTugas(
        @retrofit2.http.Part("nisn") nisn: okhttp3.RequestBody,
        @retrofit2.http.Part("tugas_id") tugasId: okhttp3.RequestBody,
        @retrofit2.http.Part("catatan_siswa") catatan: okhttp3.RequestBody,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): retrofit2.Response<SubmitTugasResponse>
    // --- API KHUSUS GURU ---
    @GET("api/presensi-guru/status")
    suspend fun getStatusAbsenGuru(@Query("id_user") idUser: String): retrofit2.Response<PresensiStatusResponse> // Sesuaikan tipe responnya dengan model bos
    // --- API KHUSUS GURU ---
    @FormUrlEncoded
    @POST("api/presensi-guru/submit")
    suspend fun submitAbsenGuru(
        @Field("id_user") idUser: String,
        @Field("latitude") lat: String,
        @Field("longitude") lon: String,
        @Field("qr_token") qrToken: String
    ): retrofit2.Response<SubmitAbsenResponse> // Menggunakan model yang sama dengan siswa

    // --- LAYANAN REKAP & IZIN GURU ---
    @GET("api/presensi-guru/rekap")
    suspend fun getRekapGuru(
        @Query("id_user") idUser: String,
        @Query("bulan") bulan: String
    ): retrofit2.Response<RekapAbsenResponse>

    @FormUrlEncoded
    @POST("api/presensi-guru/izin")
    suspend fun ajukanIzinGuru(
        @Field("id_user") idUser: String,
        @Field("tanggal") tanggal: String,
        @Field("status") status: String,
        @Field("keterangan") keterangan: String,
        @Field("bukti") base64Image: String
    ): retrofit2.Response<SubmitIzinResponse>

    @GET("api/ujian/cek_waktu")
    suspend fun cekWaktuUjian(@Query("id_ujian_siswa") id: String): retrofit2.Response<CekWaktuResponse>
}

