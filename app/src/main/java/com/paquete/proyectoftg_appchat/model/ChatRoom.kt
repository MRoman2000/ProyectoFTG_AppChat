package com.paquete.proyectoftg_appchat.model

import com.google.firebase.Timestamp

data class ChatRoom
    (var chatroomId: String? = null,
    var userIds: List<String?>? = null,
    var lastMessageTimestamp: Timestamp? = null,
    var lastMessageSenderId: String? = null,
    var lastMessage: String? = null)
