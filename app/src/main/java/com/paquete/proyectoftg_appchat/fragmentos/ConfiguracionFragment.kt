package com.paquete.proyectoftg_appchat.fragmentos

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.actividades.SplashActivity
import com.paquete.proyectoftg_appchat.databinding.FragmentConfiguracionBinding
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.ProfileFragment
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.SecurityFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.Utils
import com.paquete.proyectoftg_appchat.utils.Utils.Companion.updateUserStatusOffline
import kotlinx.coroutines.launch
import java.util.Locale


class ConfiguracionFragment : Fragment() {
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }
    private var userData: DataUser? = null
    private val PREFS_KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private val PREFS_KEY_THEME = "theme_preference"
    private var _binding: FragmentConfiguracionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Configuracion"
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        val usuarioActual = FirebaseUtils.getCurrentUserId()


        // Utilizar los datos del usuario actual si ya están disponibles
        userData?.let {
            loadImageFromUrl(it.imageUrl.toString(), binding.imagenPerfil)
            binding.textNombreCompleto.text = it.nombreCompleto ?: ""
            binding.textNombreUsuario.text = it.nombreUsuario ?: ""
        }

        elementosViewModel.obtenerDatosYElementoUsuarioActual(usuarioActual).observe(viewLifecycleOwner) { datauser ->
            datauser?.let {
                userData = it
                loadImageFromUrl(it.imageUrl.toString(), binding.imagenPerfil)
                binding.textNombreCompleto.text = it.nombreCompleto ?: ""
                binding.textNombreUsuario.text = it.nombreUsuario ?: ""
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

        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setNotificationsEnabled(true)
            } else {
                setNotificationsEnabled(false)
            }
        }

        binding.layoutAparencia.setOnClickListener {
            val options = arrayOf("Oscuro", "Claro", "Por Defecto")
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

        binding.layoutSecurity.setOnClickListener {

            val security = SecurityFragment()
            Utils.navigateToFragment(requireActivity(), security)
        }


        binding.layoutIdioma.setOnClickListener {
            val languages = arrayOf("English", "Spanish") // Lista de idiomas disponibles

            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle("Select Language")
            builder.setItems(languages) { dialog, which ->
                when (which) {
                    0 -> {
                        setLocale("en") // Idioma inglés
                        //    sharedPref.edit().putString("language", "en").apply()
                    }

                    1 -> {
                        setLocale("es") // Idioma español
                        //     sharedPref.edit().putString("language", "es").apply()
                    }
                }
            }
            builder.show()
        }




        binding.layoutLogout.setOnClickListener {
            cerrarSesion()
        }
    }

    // Función para cargar los datos del usuario en la interfaz de usuario
    fun cargarDatosUsuario(userData: DataUser?) {
        userData?.let { user ->
            loadImageFromUrl(user.imageUrl.toString(), binding.imagenPerfil)
            binding.textNombreCompleto.text = user.nombreCompleto ?: ""
            binding.textNombreUsuario.text = user.nombreUsuario ?: ""
        }
    }

// Llamamos a la función para cargar los datos del usuario cuando se tenga la información disponible

    fun mostrarDatos(userList: List<DataUser>) {
        val userId = FirebaseUtils.getCurrentUserId() // Obtener el ID del usuario actual
        val currentUser = userList.find { it.uid == userId } // Encontrar el usuario actual en la lista de usuarios

        currentUser?.let { user ->
            // Mostrar los datos del usuario en la interfaz de usuario
            loadImageFromUrl(user.imageUrl.toString(), binding.imagenPerfil)
            binding.textNombreCompleto.text = user.nombreCompleto ?: ""
            binding.textNombreUsuario.text = user.nombreUsuario ?: ""
        }
    }


    fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
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
                    updateUserStatusOffline()
                    FirebaseUtils.signOut()
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



