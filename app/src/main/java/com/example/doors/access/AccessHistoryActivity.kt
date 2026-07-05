package com.example.doors.access

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doors.R
import com.example.doors.data.ValidationResult
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityAccessHistoryBinding
import com.example.doors.databinding.DialogScanResultBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Pantalla de "Accesos":
 *  - Botón para escanear un código QR con la cámara (portón).
 *  - Historial de todos los escaneos (válidos o no).
 *
 * La validación es simple pero es la regla clave del sistema:
 * un QR solo es válido si su ID existe en la colección "visitas" de Firestore
 * y aún no ha sido usado. Cualquier otro caso => acceso denegado.
 */
class AccessHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccessHistoryBinding
    private val repository = VisitRepository()
    private val adapter = AccessLogsAdapter()

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents
        if (content != null) {
            validateScannedCode(content)
        }
        // Si content es null, el usuario canceló el escaneo: no hacemos nada.
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScanner()
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara para escanear el QR", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnScanQr.setOnClickListener { onScanButtonClicked() }

        binding.rvAccessLogs.layoutManager = LinearLayoutManager(this)
        binding.rvAccessLogs.adapter = adapter

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun onScanButtonClicked() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Escanea el código QR de la visita")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCameraId(0)
        }
        scanLauncher.launch(options)
    }

    /** Consulta Firestore para saber si el contenido escaneado corresponde a una visita válida. */
    private fun validateScannedCode(content: String) {
        repository.validateQr(content) { result ->
            when (result) {
                is ValidationResult.Granted -> showResultDialog(
                    iconRes = R.drawable.ic_result_success,
                    title = "Acceso concedido",
                    message = "Bienvenido/a, ${result.visit.visitorName}. Visita al depto. ${result.visit.apartment}.",
                    titleColor = 0xFF2ED573.toInt()
                )
                is ValidationResult.AlreadyUsed -> showResultDialog(
                    iconRes = R.drawable.ic_result_warning,
                    title = "Este código ya fue usado",
                    message = "El QR de ${result.visit.visitorName} ya se utilizó anteriormente y no permite un nuevo ingreso.",
                    titleColor = 0xFFFFA53D.toInt()
                )
                is ValidationResult.Invalid -> showResultDialog(
                    iconRes = R.drawable.ic_result_error,
                    title = "⚠ QR no válido",
                    message = "Este código no corresponde a ninguna visita registrada. Acceso denegado.",
                    titleColor = 0xFFFF5C6C.toInt()
                )
                is ValidationResult.Error -> showResultDialog(
                    iconRes = R.drawable.ic_result_error,
                    title = "No se pudo validar",
                    message = "Ocurrió un problema de conexión. Intenta escanear nuevamente.",
                    titleColor = 0xFFFF5C6C.toInt()
                )
            }
            loadHistory()
        }
    }

    private fun showResultDialog(iconRes: Int, title: String, message: String, titleColor: Int) {
        val dialogBinding = DialogScanResultBinding.inflate(LayoutInflater.from(this))
        dialogBinding.imgResultIcon.setImageResource(iconRes)
        dialogBinding.tvResultTitle.text = title
        dialogBinding.tvResultTitle.setTextColor(titleColor)
        dialogBinding.tvResultMessage.text = message

        val cardBackground = GradientDrawable().apply {
            cornerRadius = 20f * resources.displayMetrics.density
            setColor(ContextCompat.getColor(this@AccessHistoryActivity, com.example.doors.R.color.surface_card))
            setStroke((2 * resources.displayMetrics.density).toInt(), titleColor)
        }
        dialogBinding.root.background = cardBackground

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.btnResultClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        repository.getAccessHistory(
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
