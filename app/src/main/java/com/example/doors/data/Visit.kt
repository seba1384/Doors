package com.example.doors.data

/**
 * Representa una visita registrada por un residente.
 * El [id] es el mismo valor que se codifica dentro del QR,
 * y también es el ID del documento en Firestore (colección "visitas").
 *
 * El QR ya no es "estático": cada visita define una ventana de tiempo
 * ([visitDateFrom] -> [visitDateTo]) y esa misma hora de término se guarda
 * en [expiresAtMillis]. Al validar el QR en portería se compara la hora
 * actual contra [expiresAtMillis]; si ya pasó, el acceso se rechaza aunque
 * el código nunca se haya usado. Sumado a esto, el campo [status] sigue
 * limitando el código a un solo uso (se marca "usado" apenas entra la visita).
 */
data class Visit(
    val id: String = "",
    val visitType: String = "Familiar o amigo",
    val visitorName: String = "",
    val visitorDocument: String = "",
    val apartment: String = "",
    // Ventana de la visita, formato "dd/MM/yyyy HH:mm"
    val visitDateFrom: String = "",
    val visitDateTo: String = "",
    val vehicle: String = "",
    val reason: String = "",
    // Epoch millis correspondiente a visitDateTo; 0 = sin vencimiento definido.
    val expiresAtMillis: Long = 0L,
    // "pendiente" -> aún no se ha usado el QR | "usado" -> ya se usó para ingresar
    val status: String = "pendiente",
    val createdAt: Long = System.currentTimeMillis()
)
