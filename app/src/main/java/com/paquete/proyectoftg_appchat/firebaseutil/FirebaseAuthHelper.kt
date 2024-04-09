package com.paquete.proyectoftg_appchat.firebaseutil

import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthHelper(private val auth: FirebaseAuth) {

    /**
     * Obtiene el ID del usuario actualmente autenticado.
     * @return El ID del usuario actual, o null si no hay usuario autenticado.
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }


    /**
     * Cierra la sesión del usuario actual.
     */
    fun signOut() {
        auth.signOut()
    }

}