package com.paquete.proyectoftg_appchat.notifications


data class PushNotification(
    val data: NotificationData,
    val to: String
)