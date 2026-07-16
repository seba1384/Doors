package com.example.doors.data

/**
 * Registro de cada escaneo realizado en el portón,
 * sea válido o rechazado. Colección "accesos" en Firestore.
 */
data class AccessLog(
    val qrId: String = "",
    val granted: Boolean = false,
    val visitorName: String = "",
    // Departamento asociado a la visita (grupo familiar). Vacío si el QR
    // era inválido y no correspondía a ninguna visita real.
    val apartment: String = "",
    // "Entrada" | "Salida" | "" (vacío para intentos rechazados o apertura manual).
    val movement: String = "",
    val timestamp: Long = 0L
)
