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
 * el código nunca se haya usado.
 *
 * El código permite hasta [maxUses] escaneos válidos: el primero cuenta
 * como "Entrada" y el segundo como "Salida" (para que la misma visita
 * pueda entrar y salir del condominio con el mismo QR). [usesCount] lleva
 * la cuenta de cuántas veces ya se usó.
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
    // Cuántas veces se ha escaneado válidamente este QR (0, 1 o 2).
    val usesCount: Int = 0,
    // Cuántos escaneos válidos permite en total: 1 = entrada, 2 = entrada + salida.
    val maxUses: Int = 2,
    // "pendiente" (0 usos) -> "dentro" (1 uso, entró pero no ha salido) -> "usado" (2 usos, entrada y salida ya registradas)
    val status: String = "pendiente",
    val createdAt: Long = System.currentTimeMillis()
)
