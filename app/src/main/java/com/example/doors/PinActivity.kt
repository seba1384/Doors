package com.example.doors

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doors.data.Session

/**
 * Pantalla de PIN de seguridad. Se muestra justo después de un login exitoso
 * (ver MainActivity) y antes de dejar entrar a la Home; funciona como un
 * segundo candado rápido de 4 dígitos, similar al lock-screen de un banco.
 */
class PinActivity : AppCompatActivity() {

    private lateinit var dots: List<View>
    private lateinit var tvError: View

    private var pin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )
        tvError = findViewById(R.id.tvPinScreenError)

        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        buttons.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                addDigit((it as Button).text.toString())
            }
        }

        findViewById<ImageButton>(R.id.btnDelete).setOnClickListener { deleteDigit() }

        // Por seguridad, "olvidé mi PIN" no revela el código: manda de vuelta
        // al login para que la persona vuelva a autenticarse desde cero.
        findViewById<View>(R.id.forgot_pin).setOnClickListener {
            Toast.makeText(
                this,
                "Por seguridad, vuelve a iniciar sesión para restablecer tu PIN.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }

        updateDots()
    }

    private fun addDigit(number: String) {
        if (pin.length >= 4) return

        tvError.visibility = View.INVISIBLE
        pin += number
        updateDots()

        if (pin.length == 4) {
            if (pin == Session.APP_PIN) {
                startActivity(
                    Intent(this, HomeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            } else {
                showWrongPin()
            }
        }
    }

    private fun deleteDigit() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            updateDots()
        }
    }

    /** PIN incorrecto: pequeña animación de "shake" en los puntos + mensaje, y se reinicia. */
    private fun showWrongPin() {
        tvError.visibility = View.VISIBLE
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        findViewById<View>(R.id.pin_dots).startAnimation(shake)

        findViewById<View>(R.id.pin_dots).postDelayed({
            pin = ""
            updateDots()
        }, 350)
    }

    private fun updateDots() {
        dots.forEachIndexed { index, view ->
            if (index < pin.length) {
                view.background.setTint(Color.WHITE)
            } else {
                view.background.setTint(Color.parseColor("#55FFFFFF"))
            }
        }
    }
}
