package com.example.doors.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Punto único de acceso a la base de datos (Firestore).
 *
 * Colecciones usadas:
 *  - "visitas": una visita por documento, el ID del documento es el mismo
 *    valor que va codificado dentro del QR.
 *  - "accesos": historial de cada intento de escaneo (válido o no).
 */
class VisitRepository {

    private val db = FirebaseFirestore.getInstance()
    private val visitsCollection = db.collection("visitas")
    private val accessCollection = db.collection("accesos")

    /** Crea una nueva visita en la base de datos. [visit.id] será el contenido del QR. */
    fun createVisit(
        visit: Visit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        visitsCollection.document(visit.id)
            .set(visit)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    /** Devuelve las visitas más recientes primero, para la pantalla "Mis visitas". */
    fun getVisits(
        onResult: (List<Visit>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        visitsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val visits = snapshot.documents.mapNotNull { it.toObject(Visit::class.java) }
                onResult(visits)
            }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Valida el contenido leído de un QR contra la base de datos.
     * Reglas:
     *  - Si el documento no existe -> QR inválido (nunca se generó desde la app).
     *  - Si existe pero ya está "usado" -> ya se usó antes (evita reingresos con el mismo QR).
     *  - Si existe y está "pendiente" -> acceso concedido, y se marca como "usado".
     * Cada intento (válido o no) se registra en el historial de accesos.
     */
    fun validateQr(
        qrContent: String,
        onResult: (ValidationResult) -> Unit
    ) {
        visitsCollection.document(qrContent)
            .get()
            .addOnSuccessListener { doc ->
                val visit = if (doc.exists()) doc.toObject(Visit::class.java) else null

                when {
                    visit == null -> {
                        logAccess(qrContent, granted = false, visitorName = "Desconocido")
                        onResult(ValidationResult.Invalid)
                    }
                    visit.status == "usado" -> {
                        logAccess(qrContent, granted = false, visitorName = visit.visitorName)
                        onResult(ValidationResult.AlreadyUsed(visit))
                    }
                    else -> {
                        visitsCollection.document(qrContent).update("status", "usado")
                        logAccess(qrContent, granted = true, visitorName = visit.visitorName)
                        onResult(ValidationResult.Granted(visit))
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(ValidationResult.Error(e.message ?: "Error al validar el QR"))
            }
    }

    /** Registra en el historial cuando el residente abre el portón manualmente desde la Home. */
    fun logManualGateOpening() {
        logAccess(qrId = "manual", granted = true, visitorName = "Apertura manual (residente)")
    }

    private fun logAccess(qrId: String, granted: Boolean, visitorName: String) {
        val log = AccessLog(
            qrId = qrId,
            granted = granted,
            visitorName = visitorName,
            timestamp = System.currentTimeMillis()
        )
        accessCollection.add(log)
    }

    /** Devuelve los últimos 50 accesos registrados, para "Historial de accesos". */
    fun getAccessHistory(
        onResult: (List<AccessLog>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        accessCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val logs = snapshot.documents.mapNotNull { it.toObject(AccessLog::class.java) }
                onResult(logs)
            }
            .addOnFailureListener { onError(it) }
    }
}
