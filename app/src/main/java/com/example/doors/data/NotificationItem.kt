package com.example.doors.data

/** Tipo de evento que originó la notificación. */
enum class NotificationType {
    ACCESS_GRANTED,
    ACCESS_DENIED,
    QR_CREATED
}

data class NotificationItem(
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long
)
