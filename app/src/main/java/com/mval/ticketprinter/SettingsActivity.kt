package com.mval.ticketprinter

import android.content.Context // Importación necesaria para Context
import android.content.Intent
import android.content.SharedPreferences // Importación necesaria para SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton // Importación necesaria para RadioButton
import android.widget.RadioGroup // Importación necesaria para RadioGroup
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
    private lateinit var buttonDeleteLogo: Button
    private lateinit var buttonDeleteQr: Button

    // [MODIFICACIÓN] Nuevos elementos para el ancho del papel
    private lateinit var radioGroupPaperWidth: RadioGroup
    private lateinit var radioButton58mm: RadioButton
    private lateinit var radioButton80mm: RadioButton

    private lateinit var sharedPreferences: SharedPreferences // [MODIFICACIÓN] Declaración de SharedPreferences

    private lateinit var logoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var qrPickerLauncher: ActivityResultLauncher<Intent>

    // Constantes para las claves de SharedPreferences (ahora en SettingsActivity)
    companion object {
        const val PREFS_NAME = "printer_settings" // Usar el nombre que ya tenías para coherencia
        const val KEY_HEADER = "header"
        const val KEY_FOOTER = "footer"
        const val KEY_PRINT_DATE_TIME = "print_date_time"
        const val KEY_LOGO_PATH = "logo_path"
        const val KEY_QR_PATH = "qr_path"
        const val KEY_PAPER_WIDTH = "paper_width" // Clave para el ancho del papel
        const val DEFAULT_PAPER_WIDTH = 58 // Ancho predeterminado en mm
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Inicializar SharedPreferences aquí, antes de usarla en `loadSettings`
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Obtener referencias a los elementos de la UI
        imageViewLogo = findViewById(R.id.imageViewLogo)
        buttonSelectLogo = findViewById(R.id.buttonSelectLogo)
        editTextHeader = findViewById(R.id.editTextHeader)
        switchDateTime = findViewById(R.id.switchDateTime)
        editTextFooter = findViewById(R.id.editTextFooter)
        imageViewQr = findViewById(R.id.imageViewQr)
        buttonSelectQr = findViewById(R.id.buttonSelectQr)
        buttonSaveSettings = findViewById(R.id.buttonSaveSettings)
        buttonDeleteLogo = findViewById(R.id.buttonDeleteLogo)
        buttonDeleteQr = findViewById(R.id.buttonDeleteQr)
        

        // [MODIFICACIÓN] Referencias para los RadioButtons
        radioGroupPaperWidth = findViewById(R.id.radioGroupPaperWidth)
        radioButton58mm = findViewById(R.id.radio58mm)
        radioButton80mm = findViewById(R.id.radio80mm)


        // Inicializar los ActivityResultLaunchers para seleccionar imágenes
        logoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedUri = result.data!!.data
                selectedUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        inputStream?.use { input ->
                            val logoFileName = "logo.png"
                            val outputStream = openFileOutput(logoFileName, MODE_PRIVATE)
                            outputStream.use { output ->
                                input.copyTo(output)
                                val logoPath = getFileStreamPath(logoFileName).absolutePath
                                sharedPreferences.edit().putString(KEY_LOGO_PATH, logoPath).apply() // [MODIFICACIÓN] Usar constante
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
                            val qrFileName = "qr.png"
                            val outputStream = openFileOutput(qrFileName, MODE_PRIVATE)
                            outputStream.use { output ->
                                input.copyTo(output)
                                val qrPath = getFileStreamPath(qrFileName).absolutePath
                                sharedPreferences.edit().putString(KEY_QR_PATH, qrPath).apply() // [MODIFICACIÓN] Usar constante
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
        
        // AGREGADO: Listener para el botón de eliminar logo
        buttonDeleteLogo.setOnClickListener {
            with(sharedPreferences.edit()) {
                remove(KEY_LOGO_PATH) // Elimina la ruta del logo de SharedPreferences
                apply()
            }
            imageViewLogo.setImageResource(android.R.drawable.ic_menu_gallery) // Restablece la imagen a la predeterminada
            Toast.makeText(this, "Logo eliminado", Toast.LENGTH_SHORT).show()
        }

        buttonSelectQr.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            qrPickerLauncher.launch(intent)
        }
        
        // AGREGADO: Listener para el botón de eliminar QR
        buttonDeleteQr.setOnClickListener {
            with(sharedPreferences.edit()) {
                remove(KEY_QR_PATH) // Elimina la ruta del QR de SharedPreferences
                apply()
            }
            imageViewQr.setImageResource(android.R.drawable.ic_menu_camera) // Restablece la imagen a la predeterminada
            Toast.makeText(this, "QR eliminado", Toast.LENGTH_SHORT).show()
        }

        // Listener para el botón "Guardar Configuración"
        buttonSaveSettings.setOnClickListener {
            val header = editTextHeader.text.toString()
            val printDateTime = switchDateTime.isChecked
            val footer = editTextFooter.text.toString()

            val editor = sharedPreferences.edit() // Usar la sharedPreferences ya inicializada

            editor.putString(KEY_HEADER, header) // [MODIFICACIÓN] Usar constante
            editor.putBoolean(KEY_PRINT_DATE_TIME, printDateTime) // [MODIFICACIÓN] Usar constante
            editor.putString(KEY_FOOTER, footer) // [MODIFICACIÓN] Usar constante

            // [MODIFICACIÓN] Guardar el ancho del papel seleccionado
            val selectedPaperWidth = when (radioGroupPaperWidth.checkedRadioButtonId) {
                R.id.radio58mm -> 58
                R.id.radio80mm -> 80
                else -> DEFAULT_PAPER_WIDTH // Valor por defecto si no se selecciona nada
            }
            editor.putInt(KEY_PAPER_WIDTH, selectedPaperWidth) // [MODIFICACIÓN] Usar constante

            editor.apply()

            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            finish()
        }

        // [MODIFICACIÓN] Listener para los RadioButtons del ancho del papel
        radioGroupPaperWidth.setOnCheckedChangeListener { _, checkedId ->
            val editor = sharedPreferences.edit()
            when (checkedId) {
                R.id.radio58mm -> editor.putInt(KEY_PAPER_WIDTH, 58)
                R.id.radio80mm -> editor.putInt(KEY_PAPER_WIDTH, 80)
            }
            editor.apply()
        }


        // Cargar la configuración guardada al iniciar la actividad
        loadSettings()
    }

    private fun loadSettings() {
        // SharedPreferences ya está inicializada en onCreate
        val logoPath = sharedPreferences.getString(KEY_LOGO_PATH, null) // [MODIFICACIÓN] Usar constante
        logoPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            imageViewLogo.setImageBitmap(bitmap)
        } ?: run {
            imageViewLogo.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        editTextHeader.setText(sharedPreferences.getString(KEY_HEADER, "")) // [MODIFICACIÓN] Usar constante
        switchDateTime.isChecked = sharedPreferences.getBoolean(KEY_PRINT_DATE_TIME, false) // [MODIFICACIÓN] Usar constante
        editTextFooter.setText(sharedPreferences.getString(KEY_FOOTER, "")) // [MODIFICACIÓN] Usar constante

        val qrPath = sharedPreferences.getString(KEY_QR_PATH, null) // [MODIFICACIÓN] Usar constante
        qrPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            imageViewQr.setImageBitmap(bitmap)
        } ?: run {
            imageViewQr.setImageResource(android.R.drawable.ic_menu_camera)
        }

        // [MODIFICACIÓN] Cargar la preferencia del ancho del papel
        val savedPaperWidth = sharedPreferences.getInt(KEY_PAPER_WIDTH, DEFAULT_PAPER_WIDTH) // [MODIFICACIÓN] Usar constante
        when (savedPaperWidth) {
            58 -> radioButton58mm.isChecked = true
            80 -> radioButton80mm.isChecked = true
        }
    }
}
