package com.example.doors.visits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doors.R
import com.example.doors.data.Visit

class VisitsAdapter(
    private val onItemClick: (Visit) -> Unit = {},
    private val items: MutableList<Visit> = mutableListOf()
) : RecyclerView.Adapter<VisitsAdapter.VisitViewHolder>() {

    fun submitList(newItems: List<Visit>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_visit, parent, false)
        return VisitViewHolder(view)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class VisitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvItemName)
        private val tvDetail: TextView = view.findViewById(R.id.tvItemDetail)
        private val tvStatus: TextView = view.findViewById(R.id.tvItemStatus)

        fun bind(visit: Visit, onItemClick: (Visit) -> Unit) {
            tvName.text = visit.visitorName
            tvDetail.text = "${visit.visitType} • Depto ${visit.apartment} • ${visit.visitDateFrom}"

            if (visit.status == "usado") {
                tvStatus.text = "Usado"
                tvStatus.setTextColor(0xFFA8A0C4.toInt())
            } else {
                tvStatus.text = "Pendiente"
                tvStatus.setTextColor(0xFF2ED573.toInt())
            }

            itemView.setOnClickListener { onItemClick(visit) }
            itemView.isClickable = true
            itemView.isFocusable = true
        }
    }
}
