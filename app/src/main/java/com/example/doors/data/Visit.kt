package com.example.doors.data

/**
 * Representa una visita registrada por un residente.
 * El [id] es el mismo valor que se codifica dentro del QR,
 * y también es el ID del documento en Firestore (colección "visitas").
 */
data class Visit(
    val id: String = "",
    val visitType: String = "Familiar",
    val visitorName: String = "",
    val visitorDocument: String = "",
    val apartment: String = "",
    val visitDate: String = "",
    // "pendiente" -> aún no se ha usado el QR | "usado" -> ya se usó para ingresar
    val status: String = "pendiente",
    val createdAt: Long = System.currentTimeMillis()
)
