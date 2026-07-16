package com.example.doors.data

/** Resultado de validar un QR escaneado contra la base de datos. */
sealed class ValidationResult {
    /** [movement] indica si este escaneo fue una "Entrada" o una "Salida". */
    data class Granted(val visit: Visit, val movement: String) : ValidationResult()
    data class AlreadyUsed(val visit: Visit) : ValidationResult()
    data class Expired(val visit: Visit) : ValidationResult()
    object Invalid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
