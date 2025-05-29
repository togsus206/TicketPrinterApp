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
import java.io.ByteArrayOutputStream
import java.util.*


class BtConnect : AppCompatActivity() {

		
	// Variables para Bluetooth
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private lateinit var bluetoothReceiver: BroadcastReceiver
    private lateinit var buttonScan: Button
    private lateinit var listViewDevices: ListView

    // Las constantes van dentro del companion object
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 102
        private const val REQUEST_ENABLE_BLUETOOTH = 103
    }
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_bt)


        // Referencias a elementos Bluetooth
        buttonScan = findViewById(R.id.buttonScan)
        listViewDevices = findViewById(R.id.listViewDevices)


        // Configuración de Bluetooth
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothDevices.map { device ->
            // Se necesita el permiso BLUETOOTH_CONNECT para acceder al nombre del dispositivo
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.name ?: device.address // Si el permiso está, intenta obtener el nombre, si no, la dirección
            } else {
                device.address // Si no hay permiso, solo muestra la dirección
            }
        })
        listViewDevices.adapter = deviceListAdapter

        buttonScan.setOnClickListener {
            checkBluetoothPermissions()
        }

        setupBluetoothReceiver()
    }
    
    
    // --- Funciones de Bluetooth ---

    private fun checkBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        } else {
            enableBluetoothAndDiscover()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                enableBluetoothAndDiscover()
            } else {
                Toast.makeText(this, "Permisos de Bluetooth o ubicación denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableBluetoothAndDiscover() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Tu dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Añadir esta verificación explícita para satisfacer a Lint y asegurar el permiso BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Si llegamos aquí sin el permiso, algo salió mal en la solicitud inicial
                // o el usuario lo denegó y estamos intentando activar Bluetooth directamente.
                // Mostrar un mensaje y salir.
                Toast.makeText(this, "Permiso BLUETOOTH_CONNECT es necesario para activar Bluetooth.", Toast.LENGTH_SHORT).show()
                return
            }
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        } else {
            startBluetoothDiscovery()
        }
    }

    private fun startBluetoothDiscovery() {
        if (bluetoothAdapter == null) return

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return // Se maneja en checkBluetoothPermissions
        }

        bluetoothDevices.clear()
        deviceListAdapter.clear()
        deviceListAdapter.add("Buscando dispositivos...")
        deviceListAdapter.notifyDataSetChanged()

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
        Toast.makeText(this, "Iniciando búsqueda de dispositivos Bluetooth...", Toast.LENGTH_SHORT).show()
    }

    private fun setupBluetoothReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (!bluetoothDevices.contains(it)) {
                                bluetoothDevices.add(it)
                                deviceListAdapter.clear()
                                bluetoothDevices.forEach { dev ->
                                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                		deviceListAdapter.add(dev.name ?: dev.address)
                            		} else {
                                		deviceListAdapter.add(dev.address) // Si no hay permiso para el nombre, solo la dirección
                            		}
                                }
                                deviceListAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Toast.makeText(context, "Búsqueda finalizada", Toast.LENGTH_SHORT).show()
                        if (bluetoothDevices.isEmpty()) {
                            Toast.makeText(context, "No se encontraron dispositivos", Toast.LENGTH_SHORT).show()
                            deviceListAdapter.clear()
                            deviceListAdapter.add("No se encontraron dispositivos")
                            deviceListAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
        registerReceiver(bluetoothReceiver, filter)

        listViewDevices.setOnItemClickListener { _, _, position, _ ->
            if (position < bluetoothDevices.size) {
                val selectedDevice = bluetoothDevices[position]
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                	Toast.makeText(this, "Dispositivo seleccionado: ${selectedDevice.name ?: selectedDevice.address}", Toast.LENGTH_SHORT).show()
            	} else {
                	Toast.makeText(this, "Dispositivo seleccionado: ${selectedDevice.address}", Toast.LENGTH_SHORT).show()
            	}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothReceiver.isInitialized) {
            unregisterReceiver(bluetoothReceiver)
        }
    }


}
