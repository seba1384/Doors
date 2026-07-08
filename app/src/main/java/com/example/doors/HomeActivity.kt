package com.example.doors

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doors.data.Session
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityHomeBinding
import com.example.doors.visits.MyVisitsActivity
import com.example.doors.visits.RegisterVisitActivity
import com.example.doors.access.AccessHistoryActivity
import com.example.doors.notifications.NotificationsActivity
import com.example.doors.profile.ProfileActivity

/**
 * Pantalla principal (Home) del residente.
 * Muestra el saludo, el banner de seguridad, los accesos rápidos
 * y la barra de navegación inferior.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val repository = VisitRepository()

    private var isGateOpen = false
    private var autoCloseTimer: CountDownTimer? = null
    private val gateOpenDurationMs = 5 * 60 * 1000L // 5 minutos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        loadResidentData()
        setupQuickAccessCards()
        setupBottomNavigation()
        setupGateButton()
        loadAccessStats()
    }

    override fun onResume() {
        super.onResume()
        loadAccessStats()
    }

    /** Dashboard compacto: cuántas veces se han usado los QR generados por este departamento. */
    private fun loadAccessStats() {
        repository.getAccessStats(
            apartment = Session.CURRENT_APARTMENT,
            onResult = { approved, rejected ->
                binding.tvStatApproved.text = approved.toString()
                binding.tvStatRejected.text = rejected.toString()
                binding.tvStatTotal.text = (approved + rejected).toString()
            },
            onError = {
                binding.tvStatApproved.text = "-"
                binding.tvStatRejected.text = "-"
                binding.tvStatTotal.text = "-"
            }
        )
    }

    private fun setupGateButton() {
        binding.btnGate.setOnClickListener {
            if (isGateOpen) closeGate() else openGate()
        }
    }

    private fun openGate() {
        isGateOpen = true
        autoCloseTimer?.cancel()

        binding.btnGate.setBackgroundResource(R.drawable.bg_gate_button_open)
        binding.imgGateIcon.setImageResource(R.drawable.ic_lock_open)
        binding.tvGateTitle.text = "Cerrar portón"
        repository.logManualGateOpening()

        autoCloseTimer = object : CountDownTimer(gateOpenDurationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = (millisUntilFinished / 1000) + 1
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                binding.tvGateSubtitle.text = String.format("Se cerrará solo en %d:%02d", minutes, seconds)
            }
            override fun onFinish() {
                closeGate()
            }
        }.start()
    }

    private fun closeGate() {
        isGateOpen = false
        autoCloseTimer?.cancel()
        autoCloseTimer = null

        binding.btnGate.setBackgroundResource(R.drawable.bg_gate_button_closed)
        binding.imgGateIcon.setImageResource(R.drawable.ic_lock_closed)
        binding.tvGateTitle.text = "Abrir portón"
        binding.tvGateSubtitle.text = "Toca para dar acceso al condominio"
    }

    override fun onDestroy() {
        super.onDestroy()
        autoCloseTimer?.cancel()
    }

    /** Ajusta el padding del header y del bottom nav para respetar la status bar / gesture bar. */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top + 20, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
            insets
        }
    }

    /**
     * TODO: reemplazar por los datos reales del residente autenticado
     * (por ejemplo, desde Firebase Auth / Firestore una vez el login esté integrado).
     */
    private fun loadResidentData() {
        binding.tvGreeting.text = "¡Hola, ${Session.CURRENT_RESIDENT_NAME.substringBefore(" ")}!"
        binding.tvUserInfo.text = "Residente • Departamento ${Session.CURRENT_APARTMENT}"
    }

    private fun setupQuickAccessCards() {
        binding.cardRegistrarVisita.setOnClickListener {
            startActivity(Intent(this, RegisterVisitActivity::class.java))
        }
        binding.btnProfileAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_inicio
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_visitas -> {
                    startActivity(Intent(this, MyVisitsActivity::class.java))
                    false
                }
                R.id.nav_accesos -> {
                    startActivity(Intent(this, AccessHistoryActivity::class.java))
                    false
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
}
