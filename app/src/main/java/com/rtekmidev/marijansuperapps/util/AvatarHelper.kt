package com.rtekmidev.marijansuperapps.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

object AvatarHelper {

    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "U"
        
        // Clean title prefixes/suffixes (e.g. S.Pd, M.Kom, S.T, Dra, Drs, H., Hj.)
        val cleanName = name.replace(Regex("(?i)(S\\.Pd|M\\.Kom|S\\.T|S\\.Kom|M\\.Pd|Drs|Dra|H\\.|Hj\\.|,.*)"), "").trim()
        val parts = cleanName.split(Regex("\\s+")).filter { it.isNotBlank() }
        
        return when {
            parts.isEmpty() -> "U"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> ("" + parts[0].first() + parts[1].first()).uppercase()
        }
    }

    fun setAvatar(
        context: Context,
        name: String?,
        fotoUrl: String?,
        ivPhoto: ImageView?,
        tvInitial: TextView?,
        cardContainer: CardView? = null
    ) {
        val initials = getInitials(name)
        tvInitial?.text = initials
        tvInitial?.visibility = View.VISIBLE
        ivPhoto?.visibility = View.GONE
        cardContainer?.setCardBackgroundColor(Color.parseColor("#EEF2FF"))

        val isValidFoto = !fotoUrl.isNullOrBlank() &&
                !fotoUrl.contains("default.png") &&
                !fotoUrl.contains("default.jpg") &&
                !fotoUrl.endsWith("/siswa/") &&
                !fotoUrl.endsWith("/guru/")

        if (isValidFoto && ivPhoto != null) {
            Glide.with(context)
                .load(fotoUrl)
                .circleCrop()
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        ivPhoto.setImageDrawable(resource)
                        ivPhoto.visibility = View.VISIBLE
                        tvInitial?.visibility = View.GONE
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        ivPhoto.setImageDrawable(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        ivPhoto.visibility = View.GONE
                        tvInitial?.visibility = View.VISIBLE
                    }
                })
        }
    }
}
