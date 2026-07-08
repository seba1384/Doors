package com.example.doors.access

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doors.data.Session
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityAccessHistoryBinding

/**
 * Pantalla de "Accesos" para el residente: muestra únicamente el historial
 * de SU grupo familiar (su departamento). El escaneo de QR y la vista de
 * TODOS los accesos vive en el panel de Administración (Mi perfil).
 */
class AccessHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccessHistoryBinding
    private val repository = VisitRepository()
    private val adapter = AccessLogsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvFamilySubtitle.text =
            "Mostrando solo la actividad de tu grupo familiar (Depto ${Session.CURRENT_APARTMENT})"

        binding.rvAccessLogs.layoutManager = LinearLayoutManager(this)
        binding.rvAccessLogs.adapter = adapter

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        repository.getAccessHistory(
            apartment = Session.CURRENT_APARTMENT,
            onResult = { logs ->
                binding.progressBar.visibility = View.GONE
                adapter.submitList(logs)
                binding.emptyState.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        )
    }
}
