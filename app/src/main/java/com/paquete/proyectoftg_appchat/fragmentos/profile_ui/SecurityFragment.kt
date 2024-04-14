package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.actividades.SplashActivity
import com.paquete.proyectoftg_appchat.databinding.FragmentSecurityBinding


class SecurityFragment : Fragment() {

    private var _binding: FragmentSecurityBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

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
        binding.layoutDeleteAccount.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser

            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("Eliminar cuenta")
                setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Todos tus datos serán eliminados de forma permanente.")
                setPositiveButton("Sí") { _, _ ->
                    // Eliminar datos de Firebase Firestore
                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("usuarios").document(user?.uid ?: "")
                    userDocRef.delete().addOnSuccessListener {

                        user?.delete()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Autenticación eliminada correctamente
                                val intent = Intent(context, SplashActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                val options =
                                    ActivityOptions.makeCustomAnimation(requireContext(), R.anim.slide_in_right, R.anim.slide_out_left)
                                startActivity(intent, options.toBundle())
                            } else {
                                // Error al eliminar la autenticación
                            }
                        }
                    }.addOnFailureListener { e ->
                        // Error al eliminar datos de Firestore
                    }
                }
                setNegativeButton("No") { _, _ -> }
            }.show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView() // Limpiar el binding al destruir la vista
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}