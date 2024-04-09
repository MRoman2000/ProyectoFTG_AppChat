package com.paquete.proyectoftg_appchat.fragmentos

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.adapters.ChannelAdapter
import com.paquete.proyectoftg_appchat.data.AppDatabase
import com.paquete.proyectoftg_appchat.databinding.FragmentChatRoomBinding
import com.paquete.proyectoftg_appchat.model.ChatRoom
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChatRoomFragment : Fragment() {
    private lateinit var db: AppDatabase
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var chatRoomList: ArrayList<ChatRoom>

    private var _binding: FragmentChatRoomBinding? = null
    private lateinit var elementosViewModel: ElementosViewModel
    private val binding get() = _binding!!

    companion object {
        private const val REQUEST_CONTACT_PERMISSIONS = 100
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())
        requestContactPermissions()
        elementosViewModel = ViewModelProvider(requireActivity()).get(ElementosViewModel::class.java)
        chatRoomList = ArrayList()
        channelAdapter = ChannelAdapter(requireContext(), chatRoomList, elementosViewModel)

        fetchChatrooms()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CONTACT_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    obtenerContactos()
                } else {
                    // Permiso denegado, manejar caso en el que no se pueden obtener los contactos
                }
            }
        }
    }

    private fun requestContactPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request the permission
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CONTACT_PERMISSIONS)
        } else {
            // Permission is already granted, proceed with obtaining contacts
            obtenerContactos()
        }
    }

    private fun obtenerContactos() {
        // Aquí colocas la lógica para obtener los contactos
        CoroutineScope(Dispatchers.Main).launch {
            val contactos = withContext(Dispatchers.IO) { Contactos.obtenerContactos(requireContext()) }
        }
    }

    private fun fetchChatrooms() {
        if (!isAdded) {
            Log.e(TAG, "Fragment not attached to context")
            return
        }
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid


        val chatroomsCollectionRef = FirebaseFirestore.getInstance().collection("chats")
        chatroomsCollectionRef.whereArrayContains("userIds", currentUserUid!!).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e(TAG, "Error fetching chatrooms: $exception")
                return@addSnapshotListener
            }

            val chatrooms = snapshot?.documents?.mapNotNull { document ->
                document.toObject(ChatRoom::class.java)
            }

            // Verificar si el fragmento está adjunto antes de acceder al contexto
            if (!isAdded) {
                Log.e(TAG, "Fragment not attached to context")
                return@addSnapshotListener
            }

            // Actualizar la lista de canales y notificar al adaptador
            chatrooms?.let {
                chatRoomList.clear()
                chatRoomList.addAll(it)
                channelAdapter.notifyDataSetChanged()
            }

            // Configurar el RecyclerView
            val layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewChat.layoutManager = layoutManager
            binding.recyclerViewChat.adapter = channelAdapter
        }


    }


}