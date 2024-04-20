package com.paquete.proyectoftg_appchat.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.actividades.FullScreenImageActivity
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(val context: Context, val messageList: ArrayList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var userList: List<DataUser> = emptyList()
    val ITEM_RECEIVE = 1
    val ITEM_SENT = 2
    val ITEM_IMAGE_RECEIVE = 3
    val ITEM_IMAGE_SENT = 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_SENT -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.message_item_right, parent, false)
                SentViewHolder(view)
            }
            ITEM_RECEIVE -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.message_item_left, parent, false)
                ReceiveViewHolder(view)
            }
            ITEM_IMAGE_SENT -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.message_item_image_right, parent, false)
                ImageSentViewHolder(view)
            }
            ITEM_IMAGE_RECEIVE -> {
                val view: View = LayoutInflater.from(context).inflate(R.layout.message_item_image_left, parent, false)
                ImageReceiveViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentMessage = messageList[position]

        when (holder) {
            is SentViewHolder -> {
                holder.sentMessage.text = currentMessage.message
                holder.time.text = sdf.format(currentMessage.timestamp!!.toDate())
            }
            is ReceiveViewHolder -> {
                holder.receiveMessage.text = currentMessage.message
                holder.time.text = sdf.format(currentMessage.timestamp!!.toDate())
            }
            is ImageSentViewHolder -> {
                Glide.with(context).load(currentMessage.imageUrl).into(holder.sentImage)
                holder.sentImage.setOnClickListener {
                    // Cuando se hace clic en la imagen, abre una actividad o diálogo para mostrar la imagen en pantalla completa
                    val intent = Intent(context, FullScreenImageActivity::class.java)
                    intent.putExtra("imageUrl", currentMessage.imageUrl)
                    context.startActivity(intent)
                }
                holder.time.text = sdf.format(currentMessage.timestamp!!.toDate())
            }
            is ImageReceiveViewHolder -> {
                Glide.with(context).load(currentMessage.imageUrl).into(holder.receiveImage)
                holder.receiveImage.setOnClickListener {
                    // Cuando se hace clic en la imagen, abre una actividad o diálogo para mostrar la imagen en pantalla completa
                    val intent = Intent(context, FullScreenImageActivity::class.java)
                    intent.putExtra("imageUrl", currentMessage.imageUrl)
                    context.startActivity(intent)
                }
                holder.time.text = sdf.format(currentMessage.timestamp!!.toDate())
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        return when {
            currentMessage.imageUrl != null && FirebaseAuth.getInstance().currentUser?.uid == currentMessage.senderId -> ITEM_IMAGE_SENT
            currentMessage.imageUrl != null -> ITEM_IMAGE_RECEIVE
            FirebaseAuth.getInstance().currentUser?.uid == currentMessage.senderId -> ITEM_SENT
            else -> ITEM_RECEIVE
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.chat_right)
        val time = itemView.findViewById<TextView>(R.id.text_view_time)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage = itemView.findViewById<TextView>(R.id.chat_left)
        val time = itemView.findViewById<TextView>(R.id.text_view_time)
    }

    class ImageSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentImage = itemView.findViewById<ImageView>(R.id.image_right)
        val time = itemView.findViewById<TextView>(R.id.text_view_time)
    }

    class ImageReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveImage = itemView.findViewById<ImageView>(R.id.image_left)
        val time = itemView.findViewById<TextView>(R.id.text_view_time)
    }
}
