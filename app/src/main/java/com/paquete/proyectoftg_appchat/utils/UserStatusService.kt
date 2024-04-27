package com.paquete.proyectoftg_appchat.utils

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.IBinder
import android.util.Log

class UserStatusService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateUserStatusOnline() // Actualiza el estado del usuario cuando se inicia el servicio
        return START_STICKY // Esto hace que el servicio se reinicie si se detiene
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        updateUserStatusOffline()
    }

    private fun updateUserStatusOnline() {
        val userId = FirebaseUtils.getCurrentUserId()
        val userRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(userId!!)
        userRef.update("estado", "online").addOnSuccessListener {
                Log.d(ContentValues.TAG, "Estado de usuario actualizado a online")
            }.addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Error al actualizar el estado del usuario a online", e)
            }
    }

    private fun updateUserStatusOffline() {
        val userId = FirebaseUtils.getCurrentUserId()
        val userRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(userId!!)
        userRef.update("estado", "offline").addOnSuccessListener {
                Log.d(ContentValues.TAG, "Estado de usuario actualizado a offline")
            }.addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Error al actualizar el estado del usuario a offline", e)
            }
    }
}
