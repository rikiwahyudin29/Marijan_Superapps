package com.rtekmidev.marijancbt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class AkademikGuruFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_akademik_guru, container, false)
        
        view.findViewById<View>(R.id.btnJurnalMengajar)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), JurnalMengajarActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<View>(R.id.btnRekapMengajar)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), RekapMengajarActivity::class.java)
            startActivity(intent)
        }
        
        view.findViewById<View>(R.id.btnWaliKelasKehadiran)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), WaliKelasKehadiranActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<View>(R.id.btnWaliKelasKeuangan)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), WaliKelasKeuanganActivity::class.java)
            startActivity(intent)
        }
        
        return view
    }
}
