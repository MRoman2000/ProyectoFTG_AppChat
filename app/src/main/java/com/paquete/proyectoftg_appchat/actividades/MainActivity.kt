package com.paquete.proyectoftg_appchat.actividades

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.databinding.ActivityMainBinding
import com.paquete.proyectoftg_appchat.fragmentos.ChatRoomFragment
import com.paquete.proyectoftg_appchat.fragmentos.ConfiguracionFragment
import com.paquete.proyectoftg_appchat.fragmentos.ContactosFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.PermissionManager
import com.paquete.proyectoftg_appchat.utils.PermissionManager.requestContactPermissions
import com.paquete.proyectoftg_appchat.utils.UserStatusService
import com.paquete.proyectoftg_appchat.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var elementosViewModel: ElementosViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val isDarkMode = isDarkModeEnabled()
        // Configura el color de la barra de navegación según el modo
        val navigationBarColor = if (isDarkMode) {
            ContextCompat.getColor(this, R.color.dark_primary_blue)
        } else {
            ContextCompat.getColor(this, R.color.my_primary_fixed)
        }
        window.navigationBarColor = navigationBarColor


        val statusBarColor = if (isDarkMode) {
            ContextCompat.getColor(this, R.color.dark_primary_blue)
        } else {
            ContextCompat.getColor(this, R.color.my_primary_fixed)
        }
        window.statusBarColor = statusBarColor
        setSupportActionBar(binding.materialToolbar)

        elementosViewModel = ViewModelProvider(this)[ElementosViewModel::class.java]
        val chatFragment = ChatRoomFragment()
        val contactos = ContactosFragment()
        val configuracion = ConfiguracionFragment()

        requestContactPermissions(this)
        askNotificationPermission()
        lifecycleScope.launch {
            addNewItem()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                // Guardar el token FCM en Firestore
                guardarTokenFCM(token)
            } else {
                Log.e(TAG, "Error al obtener el token FCM", task.exception)
            }
        }


        // Verifica si la actividad se está recreando debido a un cambio de configuración
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, chatFragment).commit()
            binding.bottomNavigation.selectedItemId = R.id.Chat
        }



        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Chat -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, chatFragment).commit()
                }

                R.id.Contactos -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, contactos).commit()
                }

                R.id.Configuracion -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, configuracion).commit()
                }
            }
            true
        }
        startService(Intent(this, UserStatusService::class.java))

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    private fun askNotificationPermission() {
        // Verificar si ya se han concedido los permisos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Permisos ya concedidos
            // Aquí puedes realizar cualquier acción necesaria si los permisos ya están otorgados

        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
            // Solicitar permisos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Utils.showMessage(this, "Notificaciones activadas")
        } else {
            Utils.showMessage(this, "Notificaciones no activadas")
        }
    }


    private fun isDarkModeEnabled(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun guardarTokenFCM(token: String?) {
        // Verificar si el token no es nulo
        token?.let { fcmToken ->
            val firestore = FirebaseFirestore.getInstance()
            val usuarioActual = FirebaseAuth.getInstance().currentUser
            Log.d("user", "$usuarioActual")
            usuarioActual?.let { user ->
                val uidUsuarioActual = user.uid
                // Actualizar el token FCM en el documento del usuario en Firestore
                val usuarioRef = firestore.collection("usuarios").document(uidUsuarioActual)
                usuarioRef.update("fcmToken", fcmToken).addOnSuccessListener {
                    Log.d("Token", "Token FCM guardado en Firestore")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error al guardar el token FCM en Firestore", e)
                }
            }
        }
    }


    private suspend fun addNewItem() {
        val firestore = FirebaseFirestore.getInstance()
        val usuarioActual = FirebaseAuth.getInstance().currentUser
        if (usuarioActual != null) {
            val uidUsuarioActual = usuarioActual.uid
            val documentSnapshot = firestore.collection("usuarios").document(uidUsuarioActual).get().await()
            withContext(Dispatchers.IO) {
                val email = documentSnapshot.getString("email") ?: ""
                val nombreCompleto = documentSnapshot.getString("nombreCompleto") ?: ""
                val nombreUsuario = documentSnapshot.getString("nombreUsuario") ?: ""
                val telefono = documentSnapshot.getString("telefono") ?: ""
                val url_image = documentSnapshot.getString("imageUrl") ?: ""
                val elemento = DataUser(uidUsuarioActual, email, nombreCompleto, nombreUsuario, telefono, url_image, "")
                elementosViewModel.insertar(elemento)
            }
        } else {
            Log.e(TAG, "No hay usuario autenticado.")
        }
    }


    override fun onStart() {
        super.onStart()
        Utils.updateUserStatusOnline()
        stopService(Intent(this, UserStatusService::class.java))
    }

    override fun onStop() {
        super.onStop()
        Utils.updateUserStatusOffline()
        stopService(Intent(this, UserStatusService::class.java))
    }


    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, UserStatusService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handlePermissionsResult(requestCode, grantResults)
    }


}


