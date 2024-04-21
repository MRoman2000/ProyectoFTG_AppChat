package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.databinding.FragmentProfileBinding
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!





    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Informacion de Perfil"
            setDisplayHomeAsUpEnabled(true)
        }
        val usuarioActual = FirebaseAuth.getInstance().currentUser?.uid

        val userData = arguments?.getParcelable<DataUser>("userData")
        userData?.let {
            // Utiliza los datos del usuario aquí
            loadImageFromUrl(it.imageUrl.toString(), binding.imagenPerfil)
            binding.editTextNombreCompleto.setText(it.nombreCompleto ?: "")
            binding.editTextNombreUsuario.setText(it.nombreUsuario ?: "")
            binding.editTextTelefono.setText(it.telefono ?: "")
        }

        addContacto(userData)
    }


    private fun addContacto(userData: DataUser?) {
        val numero = userData?.telefono
        CoroutineScope(Dispatchers.Main).launch {
            val contactos = withContext(Dispatchers.IO) {
                Contactos.obtenerContactos(requireContext())
            }
            val usuario = contactos?.find { it.numero == numero }
            binding.buttonAddContact.visibility = View.VISIBLE
            if (usuario == null) {
                binding.buttonAddContact.setOnClickListener {
                    val addContactFragment = AddContactFragment()
                    val bundle = Bundle().apply {
                        putString("numero", numero)
                    }
                    addContactFragment.arguments = bundle
                    Utils.navigateToFragment(requireActivity(), addContactFragment)
                }
            } else {
                binding.buttonAddContact.visibility = View.GONE
            }
        }
    }



    private fun loadImageFromUrl(imageUrl: String, imageView: ImageView) {
        Glide.with(this).load(imageUrl).apply(RequestOptions.circleCropTransform()).into(imageView)
    }


    override fun onDestroyView() {
        super.onDestroyView() // Limpiar el binding al destruir la vista
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
    }
}