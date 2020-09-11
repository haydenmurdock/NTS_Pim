package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class ConnectThread(device: BluetoothDevice) : Thread() {
    private val mmSocket: BluetoothSocket?
    private val mmDevice: BluetoothDevice
    var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val testMessage = "testMessage".toByteArray()
    override fun run() {
        // Cancel discovery because it will slow down the connection
        bluetoothAdapter.cancelDiscovery()
        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket!!.connect()
            ConnectedThread(mmSocket).write(testMessage)
        } catch (connectException: IOException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket!!.close()
            } catch (closeException: IOException) {
            }
            return
        }

        // Do work to manage the connection (in a separate thread)
//            bluetooth_message = "Initial message"
//            mHandler.obtainMessage(MESSAGE_WRITE,mmSocket).sendToTarget();
    }

    /** Will cancel an in-progress connection, and close the socket  */
    fun cancel() {
        try {
            mmSocket!!.close()
        } catch (e: IOException) {
        }
    }

    init {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        var tmp: BluetoothSocket? = null
        mmDevice = device

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createInsecureRfcommSocketToServiceRecord(BlueToothHelper._UUID)
        } catch (e: IOException) {
        }
        mmSocket = tmp
    }
}

internal class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?
    override fun run() {
        val buffer = ByteArray(2) // buffer store for the stream
        var bytes: Int // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream!!.read(buffer)
                // Send the obtained bytes to the UI activity
            } catch (e: IOException) {
                break
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    fun write(bytes: ByteArray?) {
        try {
            mmOutStream!!.write(bytes)
        } catch (e: IOException) {
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
        }
    }

    init {
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = mmSocket.inputStream
            tmpOut = mmSocket.outputStream
        } catch (e: IOException) {
        }
        mmInStream = tmpIn
        mmOutStream = tmpOut
    }
}