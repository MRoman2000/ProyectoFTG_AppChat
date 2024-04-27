package com.paquete.proyectoftg_appchat.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager {

    companion object {
        private const val REQUEST_CONTACT_PERMISSIONS = 1001
    }

    fun requestContactPermissions(activity: Activity) {
        val readPermission = Manifest.permission.READ_CONTACTS
        val writePermission = Manifest.permission.WRITE_CONTACTS

        val readPermissionGranted = ContextCompat.checkSelfPermission(activity, readPermission) == PackageManager.PERMISSION_GRANTED
        val writePermissionGranted = ContextCompat.checkSelfPermission(activity, writePermission) == PackageManager.PERMISSION_GRANTED

        if (!readPermissionGranted || !writePermissionGranted) {
            // Si alguno de los permisos no está otorgado, solicita los permisos
            ActivityCompat.requestPermissions(activity, arrayOf(readPermission, writePermission), REQUEST_CONTACT_PERMISSIONS)
        }
    }

    fun handlePermissionsResult(context: Context, requestCode: Int, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CONTACT_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Utils.showMessage(context , "Notificaciones activadas")
                } else {
                    // Aquí puedes manejar el caso cuando el permiso es denegado
                }
            }
        }
    }
}

