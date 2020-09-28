package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.util.Log
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class ConnectThread(device: BluetoothDevice) : Thread() {
    private val logTag = "Bluetooth_Connect_Thread"
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mmSocket: BluetoothSocket? = null
    init {
        if(mmSocket == null){
            Log.i("Bluetooth", "Socket was null so created socket")
             mmSocket = device.createInsecureRfcommSocketToServiceRecord(BlueToothHelper._UUID)
        }
    }

     override fun run() {
        // Cancel discovery because it otherwise slows down the connection.
        bluetoothAdapter?.cancelDiscovery()
        try{
            mmSocket?.connect()
            Log.i("Bluetooth", "socket is connected. trying to send test message")
            if(mmSocket!!.isConnected){
                BluetoothDataCenter.blueToothSocketIsConnected(mmSocket!!)
            }
            } catch (e: IOException){
            Log.i("Bluetooth", "socket error. $e")
            BluetoothDataCenter.blueToothSocketIsDisconnected()
            BluetoothDataCenter.disconnectedToDriverTablet()
           }
        }
         private fun cancelThread() {
             try {
                 mmSocket?.close()
                 Log.i("Bluetooth", "socket is closed")
             } catch (e: IOException) {
                 Log.e(TAG, "Could not close the client socket", e)
             }
         }


}

class ConnectedThread(private var mmSocket: BluetoothSocket) : Thread() {
    private var mmInStream: InputStream = mmSocket.inputStream
    private var mmOutStream: OutputStream = mmSocket.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    init {
        Log.i("bluetooth", "sending request packet")
        val pimStatus = VehicleTripArrayHolder.getInternalPIMStatus()
         val statusObject = NTSPimPacket.PimStatusObj()
        statusObject.pimStatus = pimStatus
        val ntsPacket = NTSPimPacket(NTSPimPacket.Command.MDT_STATUS, statusObject)
        val toBytes = ntsPacket.toBytes()
        write(toBytes)
    }

    override fun run() {
        this.name = "Connected Thread"
        var numBytes: Int // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            // Read from the InputStream.
            numBytes = try {
                mmInStream.read(mmBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "Input stream was disconnected", e)
                BluetoothDataCenter.blueToothSocketIsDisconnected()
                break
            }
            val receivedArrayMessage = mmBuffer
            Log.i("bluetooth", "message Received. $receivedArrayMessage")
           val containsStart = NTSPimPacket.containsPacketStart(receivedArrayMessage)
            if(containsStart){
                Log.i("bluetooth", "Packet contains a start")
                val isPacketAck = NTSPimPacket().isAckPacket
                Log.i("bluetooth", "Is packet received act: $isPacketAck")
                val isDriverTabletFound = BluetoothDataCenter.isConnectedToDriverTablet().value
                if(!isDriverTabletFound!!){
                    Log.i("Bluetooth", "Driver tablet sent back ack but driverTablet connected was false. Updating to connected driver tablet")
                    BluetoothDataCenter.connectedToDriverTablet()
                }
            }
        }
    }

    internal fun write(bytes: ByteArray?) {
        try {
            Log.i("bluetooth", "Writing bytes")
            mmOutStream.write(bytes)
            mmOutStream.flush()
        } catch (e: IOException) {
            Log.i("bluetooth", "error writing bytes")
            BluetoothDataCenter.blueToothSocketIsDisconnected()
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
        }
    }
}