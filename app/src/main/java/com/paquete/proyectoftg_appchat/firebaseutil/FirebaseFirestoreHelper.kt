package com.paquete.proyectoftg_appchat.firebaseutil

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot


class FirebaseFirestoreHelper(private val firestore: FirebaseFirestore) {

    // Método para obtener una referencia a una colección específica
    fun obtenerColeccion(nombreColeccion: String): CollectionReference {
        return firestore.collection(nombreColeccion)
    }

    // Método para agregar un nuevo documento a una colección
    fun agregarDocumento(coleccion: CollectionReference, datos: Any): Task<DocumentReference> {
        return coleccion.add(datos)
    }

    // Método para actualizar un documento existente en una colección
    fun actualizarDocumento(documento: DocumentReference, datos: Map<String, Any>): Task<Void> {
        return documento.update(datos)
    }

    // Método para eliminar un documento de una colección
    fun eliminarDocumento(documento: DocumentReference): Task<Void> {
        return documento.delete()
    }

    // Método para obtener un documento específico de una colección
    fun obtenerDocumento(documento: DocumentReference): Task<DocumentSnapshot> {
        return documento.get()
    }

    // Método para realizar una consulta en una colección
    fun realizarConsulta(coleccion: CollectionReference, consulta: Query): Task<QuerySnapshot> {
        return consulta.get()
    }


}