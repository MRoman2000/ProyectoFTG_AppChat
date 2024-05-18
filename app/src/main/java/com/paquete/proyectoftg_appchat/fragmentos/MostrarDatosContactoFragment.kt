package com.paquete.proyectoftg_appchat.fragmentos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.data.ViewModel
import com.paquete.proyectoftg_appchat.databinding.FragmentDatosContactoBinding
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.Utils

class MostrarDatosContactoFragment : Fragment() {

    private var _binding: FragmentDatosContactoBinding? = null
    private val binding get() = _binding!!
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ViewModel::class.java]
    }
    private var userData: DataUser? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDatosContactoBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Informacion de Contacto"
            setDisplayHomeAsUpEnabled(true)
        }

        binding.layoutSendMenssage.setOnClickListener {

            val contactos = elementosViewModel.contactoSelecionado().value
            Log.d("Datos1", "$contactos")
            //  val telefono = binding.editTelefono.text.toString()
            FirebaseUtils.getFirestoreInstance().collection("usuarios").whereEqualTo("uid", contactos!!.uid).get()
                .addOnSuccessListener { nombreUsuarioSnapshot ->
                    if (!nombreUsuarioSnapshot.isEmpty) {
                        //       val contactos = elementosViewModel.contactoSelecionado().value
                        val uidUsuario = nombreUsuarioSnapshot.documents.first().id
                        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                        Log.d("Datos2", "$userData , $uidUsuario")
                        if (currentUserUid == uidUsuario) {
                            Utils.showMessage(requireContext(), "No puedes enviarte un mensaje a ti mismo")
                        } else {
                            val channelId = generateChannelId(currentUserUid!!, uidUsuario)
                            Log.d("Datos3", "$currentUserUid , $uidUsuario")

                            val otherUserModel = nombreUsuarioSnapshot.documents.first().toObject(DataUser::class.java)
                            if (otherUserModel != null) {
                                val elemento = DataUser(uid = otherUserModel.uid,
                                    email = otherUserModel.email,
                                    nombreCompleto = otherUserModel.nombreCompleto,
                                    nombreUsuario = otherUserModel.nombreUsuario,
                                    telefono = otherUserModel.telefono,
                                    imageUrl = otherUserModel.imageUrl)
                                //    elementosViewModel.insertar(elemento)

                                val profileFragment = MessageFragment()
                                val bundle = Bundle().apply {
                                    putParcelable("dataUser", elemento)
                                    putString("channelId", channelId)
                                    putString("recipientId", uidUsuario)

                                }
                                profileFragment.arguments = bundle
                                Utils.navigateToFragment(requireActivity(), profileFragment)
                            } else {
                                Log.e("Error", "No se pudo obtener los datos del usuario")
                                Utils.showMessage(requireContext(), "Error al obtener datos del usuario")
                            }
                        }
                    } else {
                        Utils.showMessage(requireContext(), "Este contacto no está registrado en la aplicación")
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Error", "Error obteniendo datos: $exception")
                    Utils.showMessage(requireContext(), "Error obteniendo datos")
                }
        }

        binding.layoutDeleteContact.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("¿Deseas borrar el contacto?")
                setMessage("¿Estás seguro de que quieres eliminar el contacto?")
                setPositiveButton("Borrar") { _, _ ->
                    // Obtener el contacto seleccionado
                    val contacto = elementosViewModel.contactoSeleccionado.value

                    // Verificar si el contacto seleccionado no es nulo
                    contacto?.let { contactToDelete ->
                        // Llamar al método para eliminar el contacto por su UID
                        elementosViewModel.eliminarContactoPorUID(contactToDelete.uid)
                        Utils.modificarContactoEnListaDeContactos(contactToDelete.uid, false)
                        // Mostrar un mensaje de éxito
                        Toast.makeText(requireContext(), "Contacto eliminado correctamente", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
                setNegativeButton("Cancelar") { _, _ -> }
            }.show()
        }

        binding.layoutAddContact.setOnClickListener {
            val contacto = elementosViewModel.contactoSeleccionado.value
            // Verificar si el contacto seleccionado no es nulo
            contacto?.let { contactToDelete ->
                // Llamar al método para eliminar el contacto por su UID
                elementosViewModel.insertarNuevoContacto(contactToDelete)
                Utils.modificarContactoEnListaDeContactos(contactToDelete.uid, true)
                // Mostrar un mensaje de éxito
                Toast.makeText(requireContext(), "Contacto añadido correctamente", Toast.LENGTH_SHORT).show()
            }
        }


        // Observar los cambios en el ViewModel.kt y actualizar la UI
        /*    elementosViewModel.seleccionado().observe(viewLifecycleOwner) { serie -> // Verificar si la serie no es nula
                serie?.let { // Cargar la imagen utilizando Glide y aplicar redondeo a las esquinas
                    Glide.with(requireContext()).load(serie.url_image_perfile)
                        .into(binding.imagenPerfil) // Mostrar otros datos en los TextViews
                    binding.textViewNombreCompleto.text = serie.nombreCompleto ?: ""
                    binding.editTelefono.text = serie.telefono ?: ""

                }
            } */

        elementosViewModel.contactoSelecionado().observe(viewLifecycleOwner) { contactos ->
            // Verificar si el contacto no es nulo
            contactos?.let {
                // Actualizar los componentes de la interfaz de usuario con los nuevos datos del contacto
                binding.textViewNombreCompleto.text = contactos.nombreCompleto
                binding.textViewNombreUsuario.text = "@" + contactos.nombreUsuario
                binding.textViewEmail.text = contactos.email
                Glide.with(binding.root.context).load(contactos.imageUrl).apply(RequestOptions.circleCropTransform())
                    .into(binding.imagenPerfil)

                if (contactos.email.isNullOrEmpty()) {
                    binding.layoutSendEmail.visibility = View.GONE
                } else {
                    binding.layoutSendEmail.visibility = View.VISIBLE
                    binding.layoutSendEmail.setOnClickListener {
                        // Obtener el correo electrónico del contacto seleccionado
                        val email = contactos.email ?: return@setOnClickListener

                        // Crear un Intent para abrir la aplicación de Gmail
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$email") // Especificar el esquema mailto: con el correo electrónico del destinatario
                            setPackage("com.google.android.gm") // Establecer el paquete de Gmail
                        }

                        // Verificar si hay una aplicación de Gmail disponible para manejar el intent
                        if (intent.resolveActivity(requireActivity().packageManager) != null) {
                            startActivity(intent) // Abrir la aplicación de Gmail
                        } else {
                            // Manejar el caso en que no haya una aplicación de Gmail disponible
                            Toast.makeText(requireContext(), "No se encontró la aplicación de Gmail", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


    private fun generateChannelId(sender: String, receiver: String): String {
        val participants = listOf(sender, receiver).sorted()
        return participants.joinToString("")
    }

    /*    private fun deleteContact() {
            val contact = elementosViewModel.contactoSelecionado().value
            contact?.let {
                val contactId = it.id

                val contentResolver: ContentResolver =
                    requireContext().contentResolver // Define la cláusula WHERE para seleccionar todos los "encabezados de contacto" con el ID específico
                val rawContactSelection = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
                val rawContactSelectionArgs = arrayOf(contactId)

                // Define la URI para eliminar todos los "encabezados de contacto" asociados a ese contacto
                val rawContactUri = ContactsContract.RawContacts.CONTENT_URI
                contentResolver.delete(rawContactUri, rawContactSelection, rawContactSelectionArgs)

                Toast.makeText(context, "Contacto eliminado correctamente", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        } */


    override fun onDestroyView() {
        super.onDestroyView() // Limpiar el binding al destruir la vista
        _binding = null
    }
}