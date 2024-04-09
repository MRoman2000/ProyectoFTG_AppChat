package com.paquete.proyectoftg_appchat.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
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
            return FirebaseFirestore.getInstance().collection("usuarios")
        }

        /**
         * Cierra la sesión del usuario actual.
         */
        fun signOut() {
            firebaseAuth.value.signOut()
        }
    }
}


