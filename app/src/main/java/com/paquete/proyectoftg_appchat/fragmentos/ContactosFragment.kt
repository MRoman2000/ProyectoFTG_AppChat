package com.paquete.proyectoftg_appchat.fragmentos

import android.content.ContentValues.TAG
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.adapters.ContactAdapter
import com.paquete.proyectoftg_appchat.data.ViewModel
import com.paquete.proyectoftg_appchat.databinding.FragmentContactosBinding
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils


class ContactosFragment : Fragment() {
    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[ViewModel::class.java]
    }
    private lateinit var adapter: ContactAdapter
    private lateinit var adapterBuscar: ContactAdapter
    private var _binding: FragmentContactosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentContactosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            hide()
        }

        setupUI()
        observeUsuarios()
        loadContactos()
        setupFirestoreListener()
    }

    private fun setupUI() {
        adapterBuscar = ContactAdapter(requireContext(), viewModel)
        adapter = ContactAdapter(requireContext(), viewModel)
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        binding.searchView.getEditText().onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            bottomNavigationView.visibility = if (hasFocus) View.GONE else View.VISIBLE
            if (!hasFocus) binding.searchView.text.clear()
        }
    }

    private fun observeUsuarios() {
        adapter = ContactAdapter(requireContext(), viewModel)
        binding.recyclerView.adapter = adapter
        viewModel.contacto.observe(viewLifecycleOwner) { usuarios ->
            adapter.establecerListaContactos(usuarios)
        }
    }

    private fun loadContactos() {
        viewModel.cargarContactos()
        viewModel.contacto.observe(viewLifecycleOwner) { contactosDispositivo ->
            if (contactosDispositivo.isNotEmpty()) {
                adapter.establecerListaContactos(contactosDispositivo)
      //          adapterBuscar.establecerListaContactos(contactosDispositivo)
            } else {
                obtenerContactosFirebase { contactosObtenidos ->
                    adapter.establecerListaContactos(contactosObtenidos)
                    adapterBuscar.establecerListaContactos(contactosDispositivo)
                }
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.recyclerViewBuscar.adapter = adapterBuscar
        binding.recyclerViewBuscar.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.searchView.getEditText().addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().trim()
                if (searchText.isEmpty()) {
                    adapterBuscar.establecerListaContactos(emptyList())
                    return
                }
                FirebaseUtils.getFirestoreInstance().collection("usuarios").whereEqualTo("nombreUsuario", searchText).get()
                    .addOnSuccessListener { result ->
                        val contactosFiltrados = mutableListOf<Contactos>()
                        for (document in result) {
                            val usuario = document.toObject(Contactos::class.java)
                            contactosFiltrados.add(usuario)
                        }
                        adapterBuscar.establecerListaContactos(contactosFiltrados)
                    }.addOnFailureListener { exception ->
                        Log.e("erroUser", "Error al buscar usuario por nombre de usuario: ", exception)
                    }
            }
        })
    }

    private fun obtenerContactosDispositivo(onSuccess: (List<Contactos>) -> Unit) {
        val contactos = mutableListOf<Contactos>()
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            FirebaseUtils.getFirestoreInstance().collection("usuarios").document(currentUser.uid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val listaContactos = documentSnapshot.get("contactos") as? List<*>

                        listaContactos?.let { contactosUIDs ->
                            contactosUIDs.forEach { contactoUID ->
                                val contactoRef =
                                    FirebaseUtils.getFirestoreInstance().collection("usuarios").document(contactoUID.toString())

                                contactoRef.get().addOnSuccessListener { contactoSnapshot ->
                                    if (contactoSnapshot.exists()) {
                                        val contacto = contactoSnapshot.toObject(Contactos::class.java)
                                        contacto?.let {
                                            contactos.add(it)
                                            viewModel.insertarNuevoContacto(contacto)
                                            adapter.establecerListaContactos(contactos)
                                        }
                                    }
                                }
                            }/* onSuccess(contactos)
                            contactos.forEach { contacto ->
                                viewModel.insertarNuevoContacto(contacto)
                            } */
                        }
                    }
                }
        }
    }


    private fun setupFirestoreListener() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        currentUserUid?.let { uid ->
            val contactosRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(uid)
            contactosRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val contacto = snapshot.toObject(Contactos::class.java)
                    contacto?.let {
                        // Aquí actualizas los datos del contacto en la base de datos local
                        viewModel.actualizarContacto(contacto)
                    }
                } else {
                    Log.d(TAG, "El documento no existe")
                }
            }
        }
    }

    fun obtenerContactosFirebase(onSuccess: (List<Contactos>) -> Unit) {
        val contactosRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(FirebaseUtils.getCurrentUserId().toString())

        contactosRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val listaContactos = documentSnapshot.get("contactos") as? List<String>

                listaContactos?.forEach { contactoId ->
                    val contactoRef = FirebaseUtils.getFirestoreInstance().collection("usuarios").document(contactoId)

                    contactoRef.get().addOnSuccessListener { contactoSnapshot ->
                        if (contactoSnapshot.exists()) {
                            val contacto = contactoSnapshot.toObject(Contactos::class.java)
                            contacto?.let {
                                Log.d("contactos", contacto.toString())
                                // Insertar el contacto en la base de datos local
                                viewModel.insertarNuevoContacto(contacto)
                            }
                        }
                    }
                }
            }
        }
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
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
    }

}