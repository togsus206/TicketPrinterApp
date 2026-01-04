package com.mval.ticketprinter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val items: List<TicketHistoryItem>) : 
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDate: TextView = view.findViewById(R.id.textDate)
        val textCount: TextView = view.findViewById(R.id.textCount)
        val textTotal: TextView = view.findViewById(R.id.textTotal)
        val textDescription: TextView = view.findViewById(R.id.textDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_ticket, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        
        // Formato de fecha
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.textDate.text = sdf.format(Date(item.date))
        
        holder.textTotal.text = String.format("$%.2f", item.total)
        holder.textCount.text = "${item.productCount} prod."
        
        if (item.description.isNullOrEmpty()) {
        	holder.textDescription.text = "Sin detalle de productos."
    	} else {
        	holder.textDescription.text = item.description
    	}

        // Lógica de expandir/contraer
        holder.textDescription.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = items.size
}
