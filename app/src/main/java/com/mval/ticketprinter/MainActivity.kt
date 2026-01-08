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
import android.app.Activity 
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.bluetooth.BluetoothSocket 
import java.io.IOException 
import java.io.OutputStream 
//-------------
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
    private lateinit var buttonDeleteAll: Button

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
    private var saveTicket: Boolean = true
    
    
    // Nuevo segmento para la impresion BT
    // --- Variables para Bluetooth Printing ---
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothDeviceAddress: String? = null

    // Constante para el UUID SPP (Serial Port Profile) genérico para impresoras Bluetooth
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Request code para iniciar la actividad BtConnect
    private val REQUEST_SELECT_DEVICE = 104
    
    
    //Chequeo de BT encendido
    private lateinit var enableBtLauncher: ActivityResultLauncher<Intent> 
    private lateinit var bluetoothPermissionLauncher: ActivityResultLauncher<String>
    
    // Fin segmento implementacion BT
    
    // Variables para el historial
	private lateinit var tabTicket: TextView
	private lateinit var tabHistory: TextView
	private lateinit var layoutTicketCreation: LinearLayout
	private lateinit var layoutHistory: LinearLayout
	private lateinit var recyclerViewHistory: RecyclerView
	private lateinit var adapterHistory: HistoryAdapter
	// Gson para guardar/cargar objetos
	private val gson = com.google.gson.Gson()
	private lateinit var buttonClearHistory: Button
        
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() //
        setContentView(R.layout.activity_main)
        
		val mainView = findViewById<View>(R.id.main)
		
		ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
    		// Combinamos los insets de las barras del sistema y del teclado (IME)
    		val barsAndIme = insets.getInsets(
        		WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
    		)
		
    		// Aplicamos el padding. 
    		// Cuando sale el teclado, 'barsAndIme.bottom' aumentará automáticamente.
    		v.setPadding(barsAndIme.left, barsAndIme.top, barsAndIme.right, barsAndIme.bottom)
		
    		// Retornamos insets (esto permite que el sistema sepa que ya manejamos el espacio)
    		WindowInsetsCompat.CONSUMED
		}
		

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
        buttonDeleteAll = findViewById(R.id.buttonDeleteAll)

        // Configurar el RecyclerView
        recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(productList, this)
        recyclerViewItems.adapter = adapter
        
        // Inicializa el launcher para la solicitud de permiso de Bluetooth
        bluetoothPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, ahora puedes intentar habilitar Bluetooth
                continueBluetoothFlow(0)
            } else {
                // Permiso denegado, muestra un mensaje al usuario
                Toast.makeText(this, "Permiso de Bluetooth denegado. No se puede imprimir.", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Inicializa el launcher
        enableBtLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                printTicket(0)
            } else {
                Toast.makeText(this, "Bluetooth no activado.", Toast.LENGTH_SHORT).show()
            }
        }

        // listener del botón "Agregar Producto"
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

        // OnClickListener del botón Print para mostrar el menú
        buttonPrint.setOnClickListener {
            if (productList.isEmpty()) {
                Toast.makeText(this, "Agrega productos para generar el ticket.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showPrintShareMenu()
        }
        


        //Boton para compartir tarjeta
        buttonShareCard.setOnClickListener {
             showPrintShareCard()
        }
        
        //Boton para eliminar todos los productos de la lista
        buttonDeleteAll.setOnClickListener {
             onDeleteAll()
        }
        
        
        // Nuevos inicializadores para el historial ---
    	tabTicket = findViewById(R.id.tabTicket)
    	tabHistory = findViewById(R.id.tabHistory)
    	layoutTicketCreation = findViewById(R.id.layoutTicketCreation)
    	layoutHistory = findViewById(R.id.layoutHistory)
    	recyclerViewHistory = findViewById(R.id.recyclerViewHistory)
    	recyclerViewHistory.layoutManager = LinearLayoutManager(this)
	
    	// Listener para Tab Ticket
    	tabTicket.setOnClickListener {
        	showTab(true)
    	}
	
    	// Listener para Tab History
    	tabHistory.setOnClickListener {
        	showTab(false)
        	loadHistory() // Cargar lista al entrar
    	}
    	
    	
    	buttonClearHistory = findViewById(R.id.buttonClearHistory)

        buttonClearHistory.setOnClickListener {
            // Confirmamos antes de borrar
            MaterialAlertDialogBuilder(this)
                .setTitle("Borrar Historial")
                .setMessage("¿Estás seguro de que deseas eliminar TODOS los tickets guardados? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { dialog, _ ->
                    deleteHistory() // Llamamos a la función de borrado
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        
        setupSwipeToDelete()

    }

    override fun onResume() {
        super.onResume()
        loadTicketSettings()
        // Cargar la última dirección Bluetooth guardada, si existe
        val sharedPreferences = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        bluetoothDeviceAddress = sharedPreferences.getString("last_bluetooth_device_address", null)
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

	//Borra un el producto de la lista 
    override fun onDeleteClick(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que quieres eliminar este producto?")
            .setPositiveButton("Eliminar") { dialog: android.content.DialogInterface, _: Int ->
                productList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, productList.size)
                updateTotal()
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog: android.content.DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }
    
    
    //Funcion para eliminar todos los productos de la lista
    private fun onDeleteAll() {
    MaterialAlertDialogBuilder(this)
        .setTitle("Eliminar todos los Productos")
        .setMessage("¿Estás seguro de que quieres eliminar todos los productos?")
        .setPositiveButton("Eliminar") { dialog: android.content.DialogInterface, _: Int ->
            
            // Lógica corregida
            val size = productList.size
            productList.clear()
            adapter.notifyItemRangeRemoved(0, size) // Notifica el rango borrado
            
            updateTotal()
            Toast.makeText(this, "Productos eliminados", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        .setNegativeButton("Cancelar") { dialog: android.content.DialogInterface, _: Int ->
            dialog.cancel()
        }
        .show()
}

	//Funcion para compartir o imprimir el ticket
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
                	checkBluetoothAndPrint(1)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
    
    
    //Funcion para compartir o imprimir la Tarjeta de presentacion
    private fun showPrintShareCard() {
        val popupMenu = PopupMenu(this, buttonShareCard)
        popupMenu.menuInflater.inflate(R.menu.print_share_card, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share_card -> {
                    generateCard()
                    true
                }
                R.id.action_print_card -> {
                	checkBluetoothAndPrint(0)
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
        
        //Guarda el ticket impreso en el historial
        saveTicketToHistory()
        
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
        enableQrCode = sharedPreferences.getBoolean(SettingsActivity.KEY_PRINT_QR, false)
        ticketPaperWidth = sharedPreferences.getInt(SettingsActivity.KEY_PAPER_WIDTH, SettingsActivity.DEFAULT_PAPER_WIDTH)
        saveTicket = sharedPreferences.getBoolean(SettingsActivity.KEY_SAVE_TICKET, false)
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
            priceTextView.text = String.format("$%.2f", product.price)

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
	
	
	//FUNCION PRINCIPAL PARA IMPRIMIR EL TICKET (CONVIERTE AL FORMATO QUE ENTIENDE LA IMPRESORA)
    private fun printTicket(ticket: Int) {
    	
        if (bluetoothDeviceAddress == null) {
            Toast.makeText(this, "Primero conecta una impresora Bluetooth en 'BT'", Toast.LENGTH_LONG).show()
            return
        }

        // Solo verificar BLUETOOTH_CONNECT si estamos en Android 12 o superior
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        	if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            	Toast.makeText(this, "Permiso BLUETOOTH_CONNECT necesario para imprimir", Toast.LENGTH_SHORT).show()
            	return
        	}
    	}
    	
		
		val ticketBitmap: Bitmap?
		
		if (ticket == 1) {
            // Generar el ticket como bitmap
        	val ticketView = generateTicketContent()
        	ticketBitmap = createBitmapFromView(ticketView)
        }
        else{
        	// Generar el ticket como bitmap
        	val ticketView = generatePresentation()
        	ticketBitmap = createBitmapFromView(ticketView)
        }
        	
        	
        //------------------------------------------------
        if(saveTicket){
        	val bytes = ByteArrayOutputStream()
        	ticketBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        	val path = MediaStore.Images.Media.insertImage(contentResolver, ticketBitmap, "Ticket_${System.currentTimeMillis()}", null)
        	//val imageUri = Uri.parse(path)
		
        }
        //-----------------------

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
                    outputStream.write(centerAlignCommand()) 

                    // Convertir el Bitmap a datos ESC/POS
                    val escPosImageBytes = convertBitmapToEscPos(ticketBitmap)
                    outputStream.write(escPosImageBytes) 

                    // Opcional: Volver a alinear a la izquierda (si hay más texto después)
                    outputStream.write(leftAlignCommand()) //  Asegurarse de que 'outputStream' es no nulo

                    // Alimentar un poco de papel al final
                    outputStream.write("\n\n\n".toByteArray()) // Asegurarse de que 'outputStream' es no nulo
                    outputStream.flush()
                    runOnUiThread { Toast.makeText(this, "Ticket enviado a la impresora.", Toast.LENGTH_SHORT).show() }
                    //Guarda el ticket impreso en el historial
                    saveTicketToHistory()
                } else {
                    runOnUiThread { Toast.makeText(this, "Error al obtener OutputStream de la impresora.", Toast.LENGTH_LONG).show() }
                }
            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this, "Error de impresión: Verifica que la impresora este en rango de alcance. ${e.message}", Toast.LENGTH_LONG).show() }
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
    
   
	
	private fun checkBluetoothAndPrint(ticket: Int) {
    	// Si la versión de Android es 12 (API 31) o superior,
    	// necesitas el permiso BLUETOOTH_CONNECT
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        	if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            	// Permiso ya concedido, continuar con la verificación de Bluetooth
            	continueBluetoothFlow(ticket)
        	} else {
            	// Solicitar el permiso al usuario
            	bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        	}
    	} else {
        	// En versiones anteriores, no se requiere BLUETOOTH_CONNECT
        	continueBluetoothFlow(ticket)
    	}
	}
	
	// Nueva función para el flujo de Bluetooth
	private fun continueBluetoothFlow(ticket: Int) {
    	val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
	
    	if (bluetoothAdapter == null) {
        	Toast.makeText(this, "Este dispositivo no soporta Bluetooth.", Toast.LENGTH_SHORT).show()
        	return
    	}
	
    	if (!bluetoothAdapter.isEnabled) {
        	val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        	enableBtLauncher.launch(enableBtIntent)
    	} else {
        	printTicket(ticket)
    	}
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

    // función para alinear a la izquierda (después de centrar la imagen)
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
    
    // Función auxiliar para cambiar colores y visibilidad
    private fun showTab(isTicket: Boolean) {
        // Definimos el color azul manualmente para evitar errores de recursos
        val activeColor = android.graphics.Color.parseColor("#2196F3")
        val inactiveColor = android.graphics.Color.GRAY

        if (isTicket) {
            layoutTicketCreation.visibility = View.VISIBLE
            layoutHistory.visibility = View.GONE

            // Estilo visual "Activo" para Ticket
            tabTicket.setTextColor(activeColor) // <--- CAMBIO AQUÍ
            tabTicket.setTypeface(null, android.graphics.Typeface.BOLD)

            // Estilo visual "Inactivo" para History
            tabHistory.setTextColor(inactiveColor)
            tabHistory.setTypeface(null, android.graphics.Typeface.NORMAL)
            
            // Opcional: Cambiar el fondo del tab activo/inactivo si lo deseas
             tabTicket.setBackgroundResource(R.drawable.tab_indicator_active)
             tabHistory.background = null

        } else {
            layoutTicketCreation.visibility = View.GONE
            layoutHistory.visibility = View.VISIBLE

            // Estilo visual "Activo" para History
            tabHistory.setTextColor(activeColor) // <--- CAMBIO AQUÍ
            tabHistory.setTypeface(null, android.graphics.Typeface.BOLD)

            // Estilo visual "Inactivo" para Ticket
            tabTicket.setTextColor(inactiveColor)
            tabTicket.setTypeface(null, android.graphics.Typeface.NORMAL)
            
            // Opcional: Cambiar el fondo del tab activo/inactivo
            tabHistory.setBackgroundResource(R.drawable.tab_indicator_active)
            tabTicket.background = null
        }
    }
	
	//Funcion para guardar el ticket en el historial
	private fun saveTicketToHistory() {
    	if (productList.isEmpty()) return
	
    	// 1. Construir el detalle completo
    	val descriptionBuilder = StringBuilder()
    	
    	for (p in productList) {
        	// Calculamos el total de esa línea (Precio unitario * Cantidad)
        	val lineTotal = p.price * p.quantity
        	
        	descriptionBuilder.append("• ${p.name} x${p.quantity}: $${String.format("%.2f", lineTotal)}\n")
    	}
    	
    	// 2. Calcular total final del ticket
    	var total = 0.0
    	for (p in productList) { total += p.quantity * p.price }
	
    	// 3. Crear el objeto
    	val newItem = TicketHistoryItem(
        	id = System.currentTimeMillis(),
        	date = System.currentTimeMillis(),
        	total = total,
        	productCount = productList.size,
        	// .trim() elimina el último salto de línea sobrante
        	description = descriptionBuilder.toString().trim() 
    	)
	
    	// 4. Guardar en SharedPreferences (Igual que antes)
    	val prefs = getSharedPreferences("TicketAppHistory", Context.MODE_PRIVATE)
    	val jsonList = prefs.getString("history_list", "[]")
    	// Nota: Como ya arreglamos Proguard, esto funcionará perfecto
    	val type = object : com.google.gson.reflect.TypeToken<ArrayList<TicketHistoryItem>>() {}.type
    	val currentHistory: ArrayList<TicketHistoryItem> = gson.fromJson(jsonList, type) ?: ArrayList()
    	
    	currentHistory.add(0, newItem) 
    	
    	prefs.edit().putString("history_list", gson.toJson(currentHistory)).apply()
	}
	
	private fun loadHistory() {
        val prefs = getSharedPreferences("TicketAppHistory", Context.MODE_PRIVATE)
        val jsonList = prefs.getString("history_list", "[]")
        
        // 1. Obtener la lista cruda de tickets
        val type = object : com.google.gson.reflect.TypeToken<ArrayList<TicketHistoryItem>>() {}.type
        val rawList: ArrayList<TicketHistoryItem> = gson.fromJson(jsonList, type) ?: ArrayList()

        // 2. Crear la lista mixta (con encabezados)
        val displayList = ArrayList<Any>()
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")) // Ej: "Lunes, 12 de Enero"
        
        var lastHeaderDate = ""

        for (ticket in rawList) {
            val ticketDateString = dateFormat.format(Date(ticket.date)).replaceFirstChar { it.uppercase() }

            // Si la fecha de este ticket es diferente a la última que pusimos, agregamos un encabezado
            if (ticketDateString != lastHeaderDate) {
                displayList.add(ticketDateString) // Agregamos el String (Encabezado)
                lastHeaderDate = ticketDateString
            }
            
            displayList.add(ticket) // Agregamos el Ticket
        }

        // 3. Pasar la lista mixta al adaptador
        adapterHistory = HistoryAdapter(displayList)
        recyclerViewHistory.adapter = adapterHistory
    }
	
	
	//Funcion para eliminar el historial
	private fun deleteHistory() {
        // 1. Borrar de la memoria (SharedPreferences)
        val prefs = getSharedPreferences("TicketAppHistory", Context.MODE_PRIVATE)
        prefs.edit().remove("history_list").apply()

        // 2. Limpiar la lista visual actual recargándola (ahora estará vacía)
        loadHistory() 

        Toast.makeText(this, "Historial eliminado correctamente", Toast.LENGTH_SHORT).show()
    }
    
    // Función para configurar el deslizamiento (Swipe)
    private fun setupSwipeToDelete() {
        val swipeHandler = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            0, // No movemos arriba/abajo
            androidx.recyclerview.widget.ItemTouchHelper.LEFT // Solo deslizar a la IZQUIERDA
        ) {
            
            // Dibujar el fondo rojo al deslizar
            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                // Solo dibujar si es un Ticket (no en los encabezados)
                if (viewHolder is HistoryAdapter.TicketViewHolder) {
                    val itemView = viewHolder.itemView
                    val paint = android.graphics.Paint()
                    paint.color = android.graphics.Color.parseColor("#D32F2F") // Rojo

                    // Dibujar rectángulo rojo
                    if (dX < 0) { // Deslizando a la izquierda
                        c.drawRect(
                            itemView.right.toFloat() + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            // Bloquear el swipe si es un Encabezado
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder is HistoryAdapter.HeaderViewHolder) return 0 // No permitir deslizar fecha
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            // Qué pasa cuando se completa el deslizamiento
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                // Obtenemos el ítem que se quiere borrar
                val itemToDelete = adapterHistory.items[position]

                if (itemToDelete is TicketHistoryItem) {
                    deleteSingleItem(itemToDelete)
                }
            }
        }

        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerViewHistory)
    }

    // Lógica para borrar un solo ticket de la memoria y recargar
    private fun deleteSingleItem(itemToDelete: TicketHistoryItem) {
        // 1. Cargar lista actual de memoria
        val prefs = getSharedPreferences("TicketAppHistory", Context.MODE_PRIVATE)
        val jsonList = prefs.getString("history_list", "[]")
        val type = object : com.google.gson.reflect.TypeToken<ArrayList<TicketHistoryItem>>() {}.type
        val currentHistory: ArrayList<TicketHistoryItem> = gson.fromJson(jsonList, type) ?: ArrayList()

        // 2. Buscar y eliminar el ticket específico (por ID)
        val iterator = currentHistory.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.id == itemToDelete.id) {
                iterator.remove()
                break
            }
        }

        // 3. Guardar la lista actualizada
        prefs.edit().putString("history_list", gson.toJson(currentHistory)).apply()

        // 4. Recargar la pantalla (Esto es importante para que se actualicen los encabezados de fecha si quedan vacíos)
        loadHistory()
        
        Toast.makeText(this, "Ticket eliminado", Toast.LENGTH_SHORT).show()
    }
}



