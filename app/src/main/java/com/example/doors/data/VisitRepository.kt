package com.example.doors.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Query

/**
 * Punto único de acceso a la base de datos (Firestore).
 *
 * Colecciones usadas:
 *  - "visitas": una visita por documento, el ID del documento es el mismo
 *    valor que va codificado dentro del QR.
 *  - "accesos": historial de cada intento de escaneo (válido o no).
 *
 * El "grupo familiar" se identifica por el número de departamento
 * ([Session.CURRENT_APARTMENT]). La vista del residente solo debe ver lo
 * de su propio departamento; la vista de administración ve todo.
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

    /** Devuelve las visitas del grupo familiar actual, más recientes primero. */
    fun getVisits(
        onResult: (List<Visit>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        visitsCollection
            .whereEqualTo("apartment", Session.CURRENT_APARTMENT)
            .get()
            .addOnSuccessListener { snapshot ->
                val visits = snapshot.documents
                    .mapNotNull { it.toObject(Visit::class.java) }
                    .sortedByDescending { it.createdAt }
                onResult(visits)
            }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Valida el contenido leído de un QR contra la base de datos.
     * Reglas:
     *  - Si el documento no existe -> QR inválido (nunca se generó desde la app).
     *  - Si existe pero ya está "usado" -> ya se usó antes (evita reingresos con el mismo QR).
     *  - Si existe pero ya pasó su [Visit.expiresAtMillis] -> venció, aunque nunca se haya usado.
     *  - Si existe, está "pendiente" y no ha vencido -> acceso concedido, y se marca como "usado".
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
                val isExpired = visit != null &&
                    visit.expiresAtMillis > 0 &&
                    System.currentTimeMillis() > visit.expiresAtMillis

                when {
                    visit == null -> {
                        logAccess(qrContent, granted = false, visitorName = "Desconocido", apartment = "")
                        onResult(ValidationResult.Invalid)
                    }
                    visit.status == "usado" -> {
                        logAccess(qrContent, granted = false, visitorName = visit.visitorName, apartment = visit.apartment)
                        onResult(ValidationResult.AlreadyUsed(visit))
                    }
                    isExpired -> {
                        logAccess(qrContent, granted = false, visitorName = visit.visitorName, apartment = visit.apartment)
                        onResult(ValidationResult.Expired(visit))
                    }
                    else -> {
                        visitsCollection.document(qrContent).update("status", "usado")
                        logAccess(qrContent, granted = true, visitorName = visit.visitorName, apartment = visit.apartment)
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
        logAccess(
            qrId = "manual",
            granted = true,
            visitorName = "Apertura manual (residente)",
            apartment = Session.CURRENT_APARTMENT
        )
    }

    private fun logAccess(qrId: String, granted: Boolean, visitorName: String, apartment: String) {
        val log = AccessLog(
            qrId = qrId,
            granted = granted,
            visitorName = visitorName,
            apartment = apartment,
            timestamp = System.currentTimeMillis()
        )
        accessCollection.add(log)
    }

    /**
     * Devuelve el historial de accesos.
     * @param apartment si se especifica, filtra solo los accesos de ese departamento
     * (vista del residente / grupo familiar). Si es null, devuelve todo (vista de administración).
     */
    fun getAccessHistory(
        apartment: String? = null,
        onResult: (List<AccessLog>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val query: Query = if (apartment != null) {
            accessCollection.whereEqualTo("apartment", apartment)
        } else {
            accessCollection.orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
        }
        query.get()
            .addOnSuccessListener { snapshot ->
                val logs = snapshot.documents
                    .mapNotNull { it.toObject(AccessLog::class.java) }
                    .sortedByDescending { it.timestamp }
                onResult(logs)
            }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Cuenta, para el departamento actual, cuántos escaneos de QR fueron
     * aprobados y cuántos fueron rechazados (código inválido, ya usado o
     * vencido). Se excluye la apertura manual del portón ("manual"),
     * ya que no corresponde a un QR generado por el residente.
     */
    fun getAccessStats(
        apartment: String,
        onResult: (approved: Int, rejected: Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        accessCollection
            .whereEqualTo("apartment", apartment)
            .get()
            .addOnSuccessListener { snapshot ->
                val logs = snapshot.documents.mapNotNull { it.toObject(AccessLog::class.java) }
                    .filter { it.qrId != "manual" }
                val approved = logs.count { it.granted }
                val rejected = logs.count { !it.granted }
                onResult(approved, rejected)
            }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Combina accesos + visitas creadas del grupo familiar en una sola lista
     * de notificaciones, ordenada por fecha descendente.
     */
    fun getNotifications(
        apartment: String,
        onResult: (List<NotificationItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val accessTask = accessCollection.whereEqualTo("apartment", apartment).get()
        val visitsTask = visitsCollection.whereEqualTo("apartment", apartment).get()

        Tasks.whenAllSuccess<QuerySnapshot>(accessTask, visitsTask)
            .addOnSuccessListener { results ->
                val accessSnapshot = results[0]
                val visitsSnapshot = results[1]
                val notifications = mutableListOf<NotificationItem>()

                accessSnapshot.documents.forEach { doc ->
                    val log = doc.toObject(AccessLog::class.java) ?: return@forEach
                    if (log.qrId == "manual") return@forEach // no es un evento relevante de notificar
                    notifications.add(
                        if (log.granted) {
                            NotificationItem(
                                type = NotificationType.ACCESS_GRANTED,
                                title = "Acceso concedido",
                                message = "${log.visitorName} ingresó al condominio.",
                                timestamp = log.timestamp
                            )
                        } else {
                            NotificationItem(
                                type = NotificationType.ACCESS_DENIED,
                                title = "Acceso rechazado",
                                message = "Se rechazó un intento de ingreso (${log.visitorName}).",
                                timestamp = log.timestamp
                            )
                        }
                    )
                }

                visitsSnapshot.documents.forEach { doc ->
                    val visit = doc.toObject(Visit::class.java) ?: return@forEach
                    notifications.add(
                        NotificationItem(
                            type = NotificationType.QR_CREATED,
                            title = "Código QR generado",
                            message = "Creaste una visita de tipo ${visit.visitType} para ${visit.visitorName}.",
                            timestamp = visit.createdAt
                        )
                    )
                }

                onResult(notifications.sortedByDescending { it.timestamp })
            }
            .addOnFailureListener { onError(it) }
    }
}
