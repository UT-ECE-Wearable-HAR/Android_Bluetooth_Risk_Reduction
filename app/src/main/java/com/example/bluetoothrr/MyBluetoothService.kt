package com.example.bluetoothrr

import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.NumberFormatException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

private const val TAG = "MY_APP_DEBUG_TAG"

// Defines several constants used when transmitting messages between the
// service and the UI.
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
// ... (Add other message types here as needed.)

class MyBluetoothService(
        // handler that gets info from Bluetooth service
        private val handler: Handler) {

    fun readImages(socket: BluetoothSocket){
        val connectedThread: ConnectedThread = ConnectedThread(socket)
        connectedThread.run()
        Log.e(null, "done running")
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // try to read 1000 JPEGs
            for(i in 0 until 1000) {
                // Read from the InputStream.
                val jpegBitmap: Bitmap? = try {
                    getJpeg()
                } catch (e: IOException){
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                if(jpegBitmap == null){
                    continue
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                        MESSAGE_READ, -1, -1,
                        jpegBitmap)
                readMsg.sendToTarget()
                TimeUnit.MILLISECONDS.sleep(200)
            }
            cancel()
        }

        private fun getJpeg(): Bitmap? {

            val byteMessage: ByteArray = "RCV_READY".toByteArray(Charsets.US_ASCII)
            val byteArrLen: Int = byteMessage.size + 1
            val byteArrToSend: ByteArray = ByteArray(byteArrLen)
            for(i in byteMessage.indices){
                byteArrToSend[i] = byteMessage[i]
            }
            byteArrToSend[byteArrLen - 1] = 0

            mmOutStream.write(byteArrToSend)
            var numBytes = mmInStream.read(mmBuffer)
            while (numBytes > 64){
                numBytes = mmInStream.read(mmBuffer)
            }
            val headerString = String(mmBuffer, Charset.forName("UTF8")).substring(0, numBytes)
            val size = getJpegLength(headerString)
            if(size == -1){
                return null
            }
            val jpegBytes = ArrayList<Byte>()
            while(jpegBytes.size < size){
                numBytes = mmInStream.read(mmBuffer)
                jpegBytes.addAll(mmBuffer.filterIndexed { index, _ ->
                    index < numBytes
                }.asIterable())
            }
            jpegBytes.toByteArray()
            System.out.println("size: " + size.toString())
            val bitmap = BitmapFactory.decodeByteArray(jpegBytes.toByteArray(), 0, jpegBytes.size)
            return bitmap
        }

        private fun getJpegLength(headerString: String): Int{
            val headerMessage = "Content-Length: "
            if(headerString.contains(headerMessage)){
                return try {
                    headerString.substring(headerMessage.length, headerString.length - 4).toInt()
                } catch (e: NumberFormatException){
                    -1
                }
            } else {
                return -1
            }
        }

        // Call this from the main activity to send data to the remote device.
//        fun write(bytes: ByteArray) {
//            try {
//                mmOutStream.write(bytes)
//            } catch (e: IOException) {
//                Log.e(TAG, "Error occurred when sending data", e)
//
//                // Send a failure message back to the activity.
//                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
//                val bundle = Bundle().apply {
//                    putString("toast", "Couldn't send data to the other device")
//                }
//                writeErrorMsg.data = bundle
//                handler.sendMessage(writeErrorMsg)
//                return
//            }
//
//            // Share the sent message with the UI activity.
//            val writtenMsg = handler.obtainMessage(
//                    MESSAGE_WRITE, -1, -1, bytes)
//            writtenMsg.sendToTarget()
//        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}