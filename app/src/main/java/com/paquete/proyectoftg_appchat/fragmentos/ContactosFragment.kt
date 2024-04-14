package com.paquete.proyectoftg_appchat.fragmentos

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.adapters.ContactAdapter
import com.paquete.proyectoftg_appchat.databinding.FragmentContactosBinding
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.AddContactFragment
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.Utils


class ContactosFragment : Fragment() {
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }

    private var _binding: FragmentContactosBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentContactosBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Contactos"

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)

        binding.searchView.getEditText().onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) { // Ocultar el bottomNavigationView cuando la barra de búsqueda obtiene el foco
                bottomNavigationView.visibility = View.GONE
                (requireActivity() as AppCompatActivity).supportActionBar?.hide()
            } else { // Mostrar el bottomNavigationView cuando la barra de búsqueda pierde el foco
                bottomNavigationView.visibility = View.VISIBLE
                binding.searchView.text.clear()
                (requireActivity() as AppCompatActivity).supportActionBar?.show()
            }
        }




        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val posicion = viewHolder.adapterPosition
                // val dataUser: Contactos = elementosAdapter.obtenerElemento(posicion)
                // elementosViewModel.eliminar(dataUser)
            }
        }).attachToRecyclerView(binding.recyclerView)

        // Aquí movemos la llamada a requestContactPermissions() después de la inicialización de requestPermissionLauncher
        requestContactPermissions()
    }


    private fun loadContactos() {
        val elementosAdapter = ContactAdapter(requireContext(), elementosViewModel)
        val contactosDispositivo = obtenerContactosDispositivo()

        elementosAdapter.establecerListaContactos(contactosDispositivo)
        Log.d("Contactos", "Cantidad de contactos: ${contactosDispositivo.size}")

        val adapterBuscar = ContactAdapter(requireContext(), elementosViewModel)
        binding.recyclerViewBuscar.adapter = adapterBuscar
        adapterBuscar.establecerListaContactos(emptyList())

        binding.searchView.getEditText().addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim() // Eliminar espacios en blanco al inicio y al final
                binding.searchBar.setText(searchText) // Inicializar una lista vacía de contactos filtrados
                val contactosFiltrados = mutableListOf<Contactos>()

                // Solo buscar si hay texto en el campo de búsqueda
                if (searchText.isNotEmpty()) { // Filtrar los contactos que coincidan parcialmente con el texto de búsqueda
                    val contactosCoincidentes = contactosDispositivo.filter {
                        it.nombre!!.contains(searchText, ignoreCase = true)
                    } // Agregar los contactos coincidentes a la lista de contactos filtrados
                    contactosFiltrados.addAll(contactosCoincidentes)
                }

                // Actualizar el adaptador del RecyclerView de búsqueda con los contactos filtrados
                adapterBuscar.establecerListaContactos(contactosFiltrados)
            }
        })
        binding.recyclerView.adapter = elementosAdapter
        binding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
    }


    private fun requestContactPermissions() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) -> {
                // Permission has already been granted
                Log.d(TAG, "Contact permission is already granted")
                // Load the contacts data
                loadContactos()

                binding.addContact.setOnClickListener {
                    val agregarContacto = AddContactFragment()
                    Utils.navigateToFragment(requireActivity(), agregarContacto)
                }
            }

            else -> {
                Utils.showMessage(requireContext(), "No hay permisos")
            }
        }
    }

    private fun obtenerContactosDispositivo(): MutableList<Contactos> {
        val contactos = mutableListOf<Contactos>()
        val contentResolver = requireActivity().contentResolver

        // Cursor para recuperar los números de teléfono
        val phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)

        phoneCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneNumberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex)
                val phoneNumber = cursor.getString(phoneNumberIndex)
                val contact = Contactos(id, name, phoneNumber, "")
                contactos.add(contact)
            }
        }

        // Cursor para recuperar las direcciones de correo electrónico
        val emailCursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null)
        emailCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val email = cursor.getString(emailIndex)
                // Buscar el contacto correspondiente en la lista y agregar el correo electrónico
                val contacto = contactos.find { it.id == id }
                contacto?.let {
                    it.email = email
                }
            }
        }
        return contactos
    }

    override fun onResume() {
        super.onResume()
        // Ocultar la barra de navegación al volver a este fragmento
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

