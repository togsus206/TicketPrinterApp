package com.mval.ticketprinter

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import android.util.Log // Asegúrate de mantener esta importación

class SettingsActivity : AppCompatActivity() {

    private lateinit var imageViewLogo: ImageView
    private lateinit var buttonSelectLogo: Button
    private lateinit var editTextHeader: EditText
    private lateinit var switchDateTime: Switch
    private lateinit var editTextFooter: EditText
    private lateinit var imageViewQr: ImageView
    private lateinit var buttonSelectQr: Button
    private lateinit var buttonSaveSettings: Button

    // private var logoUri: Uri? = null // Eliminado
    // private var qrUri: Uri? = null   // Eliminado

    private lateinit var logoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var qrPickerLauncher: ActivityResultLauncher<Intent>

    // private val READ_STORAGE_PERMISSION_CODE = 101 // Ya no es necesario para cargar imágenes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Obtener referencias a los elementos de la UI
        imageViewLogo = findViewById(R.id.imageViewLogo)
        buttonSelectLogo = findViewById(R.id.buttonSelectLogo)
        editTextHeader = findViewById(R.id.editTextHeader)
        switchDateTime = findViewById(R.id.switchDateTime)
        editTextFooter = findViewById(R.id.editTextFooter)
        imageViewQr = findViewById(R.id.imageViewQr)
        buttonSelectQr = findViewById(R.id.buttonSelectQr)
        buttonSaveSettings = findViewById(R.id.buttonSaveSettings)

        // Inicializar los ActivityResultLaunchers para seleccionar imágenes
        logoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedUri = result.data!!.data
                selectedUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        inputStream?.use { input ->
                            val logoFileName = "logo.png" // Nombre fijo para el archivo del logo
                            val outputStream = openFileOutput(logoFileName, MODE_PRIVATE)
                            outputStream.use { output ->
                                input.copyTo(output)
                                // Guardar la ruta del archivo interno en SharedPreferences
                                val logoPath = getFileStreamPath(logoFileName).absolutePath
                                val sharedPreferences = getSharedPreferences("printer_settings", MODE_PRIVATE)
                                sharedPreferences.edit().putString("logo_path", logoPath).apply()

                                // Cargar la imagen desde la ruta interna para mostrarla inmediatamente
                                val bitmap = BitmapFactory.decodeFile(logoPath)
                                imageViewLogo.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        qrPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedUri = result.data!!.data
                selectedUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        inputStream?.use { input ->
                            val qrFileName = "qr.png" // Nombre fijo para el archivo del QR
                            val outputStream = openFileOutput(qrFileName, MODE_PRIVATE)
                            outputStream.use { output ->
                                input.copyTo(output)
                                // Guardar la ruta del archivo interno en SharedPreferences
                                val qrPath = getFileStreamPath(qrFileName).absolutePath
                                val sharedPreferences = getSharedPreferences("printer_settings", MODE_PRIVATE)
                                sharedPreferences.edit().putString("qr_path", qrPath).apply()

                                // Cargar la imagen desde la ruta interna para mostrarla inmediatamente
                                val bitmap = BitmapFactory.decodeFile(qrPath)
                                imageViewQr.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error al guardar la imagen QR", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Listeners para los botones de selección de imagen
        buttonSelectLogo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            logoPickerLauncher.launch(intent)
        }

        buttonSelectQr.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            qrPickerLauncher.launch(intent)
        }

        // Listener para el botón "Guardar Configuración"
        buttonSaveSettings.setOnClickListener {
            // Aquí guardaremos la configuración utilizando SharedPreferences
            val header = editTextHeader.text.toString()
            val printDateTime = switchDateTime.isChecked
            val footer = editTextFooter.text.toString()

            val sharedPreferences = getSharedPreferences("printer_settings", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            editor.putString("header", header)
            editor.putBoolean("print_date_time", printDateTime)
            editor.putString("footer", footer)

            editor.apply()

            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            finish() // Volver a la MainActivity después de guardar
        }

        // Cargar la configuración guardada al iniciar la actividad
        loadSettings()
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("printer_settings", MODE_PRIVATE)

        // Cargar el logo desde la ruta interna si existe
        val logoPath = sharedPreferences.getString("logo_path", null)
        logoPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            imageViewLogo.setImageBitmap(bitmap)
        } ?: run {
            // Si no hay ruta guardada, mostrar un placeholder o dejar vacío
            imageViewLogo.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        editTextHeader.setText(sharedPreferences.getString("header", ""))
        switchDateTime.isChecked = sharedPreferences.getBoolean("print_date_time", false)
        editTextFooter.setText(sharedPreferences.getString("footer", ""))

        // Cargar el QR desde la ruta interna si existe
        val qrPath = sharedPreferences.getString("qr_path", null)
        qrPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            imageViewQr.setImageBitmap(bitmap)
        } ?: run {
            // Si no hay ruta guardada, mostrar un placeholder o dejar vacío
            imageViewQr.setImageResource(android.R.drawable.ic_menu_camera)
        }
    }

    // override fun onRequestPermissionsResult(...) { ... } // Puedes eliminar esta función
}
