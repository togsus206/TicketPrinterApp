package com.mval.ticketprinter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater // ¡AÑADIR ESTA IMPORTACIÓN!
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.ArrayList

class MainActivity : AppCompatActivity(), ProductAdapter.OnItemClickListener {

    private lateinit var editTextProductName: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var buttonAddProduct: Button
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var textViewTotal: TextView
    private lateinit var buttonPrint: Button
    private lateinit var buttonSettings: Button

    private val productList = ArrayList<Product>()
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtener referencias a los elementos de la UI
        editTextProductName = findViewById(R.id.editTextProductName)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        editTextPrice = findViewById(R.id.editTextPrice)
        buttonAddProduct = findViewById(R.id.buttonAddProduct)
        recyclerViewItems = findViewById(R.id.recyclerViewItems)
        textViewTotal = findViewById(R.id.textViewTotal)
        buttonPrint = findViewById(R.id.buttonPrint)
        buttonSettings = findViewById(R.id.buttonSettings)

        // Configurar el RecyclerView
        recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(productList, this)
        recyclerViewItems.adapter = adapter

        // Configurar el listener del botón "Agregar Producto"
        buttonAddProduct.setOnClickListener {
            val name = editTextProductName.text.toString().trim()
            val quantityStr = editTextQuantity.text.toString().trim()
            val priceStr = editTextPrice.text.toString().trim()

            if (name.isNotEmpty() && quantityStr.isNotEmpty() && priceStr.isNotEmpty()) {
                try {
                    val quantity = quantityStr.toInt()
                    val price = priceStr.toDouble()
                    val product = Product(name, quantity, price)
                    productList.add(product)
                    adapter.notifyItemInserted(productList.size - 1)
                    updateTotal()
                    editTextProductName.text.clear()
                    editTextQuantity.text.clear()
                    editTextPrice.text.clear()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Cantidad o Precio deben ser números válidos.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos del producto.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener para el botón "Ajustes de Impresión"
        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Por ahora, podemos dejar el botón de imprimir sin funcionalidad
        buttonPrint.setOnClickListener {
            // Implementaremos la lógica de impresión más adelante
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateTotal() {
        var total = 0.0
        for (product in productList) {
            total += product.quantity * product.price
        }
        textViewTotal.text = String.format("$%.2f", total)
    }

    // Métodos de la interfaz ProductAdapter.OnItemClickListener
    override fun onEditClick(position: Int) {
        val productToEdit = productList[position]

        // Crear un layout para el diálogo de edición
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_product, null)
        val editName = dialogView.findViewById<EditText>(R.id.editProductName)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editProductQuantity)
        val editPrice = dialogView.findViewById<EditText>(R.id.editProductPrice)

        // Rellenar con los datos actuales del producto
        editName.setText(productToEdit.name)
        editQuantity.setText(productToEdit.quantity.toString())
        editPrice.setText(productToEdit.price.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Producto")
            .setView(dialogView)
            // Especificar explícitamente los tipos para el DialogInterface.OnClickListener
            .setPositiveButton("Guardar") { dialog: android.content.DialogInterface, _: Int -> // Corregido: tipos explícitos
                val newName = editName.text.toString().trim()
                val newQuantityStr = editQuantity.text.toString().trim()
                val newPriceStr = editPrice.text.toString().trim()

                if (newName.isNotEmpty() && newQuantityStr.isNotEmpty() && newPriceStr.isNotEmpty()) {
                    try {
                        val newQuantity = newQuantityStr.toInt()
                        val newPrice = newPriceStr.toDouble()

                        // Actualizar el producto en la lista
                        productToEdit.name = newName // Ahora se puede reasignar porque es 'var' en Product.kt
                        productToEdit.quantity = newQuantity
                        productToEdit.price = newPrice

                        adapter.notifyItemChanged(position)
                        updateTotal()
                        Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Cantidad o Precio inválidos", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            // Especificar explícitamente los tipos para el DialogInterface.OnClickListener
            .setNegativeButton("Cancelar") { dialog: android.content.DialogInterface, _: Int -> // Corregido: tipos explícitos
                dialog.cancel()
            }
            .show()
    }

    override fun onDeleteClick(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que quieres eliminar este producto?")
            .setPositiveButton("Eliminar") { dialog: android.content.DialogInterface, _: Int -> // Corregido: tipos explícitos
                productList.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateTotal()
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog: android.content.DialogInterface, _: Int -> // Corregido: tipos explícitos
                dialog.cancel()
            }
            .show()
    }
}
