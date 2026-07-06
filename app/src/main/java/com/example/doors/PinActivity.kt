package com.example.doors

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PinActivity : AppCompatActivity() {

    private lateinit var dots: List<View>

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

        val buttons = listOf(
            R.id.btn0,
            R.id.btn1,
            R.id.btn2,
            R.id.btn3,
            R.id.btn4,
            R.id.btn5,
            R.id.btn6,
            R.id.btn7,
            R.id.btn8,
            R.id.btn9
        )

        buttons.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                addDigit((it as Button).text.toString())
            }
        }

        findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            deleteDigit()
        }

        updateDots()
    }

    private fun addDigit(number: String) {
        if (pin.length < 4) {
            pin += number
            updateDots()

            if (pin.length == 4) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }

    private fun deleteDigit() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            updateDots()
        }
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