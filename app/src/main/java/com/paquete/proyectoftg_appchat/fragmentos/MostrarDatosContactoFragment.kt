package com.paquete.proyectoftg_appchat.fragmentos

import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.databinding.FragmentDatosContactoBinding
import com.paquete.proyectoftg_appchat.firebaseutil.FirebaseFirestoreHelper
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.AddContactFragment
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.Utils

class MostrarDatosContactoFragment : Fragment() {

    private var _binding: FragmentDatosContactoBinding? = null
    private val binding get() = _binding!!
    private lateinit var elementosViewModel: ElementosViewModel
    private var firestoreHelper = FirebaseFirestoreHelper(FirebaseFirestore.getInstance())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDatosContactoBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        elementosViewModel = ViewModelProvider(requireActivity())[ElementosViewModel::class.java]


        binding.layoutSendMenssage.setOnClickListener {
            val telefono = binding.editTelefono.text.toString()
            FirebaseUtils.getFirestoreInstance().collection("usuarios").whereEqualTo("telefono", telefono).get()
                .addOnSuccessListener { nombreUsuarioSnapshot ->
                    if (!nombreUsuarioSnapshot.isEmpty) {
                        val contactos = elementosViewModel.contactoSelecionado().value
                        if (contactos != null) {
                            val uidUsuario = nombreUsuarioSnapshot.documents.first().id
                            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                            val channelId = currentUserUid?.let { generateChannelId(it, uidUsuario) }

                            val nombreRemitente = contactos.nombre.toString()
                            val messageFragment = MessageFragment.newInstance(channelId, uidUsuario, nombreRemitente)
                            Utils.navigateToFragment(requireActivity(), messageFragment)
                        }
                    } else {
                        Utils.showMessage(requireContext(),"Este contacto no está registrado en la aplicación")
                    }
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Error obteniendo datos: $exception")
                    Utils.showMessage(requireContext(),"Error obteniendo datos")
                }
        }



        binding.layoutDeleteContact.setOnClickListener {


            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("¿Deseas borrar el contacto?")
                setMessage("¿Estás seguro de que quieres eliminar contacto?")
                setPositiveButton("Borrar") { _, _ ->
                    deleteContact()
                }
                setNegativeButton("Cancelar") { _, _ -> }
            }.show()
        }


        binding.layoutEditContact.setOnClickListener {
            // Crear un nuevo fragmento de edición
            val editContactFragment = AddContactFragment()
            // Obtener el contacto que se va a editar
            val contactToEdit = elementosViewModel.contactoSeleccionado.value
            // Crear un Bundle para pasar la información del contacto al fragmento de edición
            val bundle = Bundle().apply {
                putParcelable("editedContact", contactToEdit)
            }
            // Establecer el Bundle como argumento del fragmento de edición
            editContactFragment.arguments = bundle
            // Navegar al fragmento de edición
            Utils.navigateToFragment(requireActivity(), editContactFragment)
        }


        // Observar los cambios en el ViewModel y actualizar la UI
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
                binding.textViewNombreCompleto.text = contactos.nombre
                binding.editTelefono.text = contactos.numero
                binding.textViewEmail.text = contactos.email

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

    private fun deleteContact() {
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
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_WRITE_CONTACTS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) { // El usuario concedió el permiso, procede con la eliminación del contacto
                    deleteContact()
                } else { // El usuario denegó el permiso, muestra un mensaje de error o toma alguna acción alternativa
                    Toast.makeText(context, "Permiso denegado para escribir en contactos", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_WRITE_CONTACTS = 1001
    }


    override fun onDestroyView() {
        super.onDestroyView() // Limpiar el binding al destruir la vista
        _binding = null
    }

}