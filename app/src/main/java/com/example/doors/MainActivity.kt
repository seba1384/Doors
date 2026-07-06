package com.example.doors

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // TODO (login real): reemplazar este bloque por la validación con
        // Firebase Auth (o lo que estén usando). Por ahora, cualquier click
        // en "Iniciar sesión" entra directo a la Home, solo para poder
        // probar el resto de la app mientras se termina el login.
        val loginButton = findViewById<android.widget.Button>(R.id.login_btn)
        loginButton.setOnClickListener {
            startActivity(Intent(this, PinActivity::class.java))
            finish()
        }
    }
}