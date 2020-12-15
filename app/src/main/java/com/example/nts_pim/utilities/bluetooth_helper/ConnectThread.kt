package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.ConditionVariable
import android.os.Handler
import android.util.Log
import androidx.core.graphics.convertTo
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * We connect the socket and report the socket to be connected to the bluetooth data center. If it doesn't connect
 * we report and socket disconnection. If the read or write thread gets an error we report that the read/write thread have disconnected from the driver tablet
 * If disconnected from driver tablet we do a full socket reconnection process. If we get a socket error we just need to cancel the connect thread and restart it.
 */
class ConnectThread(device: BluetoothDevice) : Thread() {
    private val logTag = "Bluetooth_Connect_Thread"
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mDevice = device
    private var mmSocket: BluetoothSocket? = null
    private var hasBeenInit = false
    private val connectThreadHandler = Handler()

    init {
        if(mmSocket == null){
            this.name = "Connect_Thread"
            Log.i("Bluetooth", "Socket was null so created socket")
            if(!hasBeenInit){
                hasBeenInit = true
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(BlueToothHelper._UUID)
            }
        }
    }

     override fun run() {
        // Cancel discovery because it otherwise slows down the connection.
        try{
            mmSocket?.connect()
            Log.i("Bluetooth", "socket is connected. trying to send test message")
            if(mmSocket!!.isConnected){
                BluetoothDataCenter.blueToothSocketIsConnected(mmSocket!!)
            }
            } catch (e: IOException){
            Log.i("Bluetooth", "socket error. $e")
            hasBeenInit = false
            BluetoothDataCenter.blueToothSocketIsDisconnected()
           } finally {
                if(!mmSocket!!.isConnected){
                    connectThreadHandler.postDelayed(Runnable {
                        Log.i("Bluetooth", "tried to reconnect socket")
                        ConnectThread(mDevice).start()
                    },10000)
                }
           }
        }
}

class ReadThread(private var mmSocket: BluetoothSocket?) : Thread() {
    private var mmInStream: InputStream? = mmSocket?.inputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
     var lastPacketSentSuccessful = true


    init {
        this.name = "Read_Thread"
        Log.i("Bluetooth", "${this.name} initialized" )
    }

    override fun run() {
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            // Read from the InputStream.
            try {
                mmInStream?.read(mmBuffer)
            } catch (e: IOException) {
                Log.i("Bluetooth", "Input stream was disconnected", e)
                BluetoothDataCenter.disconnectedToDriverTablet()
                break
            }
            val receivedArrayMessage = mmBuffer
           val containsStart = NTSPimPacket.containsPacketStart(receivedArrayMessage)
            if(containsStart){
                Log.i("Bluetooth", "Packet contains a start")
               val isPacketACK = BlueToothHelper.parseBlueToothData(receivedArrayMessage)
                if(isPacketACK){
                    lastPacketSentSuccessful = true
                }
                if(!isPacketACK){
                   lastPacketSentSuccessful = false
                }
                val isDriverTabletFound = BluetoothDataCenter.isConnectedToDriverTablet().value
                if(!isDriverTabletFound!!){
                    Log.i("Bluetooth", "Driver tablet sent. Updating to connected driver tablet value")
                    BluetoothDataCenter.connectedToDriverTablet()
                }
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
        }
    }
}

class WriteThread(private var mmSocket: BluetoothSocket?): Thread(){
    private var mmOutStream: OutputStream? = mmSocket?.outputStream

    init {
        this.name = "Write_Thread"
        Log.i("Bluetooth", "${this.name} initialized" )
    }

    internal fun write(bytes: ByteArray?) {
        try {
            Log.i("Bluetooth", "Writing bytes")
            mmOutStream?.write(bytes)
            mmOutStream?.flush()

        } catch (e: IOException) {
            Log.i("Bluetooth", "Output stream was disconnected", e)
            BluetoothDataCenter.disconnectedToDriverTablet()
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
        }
    }

}