package com.example.doors.data

/**
 * Registro de cada escaneo realizado en el portón,
 * sea válido o rechazado. Colección "accesos" en Firestore.
 */
data class AccessLog(
    val qrId: String = "",
    val granted: Boolean = false,
    val visitorName: String = "",
    val timestamp: Long = 0L
)
