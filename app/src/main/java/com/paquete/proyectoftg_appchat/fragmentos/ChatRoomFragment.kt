package com.paquete.proyectoftg_appchat.fragmentos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.adapters.ChatRoomsAdapter
import com.paquete.proyectoftg_appchat.data.ViewModel
import com.paquete.proyectoftg_appchat.databinding.FragmentChatRoomBinding
import com.paquete.proyectoftg_appchat.model.ChatRoom


class ChatRoomFragment : Fragment() {

    private lateinit var chatRoomsAdapter: ChatRoomsAdapter
    private lateinit var chatRoomList: ArrayList<ChatRoom>
    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[ViewModel::class.java]
    }
    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = "Chat"
        }
        
        chatRoomList = ArrayList()
        observeUsuarios()
        cargarChatRoom()
        setupRecyclerView()

    }


    private fun setupRecyclerView() {
        chatRoomsAdapter = ChatRoomsAdapter(chatRoomList, viewModel)
        binding.recyclerViewChat.apply {
            adapter = chatRoomsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewChat.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            // Adjuntar ItemTouchHelper al RecyclerView
            chatRoomsAdapter.itemTouchHelper.attachToRecyclerView(this)
        }
    }
    private fun observeUsuarios() {
        viewModel.cargarUsuarios()
        viewModel.usuarios.observe(viewLifecycleOwner) { usuarios ->
            chatRoomsAdapter.actualizarUsuarios(usuarios)
        }

    }

    private fun cargarChatRoom() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatroomsCollectionRef = FirebaseFirestore.getInstance().collection("chats")
        chatroomsCollectionRef.whereArrayContains("userIds", currentUserUid).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("Error", "Error fetching chatrooms: $exception")
                return@addSnapshotListener
            }

            val chatrooms = snapshot?.documents?.mapNotNull { document ->
                document.toObject(ChatRoom::class.java)
            }

            chatrooms?.let {
                chatRoomList.clear()
                chatRoomList.addAll(it)
                chatRoomsAdapter.notifyDataSetChanged()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}