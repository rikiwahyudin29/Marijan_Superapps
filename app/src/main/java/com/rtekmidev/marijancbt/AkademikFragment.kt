package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AkademikFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_akademik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", null)

        val tvIpk = view.findViewById<TextView>(R.id.tvIpk)
        val tvTugasAktif = view.findViewById<TextView>(R.id.tvTugasAktif)
        val tvPesanBaru = view.findViewById<TextView>(R.id.tvPesanBaru)
        val llAktivitasTerkini = view.findViewById<LinearLayout>(R.id.llAktivitasTerkini)

        view.findViewById<View>(R.id.scrollViewAkademik)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardActivity)?.showBottomNav()
            }
        }

        if (!nisn.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiClient.instance.getDashboard(nisn)
                    
                    var totalMateriCount = 0
                    try {
                        val materiResponse = ApiClient.instance.getMateri(nisn)
                        if (materiResponse.isSuccessful && materiResponse.body()?.status == true) {
                            val rawData = materiResponse.body()?.data
                            if (rawData != null && rawData.isJsonArray) {
                                var count = 0
                                for (groupItem in rawData.asJsonArray) {
                                    val groupObj = groupItem.asJsonObject
                                    val materiArr = groupObj.get("materi")?.asJsonArray
                                    if (materiArr != null) count += materiArr.size()
                                }
                                totalMateriCount = count
                            }
                        }
                    } catch (e: Exception) {
                        // Silently ignore if materi fails to load
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.status == true && response.body()?.data != null) {
                            val data = response.body()?.data!!
                            tvIpk?.text = data.rata_rata_nilai?.toString() ?: "0.0"
                            tvTugasAktif?.text = data.tugas_aktif?.toString() ?: "0"
                            tvPesanBaru?.text = totalMateriCount.toString()
                            
                            // Populate aktivitas terkini
                            llAktivitasTerkini?.removeAllViews()
                            data.aktivitas_terkini?.let { aktivitasList ->
                                for ((index, aktivitas) in aktivitasList.withIndex()) {
                                    val itemView = layoutInflater.inflate(R.layout.item_aktivitas_terkini, llAktivitasTerkini, false)
                                    
                                    val ivIcon = itemView.findViewById<ImageView>(R.id.ivIconAktivitas)
                                    val tvJudul = itemView.findViewById<TextView>(R.id.tvJudulAktivitas)
                                    val tvWaktu = itemView.findViewById<TextView>(R.id.tvWaktuAktivitas)
                                    val tvStatus = itemView.findViewById<TextView>(R.id.tvStatusAktivitas)
                                    val vDivider = itemView.findViewById<View>(R.id.vDividerAktivitas)
                                    
                                    tvJudul.text = aktivitas.judul ?: "-"
                                    tvWaktu.text = aktivitas.waktu ?: "-"
                                    
                                    if (aktivitas.status.isNullOrEmpty()) {
                                        tvStatus.visibility = View.GONE
                                    } else {
                                        tvStatus.visibility = View.VISIBLE
                                        tvStatus.text = aktivitas.status
                                    }
                                    
                                    if (aktivitas.icon == "materi") {
                                        ivIcon.setImageResource(R.drawable.ic_modern_materi)
                                    } else {
                                        ivIcon.setImageResource(R.drawable.ic_modern_tugas)
                                    }
                                    
                                    // Hide divider on last item
                                    if (index == aktivitasList.size - 1) {
                                        vDivider.visibility = View.GONE
                                    }
                                    
                                    llAktivitasTerkini?.addView(itemView)
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Gagal memuat data akademik", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error koneksi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        view.findViewById<CardView>(R.id.btnTugas)?.setOnClickListener {
            val intent = Intent(requireContext(), DaftarTugasActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<CardView>(R.id.btnMateri)?.setOnClickListener {
            val intent = Intent(requireContext(), MateriBelajarActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<CardView>(R.id.btnRaport)?.setOnClickListener {
            val intent = Intent(requireContext(), NilaiRaportActivity::class.java)
            startActivity(intent)
        }
    }
}
