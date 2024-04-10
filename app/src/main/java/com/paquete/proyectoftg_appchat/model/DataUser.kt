package com.paquete.proyectoftg_appchat.model

/*
 Clase que representa la entidad de datos para la base de datos local
 */
import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DataUser")
data class DataUser(
    @PrimaryKey val uid: String,
    val email: String? = null,
    val fechaNacimiento: String? = null,
    val nombreCompleto: String? = null,
    val nombreUsuario: String? = null,
    val telefono: String? = null,
    val imageUrl: String? = null,
    val estado: String = "offline" // Nuevo campo para el estado del usuario
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(email)
        parcel.writeString(fechaNacimiento)
        parcel.writeString(nombreCompleto)
        parcel.writeString(nombreUsuario)
        parcel.writeString(telefono)
        parcel.writeString(imageUrl)
        parcel.writeString(estado)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DataUser> {
        override fun createFromParcel(parcel: Parcel): DataUser {
            return DataUser(parcel)
        }

        override fun newArray(size: Int): Array<DataUser?> {
            return arrayOfNulls(size)
        }
    }

    constructor() : this("")
}





