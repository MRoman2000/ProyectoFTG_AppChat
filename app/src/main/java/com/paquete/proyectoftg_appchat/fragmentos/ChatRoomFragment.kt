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
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        chatRoomList = ArrayList()
        setupRecyclerView()
        observeUsuarios()
        fetchChatrooms()
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(chatRoomList, elementosViewModel)
        binding.recyclerViewChat.apply {
            adapter = channelAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeUsuarios() {
        elementosViewModel.cargarUsuarios()
        elementosViewModel.usuarios.observe(viewLifecycleOwner) { usuarios ->
            channelAdapter.actualizarUsuarios(usuarios)
        }


    }

    private fun fetchChatrooms() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val chatroomsCollectionRef = FirebaseFirestore.getInstance().collection("chats")
        chatroomsCollectionRef.whereArrayContains("userIds", currentUserUid).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e(TAG, "Error fetching chatrooms: $exception")
                return@addSnapshotListener
            }

            val chatrooms = snapshot?.documents?.mapNotNull { document ->
                document.toObject(ChatRoom::class.java)
            }

            chatrooms?.let {
                chatRoomList.clear()
                chatRoomList.addAll(it)
                channelAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}