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
     * Un ID de visita/QR válido generado por esta app es un UUID: solo
     * letras, números y guiones, sin "/" ni espacios. Cualquier otra cosa
     * (una URL, texto random, un QR de otra app) se descarta ACÁ, antes de
     * tocar Firestore. Esto es clave porque un "/" dentro del ID rompe la
     * ruta del documento (Firestore lo interpreta como sub-colecciones) y
     * eso lanzaba una excepción no controlada que cerraba la app al
     * escanear códigos que no eran nuestros.
     */
    private fun isPlausibleVisitId(content: String): Boolean {
        if (content.isBlank()) return false
        if (content.length > 200) return false
        val allowedPattern = Regex("^[A-Za-z0-9-]+$")
        return allowedPattern.matches(content)
    }

    /**
     * Valida el contenido leído de un QR contra la base de datos.
     * Reglas:
     *  - Si el formato ni siquiera parece un ID de visita (por ejemplo, viene
     *    de un QR ajeno a la app, una URL, etc.) -> inválido, sin consultar Firestore.
     *  - Si el documento no existe -> QR inválido (nunca se generó desde la app).
     *  - Si existe pero ya alcanzó su límite de usos -> ya se usó antes.
     *  - Si existe pero ya pasó su [Visit.expiresAtMillis] -> venció, aunque nunca se haya usado.
     *  - Si existe, tiene usos disponibles y no ha vencido -> acceso concedido.
     *    El primer uso cuenta como "Entrada" y el segundo como "Salida".
     * Cada intento (válido o no) se registra en el historial de accesos.
     * Cualquier error inesperado (de red, de formato, etc.) se captura y se
     * informa como [ValidationResult.Error] en vez de dejar que la app se cierre.
     */
    fun validateQr(
        qrContent: String,
        onResult: (ValidationResult) -> Unit
    ) {
        try {
            if (!isPlausibleVisitId(qrContent)) {
                logAccess(qrId = qrContent.take(120), granted = false, visitorName = "Código no reconocido", apartment = "")
                onResult(ValidationResult.Invalid)
                return
            }

            visitsCollection.document(qrContent)
                .get()
                .addOnSuccessListener { doc ->
                    try {
                        val visit = if (doc.exists()) doc.toObject(Visit::class.java) else null
                        val isExpired = visit != null &&
                            visit.expiresAtMillis > 0 &&
                            System.currentTimeMillis() > visit.expiresAtMillis

                        when {
                            visit == null -> {
                                logAccess(qrContent, granted = false, visitorName = "Desconocido", apartment = "")
                                onResult(ValidationResult.Invalid)
                            }
                            visit.usesCount >= visit.maxUses -> {
                                logAccess(qrContent, granted = false, visitorName = visit.visitorName, apartment = visit.apartment)
                                onResult(ValidationResult.AlreadyUsed(visit))
                            }
                            isExpired -> {
                                logAccess(qrContent, granted = false, visitorName = visit.visitorName, apartment = visit.apartment)
                                onResult(ValidationResult.Expired(visit))
                            }
                            else -> {
                                val newUsesCount = visit.usesCount + 1
                                val movement = if (newUsesCount >= visit.maxUses) "Salida" else "Entrada"
                                val newStatus = if (newUsesCount >= visit.maxUses) "usado" else "dentro"

                                visitsCollection.document(qrContent)
                                    .update(mapOf("usesCount" to newUsesCount, "status" to newStatus))

                                logAccess(
                                    qrContent,
                                    granted = true,
                                    visitorName = visit.visitorName,
                                    apartment = visit.apartment,
                                    movement = movement
                                )
                                onResult(
                                    ValidationResult.Granted(
                                        visit.copy(usesCount = newUsesCount, status = newStatus),
                                        movement
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        onResult(ValidationResult.Error(e.message ?: "No se pudo leer la visita"))
                    }
                }
                .addOnFailureListener { e ->
                    onResult(ValidationResult.Error(e.message ?: "Error al validar el QR"))
                }
        } catch (e: Exception) {
            // Red de seguridad final: cualquier excepción (por ejemplo, un ID con
            // caracteres que igual rompen la ruta de Firestore) nunca debe tumbar la app.
            onResult(ValidationResult.Invalid)
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

    private fun logAccess(
        qrId: String,
        granted: Boolean,
        visitorName: String,
        apartment: String,
        movement: String = ""
    ) {
        val log = AccessLog(
            qrId = qrId,
            granted = granted,
            visitorName = visitorName,
            apartment = apartment,
            movement = movement,
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
                            val movementLabel = if (log.movement.isNotBlank()) log.movement.lowercase() else "ingreso"
                            NotificationItem(
                                type = NotificationType.ACCESS_GRANTED,
                                title = "Acceso concedido",
                                message = "${log.visitorName} registró su $movementLabel al condominio.",
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
