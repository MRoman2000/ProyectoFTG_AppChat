package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.paquete.proyectoftg_appchat.databinding.FragmentProfileBinding
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }


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


        val userData = arguments?.getParcelable<DataUser>("userData")
        userData?.let {
            loadImageFromUrl(it.imageUrl.toString(), binding.imagenPerfil)
            binding.editTextNombreCompleto.text = it.nombreCompleto ?: ""
            binding.editTextNombreUsuario.text = it.nombreUsuario ?: ""
            binding.editTextTelefono.text = it.telefono ?: ""
            binding.textViewEmail.text = it.email ?: ""
        }

        addContacto(userData)
    }


    private fun addContacto(userData: DataUser?) {
        elementosViewModel.obtenerDatosYElementoUsuarioActual(FirebaseUtils.getCurrentUserId()).observe(viewLifecycleOwner) { datauser ->
            datauser?.let {
                val numeroActual = datauser.telefono

                val numero = userData?.telefono
                if (numero == numeroActual) {
                    // Si el número coincide con el del usuario actual, ocultar el botón de agregar
                    binding.buttonAddContact.visibility = View.GONE
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        val contactos = withContext(Dispatchers.IO) {
                            Contactos.obtenerContactos(requireContext())
                        }
                        val usuario = contactos?.find { it.numero == numero }

                        // Si el número no está en la lista de contactos, mostrar el botón de agregar y configurar el listener
                        if (usuario == null) {
                            binding.buttonAddContact.visibility = View.VISIBLE
                            binding.buttonAddContact.setOnClickListener {
                                val addContactFragment = AddContactFragment()
                                val bundle = Bundle().apply {
                                    putString("numero", numero)
                                }
                                addContactFragment.arguments = bundle
                                Utils.navigateToFragment(requireActivity(), addContactFragment)
                            }
                        } else {
                            // Si el número está en la lista de contactos, ocultar el botón de agregar
                            binding.buttonAddContact.visibility = View.GONE
                        }
                    }
                }
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

}