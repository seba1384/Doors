package com.example.doors.data

/**
 * Datos del residente que tiene la sesión abierta.
 *
 * TODO (login real): reemplazar estos valores fijos por los datos reales
 * del usuario autenticado (Firebase Auth / Firestore). El [CURRENT_APARTMENT]
 * es lo que usamos como identificador del "grupo familiar": todas las
 * visitas y accesos de ese departamento se consideran del mismo grupo.
 */
object Session {
    const val CURRENT_RESIDENT_NAME = "Valentina Soto"
    const val CURRENT_APARTMENT = "402"

    // TODO: mover esto a Firestore / Remote Config cuando exista gestión real
    // de conserjería, en vez de dejarlo fijo en el código.
    const val ADMIN_PIN = "1234"
}
