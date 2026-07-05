package com.example.doors.visits

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.doors.data.QrGenerator
import com.example.doors.data.Visit
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityRegisterVisitBinding
import java.util.Calendar
import java.util.UUID

/**
 * Registra una nueva visita:
 * 1. Valida el formulario.
 * 2. Crea el documento en Firestore (colección "visitas") con un ID único (UUID).
 * 3. Genera el QR a partir de ese mismo ID y lo muestra en pantalla.
 *
 * Ese mismo ID es lo único que el QR contiene; por eso, al escanearlo,
 * basta con buscar ese ID en la base de datos para saber si es válido.
 */
class RegisterVisitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterVisitBinding
    private val repository = VisitRepository()
    private var generatedVisit: Visit? = null
    private var selectedVisitType: String = "Familiar"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterVisitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.etVisitDate.setOnClickListener { showDatePicker() }
        binding.btnGenerateQr.setOnClickListener { onGenerateQrClicked() }
        binding.btnNewVisit.setOnClickListener { resetForm() }
        binding.btnShareQr.setOnClickListener { shareQrCode() }

        setupVisitTypeChips()
    }

    /** Los tipos de visita que no requieren RUT (no necesitamos identificar a la persona). */
    private val typesWithoutDocument = setOf("Delivery", "Proveedor")

    private fun setupVisitTypeChips() {
        val chips = listOf(
            binding.chipFamiliar to "Familiar",
            binding.chipDelivery to "Delivery",
            binding.chipProveedor to "Proveedor",
            binding.chipOtro to "Otro"
        )
        chips.forEach { (chip, type) ->
            chip.setOnClickListener { selectVisitType(type, chips) }
        }
        selectVisitType(selectedVisitType, chips)
    }

    private fun selectVisitType(type: String, chips: List<Pair<android.widget.TextView, String>>) {
        selectedVisitType = type
        chips.forEach { (chip, chipType) ->
            if (chipType == type) {
                chip.setBackgroundResource(com.example.doors.R.drawable.bg_chip_selected)
                chip.setTextColor(resources.getColor(com.example.doors.R.color.white, theme))
            } else {
                chip.setBackgroundResource(com.example.doors.R.drawable.bg_chip_unselected)
                chip.setTextColor(resources.getColor(com.example.doors.R.color.text_secondary, theme))
            }
        }

        // Delivery y Proveedor no necesitan RUT: ocultamos ese campo.
        val needsDocument = type !in typesWithoutDocument
        binding.etVisitorDocument.visibility = if (needsDocument) View.VISIBLE else View.GONE
        if (!needsDocument) binding.etVisitorDocument.setText("")
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val formatted = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                binding.etVisitDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun onGenerateQrClicked() {
        val name = binding.etVisitorName.text.toString().trim()
        val document = binding.etVisitorDocument.text.toString().trim()
        val date = binding.etVisitDate.text.toString().trim()
        val apartment = binding.etApartment.text.toString().trim()
        val needsDocument = selectedVisitType !in typesWithoutDocument

        if (name.isEmpty() || date.isEmpty() || apartment.isEmpty() || (needsDocument && document.isEmpty())) {
            showError("Completa todos los campos para continuar.")
            return
        }

        showError(null)
        setLoading(true)

        val visit = Visit(
            id = UUID.randomUUID().toString(),
            visitType = selectedVisitType,
            visitorName = name,
            visitorDocument = if (needsDocument) document else "-",
            apartment = apartment,
            visitDate = date,
            status = "pendiente",
            createdAt = System.currentTimeMillis()
        )

        repository.createVisit(
            visit = visit,
            onSuccess = {
                setLoading(false)
                generatedVisit = visit
                showQrResult(visit)
            },
            onError = {
                setLoading(false)
                showError("No se pudo registrar la visita. Revisa tu conexión e intenta de nuevo.")
            }
        )
    }

    private fun showQrResult(visit: Visit) {
        val qrBitmap = QrGenerator.generate(visit.id)
        binding.imgQrCode.setImageBitmap(qrBitmap)
        binding.tvQrId.text = visit.id
        binding.tvResultSubtitle.text =
            "Comparte este código con ${visit.visitorName} para que lo presente en el portón."

        binding.formContainer.visibility = View.GONE
        binding.resultContainer.visibility = View.VISIBLE
    }

    private fun resetForm() {
        binding.etVisitorName.setText("")
        binding.etVisitorDocument.setText("")
        binding.etVisitDate.setText("")
        binding.etApartment.setText("")
        generatedVisit = null
        setupVisitTypeChips()

        binding.resultContainer.visibility = View.GONE
        binding.formContainer.visibility = View.VISIBLE
    }

    private fun shareQrCode() {
        val visit = generatedVisit ?: return
        val message = "Código de acceso para ${visit.visitorName} (${visit.visitDate}): ${visit.id}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir código de acceso"))
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnGenerateQr.isEnabled = !isLoading
    }

    private fun showError(message: String?) {
        if (message == null) {
            binding.tvError.visibility = View.GONE
        } else {
            binding.tvError.text = message
            binding.tvError.visibility = View.VISIBLE
        }
    }
}
