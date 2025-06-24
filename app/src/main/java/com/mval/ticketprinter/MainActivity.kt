package com.mval.ticketprinter

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

import android.bluetooth.BluetoothSocket // For BluetoothSocket
import java.io.IOException // For IOException
import java.io.OutputStream // For OutputStream

class MainActivity : AppCompatActivity(), ProductAdapter.OnItemClickListener {

    private lateinit var editTextProductName: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var buttonAddProduct: Button
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var textViewTotal: TextView
    private lateinit var buttonPrint: Button
    private lateinit var buttonSettings: Button
    private lateinit var buttonConexionBt: Button
    
    private lateinit var buttonShareCard: Button

    private val productList = ArrayList<Product>()
    private lateinit var adapter: ProductAdapter

    // Variables para la configuración del ticket (estas se cargarán desde SharedPreferences)
    private var logoBitmap: Bitmap? = null
    private var headerText: String = ""
    private var footerText: String = ""
    private var printDateTime: Boolean = false
    private var enableQrCode: Boolean = true
    private var qrCodeText: String = "equicontrol.dev.ar"
    private var qrImageBitmap: Bitmap? = null
    private var ticketPaperWidth: Int = SettingsActivity.DEFAULT_PAPER_WIDTH
    
    
    // Nuevo segmento para la impresion BT
    // --- Variables para Bluetooth Printing ---
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothDeviceAddress: String? = null

    // Constante para el UUID SPP (Serial Port Profile) genérico para impresoras Bluetooth
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Request code para iniciar la actividad BtConnect
    private val REQUEST_SELECT_DEVICE = 104
    
    // Fin segmento implementacion BT

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
        buttonConexionBt = findViewById(R.id.buttonConexionBt)
        buttonShareCard = findViewById(R.id.buttonShareCard)

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
        
        // Listener para el botón "Conexion BT"
        buttonConexionBt.setOnClickListener {
            val intent = Intent(this, BtConnect::class.java)
            // Usa startActivityForResult para obtener la dirección del dispositivo seleccionado
            startActivityForResult(intent, REQUEST_SELECT_DEVICE) 
        }

        // Cambiar el OnClickListener del botón Print para mostrar el menú
        buttonPrint.setOnClickListener {
            if (productList.isEmpty()) {
                Toast.makeText(this, "Agrega productos para generar el ticket.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showPrintShareMenu()
        }
        


        //Boton para compartir tarjeta
        buttonShareCard.setOnClickListener {
             generateCard()
        }

    }

    override fun onResume() {
        super.onResume()
        loadTicketSettings()
        // Cargar la última dirección Bluetooth guardada, si existe
        val sharedPreferences = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        bluetoothDeviceAddress = sharedPreferences.getString("last_bluetooth_device_address", null)
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
        qrCodeText = "Total: ${textViewTotal.text}"
    }

    // Métodos de la interfaz ProductAdapter.OnItemClickListener
    override fun onEditClick(position: Int) {
        val productToEdit = productList[position]

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_product, null)
        val editName = dialogView.findViewById<EditText>(R.id.editProductName)
        val editQuantity = dialogView.findViewById<EditText>(R.id.editProductQuantity)
        val editPrice = dialogView.findViewById<EditText>(R.id.editProductPrice)

        editName.setText(productToEdit.name)
        editQuantity.setText(productToEdit.quantity.toString())
        editPrice.setText(productToEdit.price.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Producto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog: android.content.DialogInterface, _: Int ->
                val newName = editName.text.toString().trim()
                val newQuantityStr = editQuantity.text.toString().trim()
                val newPriceStr = editPrice.text.toString().trim()

                if (newName.isNotEmpty() && newQuantityStr.isNotEmpty() && newPriceStr.isNotEmpty()) {
                    try {
                        val newQuantity = newQuantityStr.toInt()
                        val newPrice = newPriceStr.toDouble()

                        productToEdit.name = newName
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
            .setNegativeButton("Cancelar") { dialog: android.content.DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    override fun onDeleteClick(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que quieres eliminar este producto?")
            .setPositiveButton("Eliminar") { dialog: android.content.DialogInterface, _: Int ->
                productList.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateTotal()
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog: android.content.DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    private fun showPrintShareMenu() {
        val popupMenu = PopupMenu(this, buttonPrint)
        popupMenu.menuInflater.inflate(R.menu.print_share_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share_ticket -> {
                    generateAndShareTicket()
                    true
                }
                R.id.action_print_ticket -> {
                    //Toast.makeText(this, "Funcionalidad de impresión (próximamente)", Toast.LENGTH_SHORT).show()
                    printTicket()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun generateAndShareTicket() {
        updateTotal()
        val ticketView = generateTicketContent()
        val ticketBitmap = createBitmapFromView(ticketView)
        shareTicket(ticketBitmap)
    }

    private fun loadTicketSettings() {
        val sharedPreferences = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        val logoPath = sharedPreferences.getString(SettingsActivity.KEY_LOGO_PATH, null)
        logoBitmap = if (logoPath != null) {
            BitmapFactory.decodeFile(logoPath)
        } else {
            null
        }
        
        val QRPath = sharedPreferences.getString(SettingsActivity.KEY_QR_PATH, null)
        qrImageBitmap = if (QRPath != null) {
            BitmapFactory.decodeFile(QRPath)
        } else {
            null
        }
        
        if (QRPath != null) {
            enableQrCode = false
        } else {
            enableQrCode = true
        }

        headerText = sharedPreferences.getString(SettingsActivity.KEY_HEADER, "Encabezado por defecto") ?: "Encabezado por defecto"
        footerText = sharedPreferences.getString(SettingsActivity.KEY_FOOTER, "Pie de página por defecto") ?: "Pie de página por defecto"
        printDateTime = sharedPreferences.getBoolean(SettingsActivity.KEY_PRINT_DATE_TIME, false)
        //enableQrCode = true
        ticketPaperWidth = sharedPreferences.getInt(SettingsActivity.KEY_PAPER_WIDTH, SettingsActivity.DEFAULT_PAPER_WIDTH)
    }

    private fun generateTicketContent(): View {
        val inflater = LayoutInflater.from(this)
        val ticketView = inflater.inflate(R.layout.ticket_preview, null)

        val logoImageView = ticketView.findViewById<ImageView>(R.id.ticketLogo)
        val headerTextView = ticketView.findViewById<TextView>(R.id.ticketHeader)
        val dateTimeTextView = ticketView.findViewById<TextView>(R.id.ticketDateTime)
        val itemsContainer = ticketView.findViewById<LinearLayout>(R.id.ticketItemsContainer)
        val totalTextView = ticketView.findViewById<TextView>(R.id.ticketTotal)
        val footerTextView = ticketView.findViewById<TextView>(R.id.ticketFooter)
        val qrCodeImageView = ticketView.findViewById<ImageView>(R.id.ticketQrCode)
        val ticketRootLayout = ticketView.findViewById<LinearLayout>(R.id.ticketRootLayout)

        val displayMetrics = resources.displayMetrics
        val ticketWidthPx = (ticketPaperWidth * displayMetrics.densityDpi / 25.4f).toInt()

        val layoutParams = LinearLayout.LayoutParams(ticketWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT)
        ticketRootLayout.layoutParams = layoutParams

        if (logoBitmap != null) {
            logoImageView.setImageBitmap(logoBitmap)
            logoImageView.visibility = View.VISIBLE
        } else {
            logoImageView.visibility = View.GONE
        }

        headerTextView.text = headerText
        if (printDateTime) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            dateTimeTextView.text = "Fecha: ${sdf.format(Date())}"
            dateTimeTextView.visibility = View.VISIBLE
        } else {
            dateTimeTextView.visibility = View.GONE
        }

        itemsContainer.removeAllViews()
        for (product in productList) {
            val itemLayout = LayoutInflater.from(this).inflate(R.layout.ticket_item_layout, itemsContainer, false)
            val nameTextView = itemLayout.findViewById<TextView>(R.id.itemName)
            val quantityTextView = itemLayout.findViewById<TextView>(R.id.itemQuantity)
            val priceTextView = itemLayout.findViewById<TextView>(R.id.itemPrice)

            nameTextView.text = product.name
            quantityTextView.text = "x${product.quantity}"
            priceTextView.text = String.format("$%.2f", product.price * product.quantity)

            itemsContainer.addView(itemLayout)
        }

        totalTextView.text = textViewTotal.text
        footerTextView.text = footerText

        if (enableQrCode) {
            val qrBitmap = generateQrCode(qrCodeText)
            if (qrBitmap != null) {
                qrCodeImageView.setImageBitmap(qrBitmap)
                qrCodeImageView.visibility = View.VISIBLE
            } else {
                qrCodeImageView.visibility = View.GONE
            }
        } else {
            if (qrImageBitmap != null) {
                qrCodeImageView.setImageBitmap(qrImageBitmap)
                qrCodeImageView.visibility = View.VISIBLE
            } else {
                qrCodeImageView.visibility = View.GONE
            }
        }

        return ticketView
    }

    private fun createBitmapFromView(view: View): Bitmap {
        // Obtenemos las métricas de la pantalla para calcular un ancho adecuado
        val displayMetrics = resources.displayMetrics
        val ticketWidthPx = (ticketPaperWidth * displayMetrics.densityDpi / 25.4f).toInt() // Ancho en mm a píxeles

        // Establecer el ancho del layout root del ticket
        val layoutParams = view.layoutParams
        if (layoutParams != null) {
            layoutParams.width = ticketWidthPx
            view.layoutParams = layoutParams
        } else {
            // Si layoutParams es nulo, creamos unos nuevos. Esto no debería pasar si ticket_preview.xml
            // ya tiene layout_width y layout_height definidos en su root.
            val newLayoutParams = LinearLayout.LayoutParams(ticketWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT)
            view.layoutParams = newLayoutParams
        }


        // Medir la vista para determinar su altura necesaria.
        // Usamos MeasureSpec.UNSPECIFIED para la altura para que la vista calcule su altura completa.
        view.measure(
            View.MeasureSpec.makeMeasureSpec(ticketWidthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED) // Aquí el cambio importante
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Crear el Bitmap con las dimensiones medidas
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // **Asegurarnos de que el fondo del Canvas no sea transparente si la vista no lo dibuja**
        // Si el ticket_preview.xml no tiene un background, el bitmap por defecto es transparente (negro en algunas representaciones)
       	// dibujar un fondo blanco en el canvas aquí:
        canvas.drawColor(android.graphics.Color.WHITE) // Dibuja un fondo blanco

        view.draw(canvas) // Dibuja la vista en el canvas
        return bitmap
    }

    private fun shareTicket(bitmap: Bitmap) {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Ticket_${System.currentTimeMillis()}", null)
        val imageUri = Uri.parse(path)

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/png"
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(shareIntent, "Compartir Ticket"))
    }

    private fun generateQrCode(text: String): Bitmap? {
        val width = 400
        val height = 400
        val hints = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L

        try {
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val offset = y * width + x
                    pixels[offset] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar código QR: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }
    
    
    // Crear tarjeta de presentacion
    private fun generatePresentation(): View {
        val inflater = LayoutInflater.from(this)
        val ticketView = inflater.inflate(R.layout.ticket_preview, null)

        val logoImageView = ticketView.findViewById<ImageView>(R.id.ticketLogo)
        val headerTextView = ticketView.findViewById<TextView>(R.id.ticketHeader)
        val dateTimeTextView = ticketView.findViewById<TextView>(R.id.ticketDateTime)
        val itemsContainer = ticketView.findViewById<LinearLayout>(R.id.ticketItemsContainer)
        val totalTextView = ticketView.findViewById<TextView>(R.id.ticketTotal)
        val footerTextView = ticketView.findViewById<TextView>(R.id.ticketFooter)
        val qrCodeImageView = ticketView.findViewById<ImageView>(R.id.ticketQrCode)

        // Get reference to the "TOTAL" label TextView
        val totalLabelTextView = ticketView.findViewById<TextView>(R.id.ticketTotalLabel) // Correct ID found in ticket_preview.xml

        val ticketRootLayout = ticketView.findViewById<LinearLayout>(R.id.ticketRootLayout)

        val displayMetrics = resources.displayMetrics
        val ticketWidthPx = (ticketPaperWidth * displayMetrics.densityDpi / 25.4f).toInt()

        val layoutParams = LinearLayout.LayoutParams(ticketWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT)
        ticketRootLayout.layoutParams = layoutParams

        if (logoBitmap != null) {
            logoImageView.setImageBitmap(logoBitmap)
            logoImageView.visibility = View.VISIBLE
        } else {
            logoImageView.visibility = View.GONE
        }

        headerTextView.text = headerText
        headerTextView.visibility = View.VISIBLE // Ensure header is visible

        // --- Start of modifications to hide unnecessary elements ---

        // Hide date/time
        dateTimeTextView.visibility = View.GONE

        // Hide items container
        itemsContainer.visibility = View.GONE

        // Hide total value
        totalTextView.visibility = View.GONE

        // Hide the "TOTAL" label
        totalLabelTextView.visibility = View.GONE 

        // Hide footer
        footerTextView.visibility = View.GONE

        // Hide QR code
        qrCodeImageView.visibility = View.GONE

        // --- End of modifications ---

        return ticketView
    }
    
    //Generar la tarjeta de presentacion
    private fun generateCard() {
        val ticketView = generatePresentation()
        val ticketBitmap = createBitmapFromView(ticketView)
        shareTicket(ticketBitmap)
    }
    
    
    // --- Nuevas funciones para impresión Bluetooth ---

    // Manejar el resultado de la actividad BtConnect
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_DEVICE && resultCode == RESULT_OK) {
            val address = data?.getStringExtra("device_address")
            if (address != null) {
                bluetoothDeviceAddress = address
                // Guardar la dirección para la próxima vez
                getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString("last_bluetooth_device_address", address)
                    .apply()
                Toast.makeText(this, "Dispositivo guardado: $address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun printTicket() {
        if (bluetoothDeviceAddress == null) {
            Toast.makeText(this, "Primero conecta una impresora Bluetooth en 'BT'", Toast.LENGTH_LONG).show()
            return
        }

        // Verificar permisos de Bluetooth antes de intentar conectar
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso BLUETOOTH_CONNECT necesario para imprimir", Toast.LENGTH_SHORT).show()
            return
        }

        // Generar el ticket como bitmap
        val ticketView = generateTicketContent()
        val ticketBitmap = createBitmapFromView(ticketView)

        Thread {
            try {
                if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                    val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(bluetoothDeviceAddress)
                    if (device == null) {
                        runOnUiThread { Toast.makeText(this, "Dispositivo Bluetooth no encontrado.", Toast.LENGTH_LONG).show() }
                        return@Thread
                    }
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    bluetoothSocket?.connect()
                    runOnUiThread { Toast.makeText(this, "Conectado a la impresora.", Toast.LENGTH_SHORT).show() }
                }

                val outputStream: OutputStream? = bluetoothSocket?.outputStream
                if (outputStream != null) {
                    // Opcional: Enviar comando de centrado antes de la imagen
                    outputStream.write(centerAlignCommand()) // Corrección: Asegurarse de que 'outputStream' es no nulo

                    // Convertir el Bitmap a datos ESC/POS
                    val escPosImageBytes = convertBitmapToEscPos(ticketBitmap)
                    outputStream.write(escPosImageBytes) // Corrección: Asegurarse de que 'outputStream' es no nulo

                    // Opcional: Volver a alinear a la izquierda (si hay más texto después)
                    outputStream.write(leftAlignCommand()) // Corrección: Asegurarse de que 'outputStream' es no nulo

                    // Alimentar un poco de papel al final
                    outputStream.write("\n\n\n".toByteArray()) // Corrección: Asegurarse de que 'outputStream' es no nulo
                    outputStream.flush()
                    runOnUiThread { Toast.makeText(this, "Ticket enviado a la impresora.", Toast.LENGTH_SHORT).show() }
                } else {
                    runOnUiThread { Toast.makeText(this, "Error al obtener OutputStream de la impresora.", Toast.LENGTH_LONG).show() }
                }
            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this, "Error de impresión: ${e.message}", Toast.LENGTH_LONG).show() }
                try {
                    bluetoothSocket?.close()
                    bluetoothSocket = null
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
                e.printStackTrace()
            } catch (e: SecurityException) {
                runOnUiThread { Toast.makeText(this, "Permiso de Bluetooth denegado: ${e.message}", Toast.LENGTH_LONG).show() }
                e.printStackTrace()
            }
        }.start()
    }

    // Función para convertir un Bitmap a datos de imagen ESC/POS
    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val printerWidth = 384 // Ancho común para impresoras de 58mm. Ajusta si tu impresora es diferente (e.g., 576 para 80mm)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, printerWidth, (bitmap.height * printerWidth / bitmap.width), false)

        val pixels = IntArray(scaledBitmap.width * scaledBitmap.height)
        scaledBitmap.getPixels(pixels, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)

        val imageBytes = ByteArrayOutputStream()

        val widthBytes = scaledBitmap.width / 8
        val heightPixels = scaledBitmap.height

        // Comandos ESC/POS para imprimir una imagen de trama (raster bit image)
        // GS v 0 m xL xH yL yH d1...dk
        imageBytes.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00)) // GS v 0 m=0 (mode 0)
        imageBytes.write(widthBytes.toByte().toInt()) // xL // Corrección: Convertir a Int antes de escribir
        imageBytes.write((widthBytes shr 8).toByte().toInt()) // xH // Corrección: Convertir a Int antes de escribir
        imageBytes.write(heightPixels.toByte().toInt()) // yL // Corrección: Convertir a Int antes de escribir
        imageBytes.write((heightPixels shr 8).toByte().toInt()) // yH // Corrección: Convertir a Int antes de escribir

        for (y in 0 until heightPixels) {
            for (xByte in 0 until widthBytes) {
                var byteValue = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < scaledBitmap.width) {
                        val pixel = pixels[y * scaledBitmap.width + x]
                        val gray = (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel)) / 3
                        if (gray < 128) {
                            byteValue = byteValue or (0x80 shr bit)
                        }
                    }
                }
                imageBytes.write(byteValue.toByte().toInt()) // Corrección: Convertir a Byte y luego a Int para el método write(Int)
            }
        }
        return imageBytes.toByteArray()
    }

    // Opcional: Una función para centrar la imagen (usando comandos ESC/POS)
    private fun centerAlignCommand(): ByteArray {
        return byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1 (center alignment)
    }

    // Opcional: Una función para alinear a la izquierda (después de centrar la imagen)
    private fun leftAlignCommand(): ByteArray {
        return byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0 (left alignment)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}



