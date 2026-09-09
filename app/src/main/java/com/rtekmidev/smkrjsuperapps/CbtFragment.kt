package com.rtekmidev.smkrjsuperapps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class CbtFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(requireContext()).apply {
            text = "Menu CBT"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }
        return tv
    }
}
