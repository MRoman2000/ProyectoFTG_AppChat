package com.paquete.proyectoftg_appchat.model

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Contactos(
    var id: String?,
    var nombre: String?,
    var numero: String?,
    var email: String?,
    var registradoEnFirestore: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(nombre)
        parcel.writeString(numero)
        parcel.writeString(email)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Contactos> {
        override fun createFromParcel(parcel: Parcel): Contactos {
            return Contactos(parcel)
        }

        override fun newArray(size: Int): Array<Contactos?> {
            return arrayOfNulls(size)
        }


        suspend fun obtenerContactos(contexto: Context): List<Contactos>? {
            return try {
                // Código propenso a errores
                val contentResolver = contexto.contentResolver
                withContext(Dispatchers.IO) {
                    val contactos = mutableListOf<Contactos>()
                    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    val selection = null
                    val selectionArgs = null
                    val sortOrder = null
                    contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder)?.use { cursor ->
                        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                        while (cursor.moveToNext()) {
                            val id = cursor.getString(idIndex)
                            val name = cursor.getString(nameIndex)
                            val number = cursor.getString(numberIndex)
                            val photo = cursor.getString(photoIndex)
                            contactos.add(Contactos(id, name, number, photo))
                        }
                    }
                    contactos
                }
            } catch (e: Exception) {
                // Manejo de la excepción
                Log.e(TAG, "Error: ${e.message}", e)
                null
            }
        }

    }
}