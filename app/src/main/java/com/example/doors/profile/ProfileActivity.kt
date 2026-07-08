package com.example.doors.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doors.MainActivity
import com.example.doors.admin.AdminActivity
import com.example.doors.data.Session
import com.example.doors.databinding.ActivityProfileBinding
import com.example.doors.databinding.DialogPinInputBinding

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

        binding.tvProfileName.text = Session.CURRENT_RESIDENT_NAME
        binding.tvProfileRole.text = "Residente • Departamento ${Session.CURRENT_APARTMENT}"
        binding.tvProfileApartment.text = Session.CURRENT_APARTMENT

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdminPanel.setOnClickListener { showAdminPinDialog() }
        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    /** Pide el PIN de conserjería antes de dejar entrar al panel de administración. */
    private fun showAdminPinDialog() {
        val dialogBinding = DialogPinInputBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnPinCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnPinConfirm.setOnClickListener {
            val enteredPin = dialogBinding.etPin.text.toString().trim()
            if (enteredPin == Session.ADMIN_PIN) {
                dialog.dismiss()
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                dialogBinding.tvPinError.visibility = View.VISIBLE
                dialogBinding.etPin.setText("")
            }
        }

        dialog.show()
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