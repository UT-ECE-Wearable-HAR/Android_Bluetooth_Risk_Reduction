package com.example.bluetoothrr

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.*

const val REQUEST_ENABLE_BT = 5
 //Note to change some consts to an uncommitted file since we have a public repo
val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
const val NAME = "Bluetooth Risk Reduction Android"
var bluetoothAdapter: BluetoothAdapter? = null

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    fun connectDevice(view: View) {
        findViewById<Button>(R.id.connect_button).isEnabled = false
        val macAddress = findViewById<EditText>(R.id.mac_address_field).text.toString().trim()
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(macAddress)
        val connectThread: ConnectThread? = device?.let { ConnectThread(it) }
        connectThread?.start()
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket){
        val btService: MyBluetoothService = MyBluetoothService(mhandler)
        btService.readImages(socket)
    }

    private val mhandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when(msg.what){
                MESSAGE_READ -> {
                    //(findViewById<TextView>(R.id.center_text)).text = String(msg.obj as ByteArray)
                    (findViewById<ImageView>(R.id.camera_feed)).setImageBitmap(msg.obj as Bitmap)
                }
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createInsecureRfcommSocketToServiceRecord(MY_UUID)
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(socket)
            }
            Log.e(null, "resource was closed")
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(null, "Could not close the client socket", e)
            }
        }
    }
}