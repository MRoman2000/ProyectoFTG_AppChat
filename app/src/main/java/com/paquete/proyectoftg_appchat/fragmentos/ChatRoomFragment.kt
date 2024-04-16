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
import com.paquete.proyectoftg_appchat.data.AppDatabase
import com.paquete.proyectoftg_appchat.databinding.FragmentChatRoomBinding
import com.paquete.proyectoftg_appchat.model.ChatRoom
import com.paquete.proyectoftg_appchat.room.ElementosViewModel


class ChatRoomFragment : Fragment() {
    private lateinit var db: AppDatabase
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

        db = AppDatabase.getDatabase(requireContext())
        chatRoomList = ArrayList()
        channelAdapter = ChannelAdapter(chatRoomList, elementosViewModel)
        fetchChatrooms()
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