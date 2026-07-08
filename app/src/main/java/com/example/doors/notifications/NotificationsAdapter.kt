package com.example.doors.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doors.R
import com.example.doors.data.NotificationItem
import com.example.doors.data.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val items: MutableList<NotificationItem> = mutableListOf()
) : RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("es", "ES"))

    fun submitList(newItems: List<NotificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        holder.bind(items[position], dateFormat)
    }

    override fun getItemCount(): Int = items.size

    class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgIcon: ImageView = view.findViewById(R.id.imgNotifIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        private val tvMessage: TextView = view.findViewById(R.id.tvNotifMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvNotifTime)

        fun bind(item: NotificationItem, dateFormat: SimpleDateFormat) {
            tvTitle.text = item.title
            tvMessage.text = item.message
            tvTime.text = dateFormat.format(Date(item.timestamp))

            val iconRes = when (item.type) {
                NotificationType.ACCESS_GRANTED -> R.drawable.ic_result_success
                NotificationType.ACCESS_DENIED -> R.drawable.ic_result_error
                NotificationType.QR_CREATED -> R.drawable.ic_result_info
            }
            imgIcon.setImageResource(iconRes)
        }
    }
}
