package com.paquete.proyectoftg_appchat.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Contactos")
data class Contactos(
    @PrimaryKey val uid: String,
    val email: String? = null,
    val nombreCompleto: String? = null,
    val nombreUsuario: String? = null,
    val imageUrl: String? = null) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(email)
        parcel.writeString(nombreCompleto)
        parcel.writeString(nombreUsuario)
        parcel.writeString(imageUrl)
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