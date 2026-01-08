package com.mval.ticketprinter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

// Notar que ahora recibimos List<Any> (puede ser String o TicketHistoryItem)
class HistoryAdapter(private val items: List<Any>) : 
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Constantes para identificar el tipo de vista
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // ViewHolder para el TICKET (Tu diseño original)
    class TicketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDate: TextView = view.findViewById(R.id.textDate)
        val textCount: TextView = view.findViewById(R.id.textCount)
        val textTotal: TextView = view.findViewById(R.id.textTotal)
        val textDescription: TextView = view.findViewById(R.id.textDescription)
    }

    // ViewHolder para el ENCABEZADO (La fecha sola)
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textHeaderDate: TextView = view.findViewById(R.id.textHeaderDate)
    }

    // Paso 1: Decidir qué diseño usar según el ítem
    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_ITEM
    }

    // Paso 2: Crear la vista correspondiente
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_ticket, parent, false)
            TicketViewHolder(view)
        }
    }

    // Paso 3: Rellenar los datos
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            // Lógica del ENCABEZADO
            val headerTitle = items[position] as String
            val headerHolder = holder as HeaderViewHolder
            headerHolder.textHeaderDate.text = headerTitle
        } else {
            // Lógica del TICKET (Tu lógica original)
            val item = items[position] as TicketHistoryItem
            val ticketHolder = holder as TicketViewHolder
            
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()) // Solo hora, la fecha ya está en el encabezado
            ticketHolder.textDate.text = sdf.format(Date(item.date))
            
            ticketHolder.textTotal.text = String.format("$%.2f", item.total)
            ticketHolder.textCount.text = "${item.productCount} prod."

            // Validar descripción
            if (item.description.isNullOrEmpty()) {
                ticketHolder.textDescription.text = "Sin detalle de productos."
            } else {
                ticketHolder.textDescription.text = item.description
            }

            ticketHolder.textDescription.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

            ticketHolder.itemView.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount() = items.size
}
