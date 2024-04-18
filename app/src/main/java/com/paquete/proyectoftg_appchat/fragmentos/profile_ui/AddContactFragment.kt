package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.RawContacts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.paquete.proyectoftg_appchat.databinding.FragmentAddContactBinding
import com.paquete.proyectoftg_appchat.model.Contactos
import java.util.UUID


class AddContactFragment : Fragment() {
    private var _binding: FragmentAddContactBinding? = null
    private val binding get() = _binding!!
    private var phoneNumber: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Agregar Contacto"
            setDisplayHomeAsUpEnabled(true)
        }
        arguments?.getParcelable<Contactos>("editedContact")?.let { editedContact ->
            // Cargar la información del contacto en los campos correspondientes
            binding.textFieldTelefono.setText(editedContact.numero)
            binding.textFieldNombreCompleto.setText(editedContact.nombre)
            binding.textFieldEmail.setText(editedContact.email)
        }

        arguments?.let {
            phoneNumber = it.getString("numero")
        }


        phoneNumber?.takeIf { it.isNotEmpty() }?.let { phoneNumber ->
            binding.textFieldTelefono.setText(phoneNumber)
        }

        binding.btnSaveContact.setOnClickListener {
            val numero = binding.textFieldTelefono.text.toString()
            val nombre = binding.textFieldNombreCompleto.text.toString()
            val email = binding.textFieldEmail.text.toString()

            if (numero.isNotEmpty()) {

                // Obtener el contacto editado de los argumentos del fragmento
                val editedContact = arguments?.getParcelable<Contactos>("editedContact")
                addOrUpdateContact(numero, nombre, email, editedContact)
                binding.textFieldTelefono.text = null
                binding.textFieldNombreCompleto.text = null
                binding.textFieldEmail.text = null
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                if (numero.isEmpty()) {
                    binding.textFieldTelefono.error = "Este campo es obligatorio"
                }
            }
        }
    }


    // Función para aplicar el formato deseado al número de teléfono
    private fun formatPhoneNumber(numero: String): String {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()

        // Obtener el código de país del dispositivo móvil
        //   val countryCode = phoneNumberUtil.getCountryCodeForRegion(Locale.getDefault().country)
        val countryCode = 34
        // Intentar parsear el número de teléfono
        try {
            val phoneNumber = phoneNumberUtil.parse(numero, null)

            // Si el número ya está en formato internacional, retornarlo tal cual
            if (phoneNumber.countryCode == countryCode) {
                return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            } else {
                // Si el número no está en formato internacional, agregar el prefijo del país del dispositivo
                return "+$countryCode ${phoneNumber.nationalNumber}"
            }
        } catch (e: Exception) {
            // Si ocurre un error al parsear el número, retornar el número original
            return numero
        }
    }

    private fun addOrUpdateContact(numero: String, nombre: String, email: String, editedContact: Contactos?) {
        val numeroFormateado = formatPhoneNumber(numero)
        val operations = ArrayList<ContentProviderOperation>()
        editedContact?.id?.let { contactId ->
            // Si editedContact no es nulo, entonces estamos editando un contacto existente

            // Crear un ContentValues para actualizar los datos del contacto
            val values = ContentValues().apply {
                put(StructuredName.DISPLAY_NAME, nombre)
            }

            // Actualizar el nombre del contacto
            val nameUpdateCount = requireContext().contentResolver.update(ContactsContract.Data.CONTENT_URI,
                values,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId, StructuredName.CONTENT_ITEM_TYPE))

            // Crear un ContentValues para actualizar el número de teléfono del contacto
            val phoneValues = ContentValues().apply {
                put(Phone.NUMBER, numeroFormateado)
            }

            // Actualizar el número de teléfono del contacto
            val phoneUpdateCount = requireContext().contentResolver.update(ContactsContract.Data.CONTENT_URI,
                phoneValues,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId, Phone.CONTENT_ITEM_TYPE))

            // Crear un ContentValues para actualizar el correo electrónico del contacto
            val emailValues = ContentValues().apply {
                put(Email.ADDRESS, email)
            }

            // Actualizar el correo electrónico del contacto
            val emailUpdateCount = requireContext().contentResolver.update(ContactsContract.Data.CONTENT_URI,
                emailValues,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId, Email.CONTENT_ITEM_TYPE))

            // Comprobar si al menos una de las actualizaciones se realizó con éxito
            if (nameUpdateCount > 0 || phoneUpdateCount > 0 || emailUpdateCount > 0) {
                // Al menos una actualización se realizó con éxito
                // Realizar cualquier acción adicional si es necesario
            } else {
                // Ninguna actualización se realizó
                // Manejar la situación según sea necesario
            }
        } ?: run {
            // Generar un identificador único para el contacto
            val contactId = UUID.randomUUID().toString()
            // Ejecutar el código para agregar el contacto aquí
            val p = ContentValues().apply {
                put(RawContacts.CONTACT_ID, contactId)
            }

            val rowContactUri: Uri? = requireContext().contentResolver.insert(RawContacts.CONTENT_URI, p)
            rowContactUri?.let { rowContact ->
                val rawContactId = ContentUris.parseId(rowContact)
                val value = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    put(StructuredName.DISPLAY_NAME, nombre)
                }
                requireContext().contentResolver.insert(ContactsContract.Data.CONTENT_URI, value)

                val ppv = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    put(Phone.NUMBER, numeroFormateado)
                    put(Phone.TYPE, Phone.TYPE_MOBILE)
                }
                requireContext().contentResolver.insert(ContactsContract.Data.CONTENT_URI, ppv)

                val emailValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    put(Email.ADDRESS, email)
                    put(Email.TYPE, Email.TYPE_HOME)
                }
                requireContext().contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
