package com.rtekmidev.smkrjsuperapps

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CalendarDay(
    val dayNumber: Int,
    val isToday: Boolean,
    var isSelected: Boolean,
    val dateString: String,
    var status: String? = null // "hadir", "izin", "sakit", "alfa"
)

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedPosition = -1

    init {
        selectedPosition = days.indexOfFirst { it.isSelected }
    }

    class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
        val viewDot: View = view.findViewById(R.id.viewDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]

        if (day.dayNumber == 0) {
            holder.tvDayNumber.text = ""
            holder.tvDayNumber.setBackgroundColor(Color.TRANSPARENT)
            holder.viewDot.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener(null)
            return
        }

        holder.tvDayNumber.text = day.dayNumber.toString()

        // Handle styling
        if (day.isSelected) {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_day_selected)
            holder.tvDayNumber.setTextColor(Color.WHITE)
        } else if (day.isToday) {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_day_today)
            holder.tvDayNumber.setTextColor(Color.parseColor("#0D9488")) // Teal color matching image
        } else {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_day_normal)
            holder.tvDayNumber.setTextColor(Color.parseColor("#111827"))
        }

        // Handle Dot
        if (day.status != null) {
            holder.viewDot.visibility = View.VISIBLE
            val drawable = holder.viewDot.background.mutate() as android.graphics.drawable.GradientDrawable
            when (day.status?.lowercase()?.trim()) {
                "hadir", "tepat waktu" -> drawable.setColor(Color.parseColor("#1E3A8A")) // Blue
                "izin", "izin pulang", "udzur syar'i", "cuti", "dinas luar" -> drawable.setColor(Color.parseColor("#00D2D3")) // Cyan
                "sakit" -> drawable.setColor(Color.parseColor("#D1D5DB")) // Gray
                "alfa", "alpha", "alpa", "a" -> drawable.setColor(Color.parseColor("#EF4444")) // Red
                "terlambat" -> drawable.setColor(Color.parseColor("#F59E0B")) // Amber
                else -> drawable.setColor(Color.TRANSPARENT)
            }
        } else {
            holder.viewDot.visibility = View.INVISIBLE
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            
            if (previousSelected != -1) {
                days[previousSelected].isSelected = false
                notifyItemChanged(previousSelected)
            }
            
            days[selectedPosition].isSelected = true
            notifyItemChanged(selectedPosition)
            
            onDayClick(day)
        }
    }

    override fun getItemCount() = days.size
}
