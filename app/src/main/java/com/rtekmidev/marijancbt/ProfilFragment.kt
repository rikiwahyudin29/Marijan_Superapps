package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfilFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profil, container, false)
        
        val tvNama = view.findViewById<TextView>(R.id.tvProfilNama)
        val tvNisn = view.findViewById<TextView>(R.id.tvProfilNisn)
        val tvRole = view.findViewById<TextView>(R.id.tvProfilRole)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        
        // Sesuaikan dengan SharedPreferences yang dipakai saat login
        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nama = sharedPref.getString("nama", "Pengguna")
        val nisn = sharedPref.getString("nisn", "-")
        val role = sharedPref.getString("role", "SISWA")
        
        tvNama.text = nama
        tvNisn.text = "NISN / Username: $nisn"
        tvRole.text = "Role: ${role?.uppercase()}"
        
        btnLogout.setOnClickListener {
            // Hapus sesi login
            sharedPref.edit().clear().apply()
            
            // Arahkan kembali ke halaman Login
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        
        return view
    }
}
