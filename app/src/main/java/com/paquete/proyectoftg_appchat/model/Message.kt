package com.paquete.proyectoftg_appchat.model

import com.google.firebase.Timestamp


class Message {
    var message: String? = null
    var senderId: String? = null
    var timestamp: Timestamp? = null
    var imageUrl: String? = null // Cambiado de Uri a String para almacenar la URL de la imagen

    constructor() {}

    constructor(message: String?, senderId: String?, timestamp: Timestamp) {
        this.message = message
        this.senderId = senderId
        this.timestamp = timestamp
    }

    constructor(message: String?, senderId: String?, timestamp: Timestamp, imageUrl: String?) {
        this.message = message
        this.senderId = senderId
        this.timestamp = timestamp
        this.imageUrl = imageUrl
    }
}

