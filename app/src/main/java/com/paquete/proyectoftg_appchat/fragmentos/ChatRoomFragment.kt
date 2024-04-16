package com.paquete.proyectoftg_appchat.fragmentos

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.adapters.ChannelAdapter
import com.paquete.proyectoftg_appchat.databinding.FragmentChatRoomBinding
import com.paquete.proyectoftg_appchat.model.ChatRoom
import com.paquete.proyectoftg_appchat.room.ElementosViewModel


class ChatRoomFragment : Fragment() {
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var chatRoomList: ArrayList<ChatRoom>
    private var _binding: FragmentChatRoomBinding? = null
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Chats"
        chatRoomList = ArrayList()

        // Observa el LiveData de usuarios en el ViewModel
        elementosViewModel.usuarios.observe(viewLifecycleOwner) { usuarios ->
            // Actualiza la lista de usuarios en el adaptador de canales
            channelAdapter.actualizarUsuarios(usuarios)
        }

        // Llama al método para cargar la lista de usuarios
        elementosViewModel.cargarUsuarios()

        // Configura el adaptador de canales
        channelAdapter = ChannelAdapter(chatRoomList, elementosViewModel)
        binding.recyclerViewChat.adapter = channelAdapter
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(requireContext())

        // Fetch chatrooms y realiza otras operaciones necesarias
        fetchChatrooms()
    }
    private fun fetchChatrooms() {
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

            chatrooms?.let {
                if (it != chatRoomList) {
                    chatRoomList.clear()
                    chatRoomList.addAll(it)
                    channelAdapter.notifyDataSetChanged()
                }
            }
        }
    }

}