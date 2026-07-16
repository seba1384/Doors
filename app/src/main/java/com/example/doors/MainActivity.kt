package com.example.doors

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        tvError = findViewById(R.id.tvLoginError)

        // TODO (login real): reemplazar este bloque por la validación con
        // Firebase Auth (o lo que estén usando). Por ahora solo validamos que
        // los campos no estén vacíos y que el correo tenga un formato válido;
        // cualquier combinación que pase esa validación entra a la app,
        // pasando primero por el PIN de seguridad (PinActivity).
        val loginButton = findViewById<Button>(R.id.login_btn)
        loginButton.setOnClickListener {
            if (validateForm()) {
                startActivity(Intent(this, PinActivity::class.java))
                finish()
            }
        }

        // Limpia el mensaje de error apenas la persona empieza a corregir los campos.
        emailInput.setOnClickListener { hideError() }
        passwordInput.setOnClickListener { hideError() }
    }

    private fun validateForm(): Boolean {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        return when {
            email.isEmpty() || password.isEmpty() -> {
                showError("Completa tu correo y contraseña para continuar.")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Ingresa un correo electrónico válido.")
                false
            }
            password.length < 4 -> {
                showError("La contraseña debe tener al menos 4 caracteres.")
                false
            }
            else -> {
                hideError()
                true
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
