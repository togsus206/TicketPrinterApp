package com.mval.ticketprinter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var editTextProductName: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var buttonAddProduct: Button
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var textViewTotal: TextView
    private lateinit var buttonPrint: Button
    private lateinit var buttonSettings: Button // Declaración del nuevo botón

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
        buttonSettings = findViewById(R.id.buttonSettings) // Obtener referencia al nuevo botón

        // Configurar el RecyclerView
        recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(productList)
        recyclerViewItems.adapter = adapter

        // Configurar el listener del botón "Agregar Producto"
        buttonAddProduct.setOnClickListener {
            val name = editTextProductName.text.toString().trim()
            val quantityStr = editTextQuantity.text.toString().trim()
            val priceStr = editTextPrice.text.toString().trim()

            if (name.isNotEmpty() && quantityStr.isNotEmpty() && priceStr.isNotEmpty()) {
                val quantity = quantityStr.toInt()
                val price = priceStr.toDouble()
                val product = Product(name, quantity, price)
                productList.add(product)
                adapter.notifyItemInserted(productList.size - 1)
                updateTotal()
                editTextProductName.text.clear()
                editTextQuantity.text.clear()
                editTextPrice.text.clear()
            } else {
                // Puedes mostrar un mensaje de error si algún campo está vacío
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
}
