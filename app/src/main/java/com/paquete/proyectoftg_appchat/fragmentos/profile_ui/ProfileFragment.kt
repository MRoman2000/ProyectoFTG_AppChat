package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.paquete.proyectoftg_appchat.data.ViewModel
import com.paquete.proyectoftg_appchat.databinding.FragmentProfileBinding
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.Utils


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null

    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[ViewModel::class.java]
    }
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Informacion de Perfil"
            setDisplayHomeAsUpEnabled(true)
        }
        val userData = arguments?.getParcelable<DataUser>("dataUser")


        loadImageFromUrl(userData!!.imageUrl.toString(), binding.imagenPerfil)
        binding.editTextNombreCompleto.text = userData.nombreCompleto ?: ""
        binding.editTextNombreUsuario.text = "@${userData.nombreUsuario}"
        binding.editTextTelefono.text = userData.telefono ?: ""
        binding.textViewEmail.text = userData.email ?: ""

        addContacto(userData)
    }

    private fun addContacto(userData: DataUser?) {
        viewModel.dataUser.observe(viewLifecycleOwner) { datauser ->
            val usuarioActual = FirebaseUtils.getCurrentUserId()
            val uidContacto = userData?.uid

            if (uidContacto == usuarioActual) {
                // Si el contacto es el usuario actual, ocultar el botón de agregar
                binding.buttonAddContact.visibility = View.GONE
            } else {
                // Verificar si el contacto ya existe
                viewModel.cargarContactos()
                // Observar la lista de contactos y verificar si el contacto ya existe
                viewModel.contacto.observe(viewLifecycleOwner) { contactos ->
                    val uidContacto = userData?.uid
                    val contactoExistente = contactos.any { it.uid == uidContacto }
                    if (contactoExistente) {
                        // Si el contacto ya está en la lista de contactos del usuario actual, ocultar el botón de agregar
                        binding.buttonAddContact.visibility = View.GONE
                    } else {
                        // Si el contacto no existe en la lista de contactos, mostrar el botón de agregar
                        binding.buttonAddContact.visibility = View.VISIBLE

                        // Configurar el clic del botón de agregar
                        binding.buttonAddContact.setOnClickListener {
                            // Crear el objeto Contactos
                            val nuevoContacto = Contactos(userData!!.uid,userData.email,userData.nombreCompleto,userData.nombreUsuario,userData.imageUrl)
                            // Insertar el nuevo contacto
                            viewModel.insertarNuevoContacto(nuevoContacto)
                            // Ocultar el botón después de agregar el contacto
                            binding.buttonAddContact.visibility = View.GONE
                            Utils.modificarContactoEnListaDeContactos(userData.uid, true)
                            // Mostrar un mensaje de éxito
                            Toast.makeText(requireContext(), "Contacto añadido correctamente", Toast.LENGTH_SHORT).show()
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
        _binding = null
    }

}