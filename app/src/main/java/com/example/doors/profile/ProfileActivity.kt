package com.example.doors.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doors.MainActivity
import com.example.doors.databinding.ActivityProfileBinding

/**
 * Perfil del residente.
 * TODO: cuando el login esté integrado, cargar estos datos desde
 * Firebase Auth / Firestore en vez de dejarlos fijos.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres cerrar tu sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ -> goToLogin() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
