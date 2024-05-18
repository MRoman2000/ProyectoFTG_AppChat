package com.paquete.proyectoftg_appchat.actividades

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.data.ViewModel
import com.paquete.proyectoftg_appchat.databinding.ActivityMainBinding
import com.paquete.proyectoftg_appchat.fragmentos.ChatRoomFragment
import com.paquete.proyectoftg_appchat.fragmentos.ConfiguracionFragment
import com.paquete.proyectoftg_appchat.fragmentos.ContactosFragment
import com.paquete.proyectoftg_appchat.fragmentos.MessageFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.SharePreference
import com.paquete.proyectoftg_appchat.utils.UserStatusService
import com.paquete.proyectoftg_appchat.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var elementosViewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //       enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val themeManager = SharePreference(this)

        themeManager.applySavedTheme()
   //     askNotificationPermission()

        val isDarkMode = isDarkModeEnabled()
        // Configura el color de la barra de navegación según el modo
        val navigationBarColor = if (isDarkMode) {
            ContextCompat.getColor(this, R.color.dark_primary_blue)
        } else {
            ContextCompat.getColor(this, R.color.white)
        }
        window.navigationBarColor = navigationBarColor

        /*    val statusBarColor = if (isDarkMode) {
                ContextCompat.getColor(this, R.color.dark_primary_blue)
            } else {
                ContextCompat.getColor(this, R.color.my_primary_fixed)
            }
            window.statusBarColor = statusBarColor */

        intent?.let {
            val fragment = it.getStringExtra("fragment")
            if (fragment == "message") {
                val messageFragment = MessageFragment()
                val senderId = it.getStringExtra("senderId")
                // Verificar que el senderId no sea nulo
                senderId?.let { id ->
                    FirebaseUtils.allusers().document(id).get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val userData = documentSnapshot.toObject(DataUser::class.java)
                            val channelId = generateChatroomId(userData?.uid.toString(), FirebaseUtils.getCurrentUserId().toString())
                            val bundle = Bundle().apply {
                                putParcelable("dataUser", userData)
                                putString("channelId", channelId)
                                putString("recipientId", userData?.uid)
                                putString("nombreRemitente", userData?.nombreCompleto)
                            }
                            messageFragment.arguments = bundle
                            Utils.navigateToFragment(this, messageFragment)
                            Log.d("MainActivity", "Opening MessageFragment")
                            // Limpiar la caché aquí
                            it.removeExtra("fragment")
                            it.removeExtra("senderId")
                        } else {
                            // El documento del usuario no existe
                            Log.e("MainActivity", "User document does not exist")
                        }
                    }.addOnFailureListener { exception ->
                        // Manejar errores al obtener la información del remitente desde Firebase
                        Log.e("MainActivity", "Failed to get user data: ${exception.message}")
                    }
                } ?: run {
                    // El senderId es null, maneja este caso si es necesario
                    Log.e("MainActivity", "SenderId is null")
                }
            }
        }


        elementosViewModel = ViewModelProvider(this)[ViewModel::class.java]
        val chatFragment = ChatRoomFragment()
        val contactos = ContactosFragment()
        val configuracion = ConfiguracionFragment()

        lifecycleScope.launch {
            addNewItem()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                // Guardar el token FCM en Firestore
                guardarTokenFCM(token)
                Log.e("FCM", "$token")
            } else {
                Log.e("FCM", "Error al obtener el token FCM", task.exception)
            }
        }


        // Verifica si la actividad se está recreando debido a un cambio de configuración
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, chatFragment).commit()
            binding.bottomNavigation.selectedItemId = R.id.Chat
        }
        setSupportActionBar(binding.materialToolbarMain)
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

    private fun generateChatroomId(sender: String, receiver: String): String {
        val participants = listOf(sender, receiver).sorted()
        return participants.joinToString("")
    }


    private fun askNotificationPermission() {
        Log.d("DEBUG", "askNotificationPermission() called")
        // Verificar si ya se han concedido los permisos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Log.d("DEBUG", "WAKE_LOCK permission already granted")
            // Permisos ya concedidos
        } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
            Log.d("DEBUG", "Requesting WAKE_LOCK permission")
            // Solicitar permisos
            /*   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                   requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
               } */
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
            val usuarioActual = FirebaseAuth.getInstance().currentUser?.uid
            Log.d("user", "$usuarioActual")
            usuarioActual?.let { user ->
                val usuarioRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(usuarioActual)
                val tokenUpdate = hashMapOf<String, Any>("fcmToken" to fcmToken)
                usuarioRef.update(tokenUpdate).addOnSuccessListener {
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
                val elemento = DataUser(uidUsuarioActual, email, nombreCompleto, nombreUsuario, telefono, url_image, "", "")
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
        Utils.updateUserStatusOffline()
        stopService(Intent(this, UserStatusService::class.java))
    }


}



