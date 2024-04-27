package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.actividades.SplashActivity
import com.paquete.proyectoftg_appchat.data.AppDatabase
import com.paquete.proyectoftg_appchat.databinding.FragmentSecurityBinding
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SecurityFragment : Fragment() {
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }
    private var _binding: FragmentSecurityBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Cuenta y Seguridad"
            setDisplayHomeAsUpEnabled(true)
        }

        binding.btnActualizarDatos.setOnClickListener {
            actualizarDatos()
        }

        elementosViewModel.obtenerDatosYElementoUsuarioActual(FirebaseUtils.getCurrentUserId()).observe(viewLifecycleOwner) { datauser ->
            datauser?.let {
                binding.editTextNombreUsuario.setText(datauser.nombreUsuario)
                binding.editTextNombreCompleto.setText(datauser.nombreCompleto)
                binding.editTextEmail.setText(datauser.email)

            }
        }


        binding.layoutDeleteAccount.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("Eliminar cuenta")
                setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Todos tus datos serán eliminados de forma permanente.")
                setPositiveButton("Sí") { _, _ ->

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            user?.delete()?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("EliminarCuenta", "Usuario eliminado exitosamente.")
                                    val db = FirebaseFirestore.getInstance()
                                    val userDocRef = db.collection("usuarios").document(user?.uid ?: "")
                                    userDocRef.delete()
                                    val intent = Intent(context, SplashActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    val options =
                                        ActivityOptions.makeCustomAnimation(requireContext(), R.anim.slide_in_right, R.anim.slide_out_left)
                                    startActivity(intent, options.toBundle())

                                } else {
                                    // Si falla, maneja el error.
                                    Log.w("EliminarCuenta", "Error al eliminar el usuario.", task.exception)
                                }
                            }

                        } catch (e: Exception) {
                            // Manejar la excepción de manera adecuada
                            withContext(Dispatchers.Main) {
                                // Imprimir el mensaje de error en la consola
                                Log.e("EliminarCuenta", "Error al eliminar cuenta", e)
                                // También puedes mostrar un mensaje de error al usuario si lo deseas
                                Toast.makeText(requireContext(), "Error al eliminar cuenta: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                setNegativeButton("No") { _, _ -> }
            }.show()
        }
    }

    fun actualizarDatos() {
        val nombreUsuario = binding.editTextNombreUsuario.text.toString()
        val nombreCompleto = binding.editTextNombreCompleto.text.toString()
        val email = binding.editTextEmail.text.toString()

        val usuarioActual = FirebaseUtils.getCurrentUserId().toString()
        val usuarioRef = FirebaseUtils.allusers()?.document(usuarioActual)

        usuarioRef?.get()?.addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot != null) {
                val updates = hashMapOf<String, Any>()
                if (nombreUsuario.isNotEmpty()) {
                    updates["nombreUsuario"] = nombreUsuario
                }
                if (nombreCompleto.isNotEmpty()) {
                    updates["nombreCompleto"] = nombreCompleto
                }
                if (email.isNotEmpty()) {
                    updates["email"] = email
                }

                usuarioRef.update(updates).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Datos actualizados con éxito", Toast.LENGTH_SHORT).show()
                    CoroutineScope(Dispatchers.Main).launch {
                        val userDao = AppDatabase.getDatabase(requireContext()).datosDao()
                        userDao.updateInfo(usuarioActual, nombreUsuario, nombreCompleto, email)
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al actualizar datos: $e", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
            }
        }?.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error al obtener usuario: $e", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView() // Limpiar el binding al destruir la vista
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}