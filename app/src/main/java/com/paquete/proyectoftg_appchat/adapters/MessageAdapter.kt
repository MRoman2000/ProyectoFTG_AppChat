package com.paquete.proyectoftg_appchat.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(val context: Context, val messageList: ArrayList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val ITEM_RECEIVE = 1
    val ITEM_SENT = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        if (viewType == 1) { // inflate receive
            val view: View = LayoutInflater.from(context).inflate(R.layout.message_item_left, parent, false)
            return ReceiveViewHolder(view)
        } else { // inflate sent
            val view: View = LayoutInflater.from(context).inflate(R.layout.message_item_right, parent, false)
            return SentViewHolder(view)
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentMessage = messageList[position]

        if (holder is SentViewHolder) {
            holder.sentMessage.text = currentMessage.message
            holder.time.text = sdf.format(currentMessage.timestamp!!.toDate())

        } else if (holder is ReceiveViewHolder) {
            holder.receiveMessage.text = currentMessage.message
            holder.time.text = sdf.format(currentMessage.timestamp!!.toDate())
        }
    }


    override fun getItemViewType(position: Int): Int {

        val currentMessage = messageList[position]

        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)) {
            return ITEM_SENT
        } else {
            return ITEM_RECEIVE
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.chat_right)
        val time = itemView.findViewById<TextView>(R.id.text_view_time)

        init {
            Log.d("MessageAdapter", "SentViewHolder: sentMessage = $sentMessage")
        }
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage = itemView.findViewById<TextView>(R.id.chat_left)
        val time = itemView.findViewById<TextView>(R.id.text_view_time)

        init {
            Log.d("MessageAdapter", "ReceiveViewHolder: receiveMessage = $receiveMessage")
        }
    }

}