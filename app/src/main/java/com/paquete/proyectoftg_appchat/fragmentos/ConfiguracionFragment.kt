package com.paquete.proyectoftg_appchat.fragmentos

import android.Manifest
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.ktx.storage
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.actividades.SplashActivity
import com.paquete.proyectoftg_appchat.data.AppDatabase
import com.paquete.proyectoftg_appchat.databinding.FragmentConfiguracionBinding
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.ProfileFragment
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.SecurityFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils.Companion.signOut
import com.paquete.proyectoftg_appchat.utils.Utils
import com.paquete.proyectoftg_appchat.utils.Utils.Companion.updateUserStatusOffline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class ConfiguracionFragment : Fragment() {
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }
    private var userData: DataUser? = null
    private val PREFS_KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private val PREFS_KEY_THEME = "theme_preference"
    private var _binding: FragmentConfiguracionBinding? = null
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private lateinit var url: Uri

    private val binding get() = _binding!!


    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            saveImageUrlToFirebase(url)
            Toast.makeText(requireContext(), "Foto capturada con éxito", Toast.LENGTH_SHORT).show()
        } else {
            // Hubo un error al capturar la foto
            Toast.makeText(requireContext(), "Error al capturar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { url ->
        if (url != null) {
            saveImageUrlToFirebase(url)
        }
    }
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // El usuario otorgó permiso de cámara, puedes proceder a capturar la foto
                takePicture.launch(url)
            } else {
                // El usuario denegó el permiso de cámara, muestra un mensaje o realiza alguna acción apropiada
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Configuracion"
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        val usuarioActual = FirebaseUtils.getCurrentUserId()

        url = createImageUri()

        userData?.let {
            loadImageFromUrl(it.imageUrl.toString(), binding.imagenPerfil)
            binding.textNombreCompleto.text = it.nombreCompleto ?: ""
        }

        elementosViewModel.obtenerDatosYElementoUsuarioActual(usuarioActual).observe(viewLifecycleOwner) { datauser ->
            datauser?.let {
                userData = it
                loadImageFromUrl(it.imageUrl.toString(), binding.imagenPerfil)
                binding.textNombreCompleto.text = it.nombreCompleto ?: ""
            }
        }



        binding.layoutProfile.setOnClickListener {
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putParcelable("userData", userData)
            profileFragment.arguments = bundle
            Utils.navigateToFragment(requireActivity(), profileFragment)
        }

        binding.switchNotification.isChecked = areNotificationsEnabled()

        binding.layoutNotification.setOnClickListener {
            val intent = Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)
            }

            startActivity(intent)

        }

        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setNotificationsEnabled(true)
            } else {
                setNotificationsEnabled(false)
            }
        }

        binding.layoutAparencia.setOnClickListener {
            cambiarTema()
        }

        binding.layoutSecurity.setOnClickListener {
            val security = SecurityFragment()
            Utils.navigateToFragment(requireActivity(), security)
        }

        binding.editFoto.setOnClickListener {
            showOptionsDialog()

        }

        binding.layoutLogout.setOnClickListener {
            cerrarSesion()
        }
    }


    private fun showOptionsDialog() {
        val options = arrayOf<CharSequence>("Tomar foto", "Elegir de la galería", "Cancelar")
        MaterialAlertDialogBuilder(requireActivity()).setTitle("Elige una opción").setItems(options) { _, item ->
            when (options[item]) {
                "Tomar foto" -> requestCameraPermission()
                "Elegir de la galería" -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }.show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // El permiso de cámara ya está otorgado, puedes proceder a capturar la foto
            takePicture.launch(url)
        } else {
            // El permiso de cámara aún no se ha otorgado, solicita el permiso al usuario
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun cambiarTema() {
        val options = arrayOf("Oscuro", "Claro", "Predeternimado ")
        val selectedTheme = AppCompatDelegate.getDefaultNightMode()
        val selectedThemeIndex = when (selectedTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            else -> 2 // Por defecto o sin especificar
        }

        val builder = MaterialAlertDialogBuilder(requireActivity())

        builder.setTitle("Selecciona una opción")
        builder.setSingleChoiceItems(options, selectedThemeIndex) { dialog, which ->
            when (which) {
                0 -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }

                1 -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }

                2 -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            // Finaliza la actividad actual para que los cambios de tema tengan efecto
            requireActivity().recreate()
            dialog.dismiss()
        }
        builder.show()
    }

    private fun saveImageUrlToFirebase(imageUri: Uri) {
        val usuarioActual = FirebaseUtils.getCurrentUserId()

        val imagesRef = storageRef.child("profile_user/$usuarioActual")

        // Subir el archivo de imagen al Firebase Storage
        imagesRef.putFile(imageUri).addOnSuccessListener { taskSnapshot ->
            // Obtener la URL de descarga de la imagen
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                loadImageFromUrl(downloadUrl, binding.imagenPerfil)

                // Actualizar la URL de la imagen en Firestore
                saveImageUrlToFirestore(downloadUrl)

                // Actualizar la URL de la imagen en la base de datos Room
                val usuarioActual = FirebaseAuth.getInstance().currentUser
                usuarioActual?.uid?.let { userId ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val userDao = AppDatabase.getDatabase(requireContext()).datosDao()
                        userDao.updateUserImageUrl(userId, downloadUrl)
                    }
                }

                Log.d("Foto", downloadUrl)
                Toast.makeText(requireContext(), "Imagen subida exitosamente", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al obtener la URL de descarga: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error al subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveImageUrlToFirestore(imageUrl: String) {
        val usuarioActual = FirebaseAuth.getInstance().currentUser
        usuarioActual?.uid?.let { userId ->
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("usuarios").document(userId)
            userRef.update("imageUrl", imageUrl).addOnSuccessListener {
                Log.d("Error foto", "URL de la imagen guardada en Firestore")
            }.addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error al guardar la URL de la imagen en Firestore", e)
            }
        }
    }


    private fun createImageUri(): Uri {
        // Obtenemos el directorio de archivos de la aplicación
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Creamos un archivo temporal en el directorio de imágenes
        val imageFile = File.createTempFile("temp_image", /* prefijo */
            ".png", /* sufijo */
            storageDir /* directorio */)

        // Devolvemos el URI del archivo temporal utilizando FileProvider
        return FileProvider.getUriForFile(requireContext(), "com.paquete.proyectoftg_appchat.fileprovider", imageFile)
    }


    private fun setNotificationsEnabled(enabled: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(PREFS_KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    private fun areNotificationsEnabled(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(PREFS_KEY_NOTIFICATIONS_ENABLED, true)
    }

    private fun setThemeMode(themeMode: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(PREFS_KEY_THEME, themeMode).apply()
    }

    private fun loadImageFromUrl(imageUrl: String, imageView: ImageView) {
        Glide.with(this).load(imageUrl).apply(RequestOptions.circleCropTransform()).into(imageView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    fun cerrarSesion() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("Cerrar sesión")
            setMessage("¿Estás seguro de que quieres cerrar sesión?")
            setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updateUserStatusOffline()
                            signOut()
                        } else {
                            Log.e(ContentValues.TAG, "Error al eliminar el token FCM", task.exception)
                        }
                    }

                    val intent = Intent(context, SplashActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val options = ActivityOptions.makeCustomAnimation(requireContext(), R.anim.slide_in_right, R.anim.slide_out_left)
                    startActivity(intent, options.toBundle())
                }
            }
            setNegativeButton("No") { _, _ -> }
        }.show()
    }

}



