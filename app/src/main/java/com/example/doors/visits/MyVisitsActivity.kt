package com.example.doors.visits

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doors.data.QrGenerator
import com.example.doors.data.Visit
import com.example.doors.data.VisitRepository
import com.example.doors.databinding.ActivityMyVisitsBinding
import com.example.doors.databinding.DialogVisitQrBinding
import java.io.File
import java.io.FileOutputStream

/** Lista todas las visitas registradas por el residente, más recientes primero. */
class MyVisitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyVisitsBinding
    private val repository = VisitRepository()
    private val adapter = VisitsAdapter(onItemClick = { visit -> showVisitQrDialog(visit) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyVisitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvVisits.layoutManager = LinearLayoutManager(this)
        binding.rvVisits.adapter = adapter

        loadVisits()
    }

    private fun loadVisits() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        repository.getVisits(
            onResult = { visits ->
                binding.progressBar.visibility = View.GONE
                adapter.submitList(visits)
                binding.emptyState.visibility = if (visits.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        )
    }

    /** Vuelve a mostrar el QR de una visita ya registrada, por si se olvidó compartir. */
    private fun showVisitQrDialog(visit: Visit) {
        val dialogBinding = DialogVisitQrBinding.inflate(LayoutInflater.from(this))

        val qrBitmap = QrGenerator.generate(visit.id)
        dialogBinding.imgDialogQr.setImageBitmap(qrBitmap)
        dialogBinding.tvDialogVisitorName.text = visit.visitorName
        dialogBinding.tvDialogFrom.text = visit.visitDateFrom
        dialogBinding.tvDialogTo.text = visit.visitDateTo

        when (visit.status) {
            "usado" -> {
                dialogBinding.tvDialogStatus.text = "Código ya utilizado (entrada y salida registradas)"
                dialogBinding.tvDialogStatus.setTextColor(0xFFA8A0C4.toInt())
            }
            "dentro" -> {
                dialogBinding.tvDialogStatus.text = "Visitante dentro del condominio · queda 1 uso (salida)"
                dialogBinding.tvDialogStatus.setTextColor(0xFFFFA53D.toInt())
            }
            else -> {
                dialogBinding.tvDialogStatus.text = "Código vigente · permite entrada y salida"
                dialogBinding.tvDialogStatus.setTextColor(0xFF2ED573.toInt())
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnDialogClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnDialogShare.setOnClickListener { shareQrCode(visit, qrBitmap) }

        dialog.show()
    }

    private fun shareQrCode(visit: Visit, bitmap: Bitmap) {
        val qrFile = saveBitmapToCache(bitmap) ?: return
        val qrUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", qrFile)

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

    override fun onResume() {
        super.onResume()
        loadVisits()
    }
}
