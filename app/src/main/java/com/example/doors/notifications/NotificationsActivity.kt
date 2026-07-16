package com.example.doors.notifications

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doors.data.Session
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityNotificationsBinding

/**
 * Notificaciones del residente: combina los accesos de su grupo familiar
 * con los códigos QR que ha generado, ordenados por fecha.
 */
class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val repository = VisitRepository()
    private val adapter = NotificationsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        repository.getNotifications(
            apartment = Session.CURRENT_APARTMENT,
            onResult = { notifications ->
                binding.progressBar.visibility = View.GONE
                adapter.submitList(notifications)
                binding.emptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        )
    }
}
