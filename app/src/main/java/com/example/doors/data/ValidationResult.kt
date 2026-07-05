package com.example.doors.data

/** Resultado de validar un QR escaneado contra la base de datos. */
sealed class ValidationResult {
    data class Granted(val visit: Visit) : ValidationResult()
    data class AlreadyUsed(val visit: Visit) : ValidationResult()
    object Invalid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
