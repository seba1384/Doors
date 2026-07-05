package com.example.doors.visits

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityMyVisitsBinding

/** Lista todas las visitas registradas por el residente, más recientes primero. */
class MyVisitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyVisitsBinding
    private val repository = VisitRepository()
    private val adapter = VisitsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyVisitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvVisits.layoutManager = LinearLayoutManager(this)
        binding.rvVisits.adapter = adapter

        loadVisits()
    }

    private fun loadVisits() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        repository.getVisits(
            onResult = { visits ->
                binding.progressBar.visibility = View.GONE
                adapter.submitList(visits)
                binding.emptyState.visibility = if (visits.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        )
    }

    override fun onResume() {
        super.onResume()
        loadVisits()
    }
}
