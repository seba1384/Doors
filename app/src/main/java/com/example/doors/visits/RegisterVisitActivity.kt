package com.example.doors.visits

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.doors.data.QrGenerator
import com.example.doors.data.Session
import com.example.doors.data.Visit
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityRegisterVisitBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Registra una nueva visita:
 * 1. Valida el formulario (incluida la ventana "desde" / "hasta" de la visita).
 * 2. Crea el documento en Firestore (colección "visitas") con un ID único (UUID)
 *    y guarda el instante exacto en que ese QR debe dejar de servir (expiresAtMillis).
 * 3. Genera el QR a partir de ese mismo ID y lo muestra en pantalla junto a sus datos.
 *
 * El QR en sí solo contiene el ID de la visita (nada de fechas ni datos personales).
 * La vigencia y el límite de un solo uso se controlan del lado del servidor
 * (VisitRepository.validateQr), así nadie puede "extender" un código editando
 * lo que ve en la app.
 */
class RegisterVisitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterVisitBinding
    private val repository = VisitRepository()
    private var generatedVisit: Visit? = null
    private var generatedQrBitmap: Bitmap? = null
    private var selectedVisitType: String = "Familiar o amigo"

    private var fromCalendar: Calendar? = null
    private var toCalendar: Calendar? = null

    private val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterVisitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.etVisitDateFrom.setOnClickListener { pickDateTime(isFrom = true) }
        binding.etVisitDateTo.setOnClickListener { pickDateTime(isFrom = false) }
        binding.btnGenerateQr.setOnClickListener { onGenerateQrClicked() }
        binding.btnNewVisit.setOnClickListener { resetForm() }
        binding.btnShareQr.setOnClickListener { shareQrCode() }

        // El residente solo registra visitas para su propio departamento
        // (así el filtro por "grupo familiar" funciona en toda la app).
        binding.etApartment.setText(Session.CURRENT_APARTMENT)

        setupVisitTypeChips()
    }

    /** Los tipos de visita que no requieren RUT (no necesitamos identificar a la persona). */
    private val typesWithoutDocument = setOf("Delivery", "Proveedor")

    private fun setupVisitTypeChips() {
        val chips = listOf(
            binding.chipFamiliar to "Familiar o amigo",
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
                tintChipIcon(chip, resources.getColor(com.example.doors.R.color.white, theme))
            } else {
                chip.setBackgroundResource(com.example.doors.R.drawable.bg_chip_unselected)
                chip.setTextColor(resources.getColor(com.example.doors.R.color.text_secondary, theme))
                tintChipIcon(chip, resources.getColor(com.example.doors.R.color.text_secondary, theme))
            }
        }

        // Delivery y Proveedor no necesitan RUT: ocultamos ese campo.
        val needsDocument = type !in typesWithoutDocument
        binding.etVisitorDocument.visibility = if (needsDocument) View.VISIBLE else View.GONE
        if (!needsDocument) binding.etVisitorDocument.setText("")
    }

    /** Tiñe el ícono de cada chip (logo del tipo de visita) según si está seleccionado o no. */
    private fun tintChipIcon(chip: android.widget.TextView, color: Int) {
        chip.compoundDrawables.getOrNull(0)?.let { icon ->
            icon.mutate()
            androidx.core.graphics.drawable.DrawableCompat.setTint(icon, color)
        }
    }

    /**
     * Abre primero el selector de fecha y luego el de hora, y guarda el resultado
     * combinado como un [Calendar] (para poder calcular la vigencia real del QR),
     * además de mostrarlo formateado en el campo correspondiente.
     */
    private fun pickDateTime(isFrom: Boolean) {
        val base = (if (isFrom) fromCalendar else toCalendar) ?: Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val withDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        withDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        withDate.set(Calendar.MINUTE, minute)
                        withDate.set(Calendar.SECOND, 0)

                        if (isFrom) {
                            fromCalendar = withDate
                            binding.etVisitDateFrom.setText(displayFormat.format(withDate.time))
                        } else {
                            toCalendar = withDate
                            binding.etVisitDateTo.setText(displayFormat.format(withDate.time))
                        }
                    },
                    base.get(Calendar.HOUR_OF_DAY),
                    base.get(Calendar.MINUTE),
                    true
                ).show()
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun onGenerateQrClicked() {
        val name = binding.etVisitorName.text.toString().trim()
        val document = binding.etVisitorDocument.text.toString().trim()
        val apartment = binding.etApartment.text.toString().trim()
        val vehicle = binding.etVehicle.text.toString().trim()
        val reason = binding.etReason.text.toString().trim()
        val needsDocument = selectedVisitType !in typesWithoutDocument

        val from = fromCalendar
        val to = toCalendar

        if (name.isEmpty() || apartment.isEmpty() || (needsDocument && document.isEmpty())) {
            showError("Completa todos los campos para continuar.")
            return
        }
        if (from == null || to == null) {
            showError("Indica desde y hasta cuándo estará vigente el código.")
            return
        }
        if (to.timeInMillis <= from.timeInMillis) {
            showError("La fecha y hora \"Hasta\" debe ser posterior a \"Desde\".")
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
            visitDateFrom = displayFormat.format(from.time),
            visitDateTo = displayFormat.format(to.time),
            vehicle = vehicle,
            reason = reason,
            expiresAtMillis = to.timeInMillis,
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
        generatedQrBitmap = qrBitmap
        binding.imgQrCode.setImageBitmap(qrBitmap)
        binding.tvQrId.text = visit.id
        binding.tvResultSubtitle.text =
            "Comparte este código con ${visit.visitorName} para que pueda ingresar al condominio."

        binding.tvDetailVisitor.text = visit.visitorName
        binding.tvDetailType.text = visit.visitType
        binding.tvDetailFrom.text = visit.visitDateFrom
        binding.tvDetailTo.text = visit.visitDateTo

        binding.rowDetailVehicle.visibility = if (visit.vehicle.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvDetailVehicle.text = visit.vehicle

        binding.rowDetailReason.visibility = if (visit.reason.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvDetailReason.text = visit.reason

        binding.tvExpiryInfo.text = "Código vigente. Expirará el ${visit.visitDateTo}."

        binding.formContainer.visibility = View.GONE
        binding.resultContainer.visibility = View.VISIBLE
    }

    private fun resetForm() {
        binding.etVisitorName.setText("")
        binding.etVisitorDocument.setText("")
        binding.etVisitDateFrom.setText("")
        binding.etVisitDateTo.setText("")
        binding.etVehicle.setText("")
        binding.etReason.setText("")
        binding.etApartment.setText(Session.CURRENT_APARTMENT)
        fromCalendar = null
        toCalendar = null
        generatedVisit = null
        generatedQrBitmap = null
        setupVisitTypeChips()

        binding.resultContainer.visibility = View.GONE
        binding.formContainer.visibility = View.VISIBLE
    }

    private fun shareQrCode() {
        val visit = generatedVisit ?: return
        val bitmap = generatedQrBitmap ?: return

        val qrFile = saveBitmapToCache(bitmap) ?: run {
            showError("No se pudo preparar la imagen para compartir.")
            return
        }

        val qrUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            qrFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, qrUri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Código de acceso para ${visit.visitorName} (${visit.visitDateFrom} - ${visit.visitDateTo})."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir código de acceso"))
    }

    /** Guarda el bitmap del QR como archivo temporal en caché para poder compartirlo. */
    private fun saveBitmapToCache(bitmap: Bitmap): File? {
        return try {
            val qrDir = File(cacheDir, "qr_codes").apply { mkdirs() }
            val file = File(qrDir, "qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            null
        }
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
