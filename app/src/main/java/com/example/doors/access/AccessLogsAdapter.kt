package com.example.doors.access

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doors.R
import com.example.doors.data.AccessLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccessLogsAdapter(
    private val items: MutableList<AccessLog> = mutableListOf()
) : RecyclerView.Adapter<AccessLogsAdapter.LogViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("es", "ES"))

    fun submitList(newItems: List<AccessLog>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_access_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(items[position], dateFormat)
    }

    override fun getItemCount(): Int = items.size

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgIcon: ImageView = view.findViewById(R.id.imgLogIcon)
        private val tvName: TextView = view.findViewById(R.id.tvLogName)
        private val tvDate: TextView = view.findViewById(R.id.tvLogDate)
        private val tvStatus: TextView = view.findViewById(R.id.tvLogStatus)

        fun bind(log: AccessLog, dateFormat: SimpleDateFormat) {
            tvName.text = log.visitorName
            val deptTag = if (log.apartment.isNotBlank()) "Depto ${log.apartment} • " else ""
            tvDate.text = "$deptTag${dateFormat.format(Date(log.timestamp))}"

            if (log.granted) {
                imgIcon.setImageResource(R.drawable.ic_result_success)
                tvStatus.text = if (log.movement.isNotBlank()) log.movement else "Concedido"
                tvStatus.setTextColor(0xFF2ED573.toInt())
            } else {
                imgIcon.setImageResource(R.drawable.ic_result_error)
                tvStatus.text = "Rechazado"
                tvStatus.setTextColor(0xFFFF5C6C.toInt())
            }
        }
    }
}
