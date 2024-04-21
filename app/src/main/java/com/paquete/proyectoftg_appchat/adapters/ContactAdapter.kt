package com.paquete.proyectoftg_appchat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.databinding.ViewholderContactosBinding
import com.paquete.proyectoftg_appchat.fragmentos.MostrarDatosContactoFragment

import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.room.ElementosViewModel

class ContactAdapter(private val context: Context,
    private val elementosViewModel: ElementosViewModel) : RecyclerView.Adapter<ContactAdapter.ElementoViewHolder>() {
    private var contactos: List<Contactos> = emptyList()

    fun establecerListaContactos(contactos: List<Contactos>) {
        this.contactos = contactos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementoViewHolder {
        val binding = ViewholderContactosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ElementoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ElementoViewHolder, position: Int) {
        val contacto = contactos[position]
        holder.bind(contacto)

    }

    override fun getItemCount(): Int {
        return contactos.size ?: 0
    }


    inner class ElementoViewHolder(private val binding: ViewholderContactosBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contacto: Contactos) {
            binding.nombre.text = contacto.nombre ?: "Sin nombre"
            binding.textViewNumero.text = contacto.numero ?: "Sin número"

            // Mostrar icono si el contacto está registrado
            if (contacto.registradoEnFirestore) {
                binding.userRegistrado.visibility = View.VISIBLE
            } else {
                binding.userRegistrado.visibility = View.GONE
            }

            itemView.setOnClickListener {
                elementosViewModel.seleccionarContacto(contacto)
                val mostrar = MostrarDatosContactoFragment()
                val transaction = (context as FragmentActivity).supportFragmentManager.beginTransaction()
                transaction.replace(R.id.main_frame_layout, mostrar)
                transaction.addToBackStack(null)
                transaction.commit()
            }
        }
    }
}

