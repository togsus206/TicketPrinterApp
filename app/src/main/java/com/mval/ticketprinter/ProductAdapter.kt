package com.mval.ticketprinter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private val productList: MutableList<Product>, // Cambiar a MutableList para permitir modificaciones
    private val listener: OnItemClickListener // Interfaz para manejar clics
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Interfaz para comunicar eventos de clic a la Activity
    interface OnItemClickListener {
        fun onEditClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewProductName)
        val textViewQuantity: TextView = itemView.findViewById(R.id.textViewQuantity)
        val textViewPrice: TextView = itemView.findViewById(R.id.textViewPrice)
        val buttonEdit: Button = itemView.findViewById(R.id.buttonEditProduct) // Nuevo botón
        val buttonDelete: Button = itemView.findViewById(R.id.buttonDeleteProduct) // Nuevo botón

        init {
            // Asignar listeners a los botones
            buttonEdit.setOnClickListener {
                // Asegurarse de que la posición sea válida
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(position)
                }
            }

            buttonDelete.setOnClickListener {
                // Asegurarse de que la posición sea válida
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = productList[position]
        holder.textViewName.text = currentItem.name
        holder.textViewQuantity.text = "x${currentItem.quantity}" // Formato mejor para cantidad
        holder.textViewPrice.text = String.format("$%.2f", currentItem.price)
    }

    override fun getItemCount() = productList.size
}
