package com.paquete.proyectoftg_appchat.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 Clase que representa la entidad de datos para la base de datos local
 */
@Entity(tableName = "DataUser")
data class DataUser(
    @PrimaryKey val uid: String,
    val email: String? = null,
    val fechaNacimiento: String? = null,
    val nombreCompleto: String? = null,
    val nombreUsuario: String? = null,
    val telefono: String? = null,
    val imageUrl: String? = null) {
    constructor() : this("")
}



