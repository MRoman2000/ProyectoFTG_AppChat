package com.paquete.proyectoftg_appchat.utils

import android.content.ContentValues
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FirebaseUtils {

    companion object {
        private val db: FirebaseFirestore by lazy {
            FirebaseFirestore.getInstance()
        }

        private val firebaseAuth = lazy { FirebaseAuth.getInstance() }

        fun getFirestoreInstance(): FirebaseFirestore {
            return db
        }

        fun getCurrentUserId(): String? {
            return firebaseAuth.value.currentUser?.uid
        }

        fun deleteTokenFCM() {
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "Token FCM eliminado con éxito")
                } else {
                    Log.e(ContentValues.TAG, "Error al eliminar el token FCM", task.exception)
                }
            }
        }

        fun getOtrosUser(userId: List<String?>?): DocumentReference? {
            return if (userId?.get(0) == getCurrentUserId()) {
                userId?.get(1)?.let { allusers()?.document(it) }
            } else {
                userId?.get(0)?.let { allusers()?.document(it) }
            }
        }

        fun getOtherProfileStorage(otros: String?): StorageReference? {
            return FirebaseStorage.getInstance().reference.child("profile_user").child(otros!!)
        }

        fun allusers(): CollectionReference? {
            return db.collection("usuarios")
        }

        fun getFCMToken(userId: String): Task<DocumentSnapshot> {
            val userRef = db.collection("usuarios").document(userId)
            return userRef.get()
        }

        /**
         * Cierra la sesión del usuario actual.
         */
        fun signOut() {
            firebaseAuth.value.signOut()
        }
        fun eliminarTokenAlCerrarSesion() {
            val currentUser = firebaseAuth.value.currentUser
            currentUser?.let { user ->
                val uidUsuarioActual = user.uid
                val firestore = FirebaseFirestore.getInstance()
                val usuarioRef = firestore.collection("usuarios").document(uidUsuarioActual)

                // Eliminar el token FCM del documento del usuario
                usuarioRef.update("fcmToken", null).addOnSuccessListener {
                    // Éxito al eliminar el token
                    // Aquí puedes realizar cualquier acción adicional si es necesario
                }.addOnFailureListener { e ->
                    // Error al eliminar el token
                    // Manejar el error según sea necesario
                }
            }
        }
    }


}


