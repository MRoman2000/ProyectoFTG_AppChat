package com.paquete.proyectoftg_appchat.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.paquete.proyectoftg_appchat.R
import java.util.Calendar


class Utils {
    companion object {
        fun navigateToFragment(activity: FragmentActivity, fragment: Fragment) {
            try {
                val transaction = activity.supportFragmentManager.beginTransaction()
                transaction.replace(R.id.main_frame_layout, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
            } catch (e: ClassCastException) {
                e.printStackTrace()
            }
        }


        fun updateUserStatusOnline() {
            val userId = FirebaseUtils.getCurrentUserId()
            val userRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(userId!!)
            userRef.update("estado", "online").addOnSuccessListener {
                Log.d(ContentValues.TAG, "Estado de usuario actualizado a online")
            }.addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Error al actualizar el estado del usuario a online", e)
            }
        }

        fun fetchVerificationCode(message: String): String {
            return Regex("(\\d{6})").find(message)?.value ?: ""
        }


        fun updateUserStatusOffline() {
            val userId = FirebaseUtils.getCurrentUserId()
            val currentTime = Calendar.getInstance().time

            userId?.let { uid ->
                val userRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(uid)
                val userData = hashMapOf(
                    "estado" to "offline",
                    "ultimaConexion" to currentTime,
                )
                userRef.update(userData as Map<String, Any>).addOnSuccessListener {
                        Log.d("Estado", "Estado de usuario actualizado a offline")
                    }.addOnFailureListener { e ->
                        Log.e("Error", "Error al actualizar el estado del usuario a offline", e)
                    }
            }
        }

        fun modificarContactoEnListaDeContactos(uidContacto: String, agregar: Boolean) {
            // Obtener la referencia al documento del usuario actual en Firestore
            val usuarioActual = FirebaseAuth.getInstance().currentUser
            usuarioActual?.let { currentUser ->
                val usuarioRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(currentUser.uid)

                // Actualizar la lista de contactos del usuario
                if (agregar) {
                    // Agregar el nuevo contacto a la lista de contactos
                    usuarioRef.update("contactos", FieldValue.arrayUnion(uidContacto)).addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Se agregó el nuevo contacto a la lista de contactos")
                        // Notificar al adaptador de que se ha actualizado la lista de contactos
                        // obtenerContactosFirebase()
                    }.addOnFailureListener { exception ->
                        Log.e(ContentValues.TAG, "Error al agregar el nuevo contacto a la lista de contactos", exception)
                    }
                } else {
                    // Eliminar el contacto de la lista de contactos
                    usuarioRef.update("contactos", FieldValue.arrayRemove(uidContacto)).addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Se eliminó el contacto de la lista de contactos")
                        // Notificar al adaptador de que se ha actualizado la lista de contactos
                        // obtenerContactosFirebase()
                    }.addOnFailureListener { exception ->
                        Log.e(ContentValues.TAG, "Error al eliminar el contacto de la lista de contactos", exception)
                    }
                }
            }
        }


        fun setProfilePic(context: Context?, imageUri: Uri?, imageView: ImageView?) {
            Glide.with(context!!).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageView!!)
        }

        fun showMessage(context: Context?, mensaje: String) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }
}

