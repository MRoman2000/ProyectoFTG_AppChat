package com.paquete.proyectoftg_appchat.actividades.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.actividades.MainActivity
import kotlin.random.Random

private const val CHANNEL_ID = "my_channel"

class MyFirebaseMessagingService : FirebaseMessagingService() {


    companion object {
        var sharedPref: SharedPreferences? = null

        var token: String?
            get() {
                return sharedPref?.getString("token", "")
            }
            set(value) {
                sharedPref?.edit()?.putString("token", value)?.apply()
            }

    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        token = newToken
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val phoneNumber = message.data["title"] // Número de teléfono del remitente

        // Verificar si el número de teléfono está en la lista de contactos y obtener el nombre del contacto si está disponible
        val contactName = getContactNameByPhoneNumber(phoneNumber)

        // Si el nombre del contacto está disponible, lo mostramos, de lo contrario, mostramos el número de teléfono
        val notificationTitle = contactName ?: phoneNumber

        val intent = Intent(this, MainActivity::class.java)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random.nextInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(notificationTitle).setContentText(message.data["message"])
                .setSmallIcon(R.drawable.ic_chat).setAutoCancel(true).setContentIntent(pendingIntent).build()

        Log.d("message1", "${message.data}")
        notificationManager.notify(notificationID, notification)
    }

    fun getContactNameByPhoneNumber(phoneNumber: String?): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val resolver = contentResolver
        val cursor = resolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)

        cursor?.use { c ->
            if (c.moveToFirst()) {
                val nameIndex = c.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                return c.getString(nameIndex)
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply {
            description = "My channel description"
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }

}