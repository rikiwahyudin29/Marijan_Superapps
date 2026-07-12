package com.rtekmidev.marijancbt

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

class BerandaGuruFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_beranda_guru, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val namaGuru = sharedPref.getString("nama", "Guru")

        // Views from Global Header (in Activity)
        val tvNamaDashboard = requireActivity().findViewById<TextView>(R.id.tvNamaDashboard)
        val tvKelas = requireActivity().findViewById<TextView>(R.id.tvKelas)
        val ivProfilPhoto = requireActivity().findViewById<ImageView>(R.id.ivProfilPhoto)

        tvNamaDashboard?.text = "Selamat Datang, $namaGuru!"
        tvKelas?.text = "Tenaga Pendidik"

        // For now, load placeholder for profile photo or from shared prefs if exists
        val fotoProfilUrl = sharedPref.getString("foto_profil", null)
        if (!fotoProfilUrl.isNullOrEmpty() && ivProfilPhoto != null) {
            val fullUrl = "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/" + fotoProfilUrl
            try {
                Glide.with(this)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(ivProfilPhoto)
            } catch (e: Exception) {
                // Ignore glide errors
            }
        }
    }
}
