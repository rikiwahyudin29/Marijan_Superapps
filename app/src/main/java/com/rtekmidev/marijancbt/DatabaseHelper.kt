package com.rtekmidev.marijancbt

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MarijanCBT_Offline.db"
        // 🔥 NAIKKAN VERSI KE 4 BIAR SQLITE BENAR-BENAR TER-RESET 🔥
        private const val DATABASE_VERSION = 4

        const val TABLE_SESI = "sesi_ujian"
        const val COL_ID_UJIAN = "id_ujian_siswa"
        const val COL_DURASI = "durasi"
        const val COL_MIN_FINISH = "min_finish"
        const val COL_JSON_SOAL = "json_soal"

        const val TABLE_JAWABAN = "jawaban_siswa"
        const val COL_SOAL_ID = "soal_id"
        const val COL_JENIS_SOAL = "jenis_soal"
        const val COL_JAWABAN_TEKS = "jawaban"
        const val COL_RAGU = "ragu_ragu"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_SESI ($COL_ID_UJIAN TEXT PRIMARY KEY, $COL_DURASI TEXT, $COL_MIN_FINISH TEXT, $COL_JSON_SOAL TEXT)")
        db.execSQL("CREATE TABLE $TABLE_JAWABAN ($COL_SOAL_ID TEXT PRIMARY KEY, $COL_JENIS_SOAL TEXT, $COL_JAWABAN_TEKS TEXT, $COL_RAGU TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESI")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_JAWABAN")
        onCreate(db)
    }

    fun clearSemuaData() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TABLE_SESI")
        db.execSQL("DELETE FROM $TABLE_JAWABAN")
        db.close()
    }

    fun simpanSesiSoal(idUjian: String, durasi: String, minFinish: String, jsonSoal: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_ID_UJIAN, idUjian)
        values.put(COL_DURASI, durasi)
        values.put(COL_MIN_FINISH, minFinish)
        values.put(COL_JSON_SOAL, jsonSoal)
        db.insertWithOnConflict(TABLE_SESI, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // 🔥 PERBAIKAN: Baca dulu status ragu SEBELUM buka writableDatabase biar gak Crash/Tabrakan
    fun simpanJawaban(soalId: String, jenisSoal: String, jawaban: String) {
        val isRagu = getRaguStatus(soalId) // Panggil di luar biar aman

        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_SOAL_ID, soalId)
        values.put(COL_JENIS_SOAL, jenisSoal)
        values.put(COL_JAWABAN_TEKS, jawaban)
        values.put(COL_RAGU, if (isRagu) "1" else "0")

        db.insertWithOnConflict(TABLE_JAWABAN, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // 🔥 PERBAIKAN: Baca dulu jawaban lama SEBELUM buka writableDatabase biar gak Crash/Tabrakan
    fun setRaguStatus(soalId: String, jenisSoal: String, isRagu: Boolean) {
        val jawabanLama = getJawabanSiswa(soalId) // Panggil di luar biar aman

        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_SOAL_ID, soalId)
        values.put(COL_JENIS_SOAL, jenisSoal)
        values.put(COL_JAWABAN_TEKS, jawabanLama)
        values.put(COL_RAGU, if (isRagu) "1" else "0")

        db.insertWithOnConflict(TABLE_JAWABAN, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getJawabanSiswa(soalId: String): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_JAWABAN_TEKS FROM $TABLE_JAWABAN WHERE $COL_SOAL_ID = ?", arrayOf(soalId))
        var jawaban = ""
        if (cursor.moveToFirst()) jawaban = cursor.getString(0) ?: ""
        cursor.close()
        db.close()
        return jawaban
    }

    // 🔥 FUNGSI BARU: Ambil status Ragu-ragu dari SQLite
    fun getRaguStatus(soalId: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_RAGU FROM $TABLE_JAWABAN WHERE $COL_SOAL_ID = ?", arrayOf(soalId))
        var isRagu = false
        if (cursor.moveToFirst()) isRagu = (cursor.getString(0) == "1")
        cursor.close()
        db.close()
        return isRagu
    }
    // 🔥 FUNGSI BARU: Cek apakah masih ada jawaban yang dicentang Ragu-Ragu
    fun isAdaRaguRagu(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_JAWABAN WHERE $COL_RAGU = '1'", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count > 0 // Akan bernilai True jika ada 1 saja soal yang masih ragu
    }
    fun getDetailSesi(): Map<String, String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_ID_UJIAN, $COL_DURASI, $COL_JSON_SOAL, $COL_MIN_FINISH FROM $TABLE_SESI LIMIT 1", null)
        val data = mutableMapOf<String, String>()
        if (cursor.moveToFirst()) {
            data["id_ujian"] = cursor.getString(0) ?: ""
            data["durasi"] = cursor.getString(1) ?: "90"
            data["json_soal"] = cursor.getString(2) ?: ""
            data["min_finish"] = cursor.getString(3) ?: "0"
        }
        cursor.close()
        db.close()
        return data
    }

    fun getAllJawaban(): List<com.rtekmidev.marijancbt.api.JawabanSiswa> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_SOAL_ID, $COL_JENIS_SOAL, $COL_JAWABAN_TEKS FROM $TABLE_JAWABAN", null)
        val list = mutableListOf<com.rtekmidev.marijancbt.api.JawabanSiswa>()
        if (cursor.moveToFirst()) {
            do {
                list.add(com.rtekmidev.marijancbt.api.JawabanSiswa(
                    soal_id = cursor.getString(0) ?: "",
                    jenis_soal = cursor.getString(1) ?: "",
                    jawaban = cursor.getString(2) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
}