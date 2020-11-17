package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.ConditionVariable
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentLinkedQueue


class ConnectThread(device: BluetoothDevice) : Thread() {
    private val logTag = "Bluetooth_Connect_Thread"
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mmSocket: BluetoothSocket? = null
    private var hasBeenInit = false

    init {
        if(mmSocket == null){
            Log.i("Bluetooth", "Socket was null so created socket")
            if(!hasBeenInit){
                hasBeenInit = true
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(BlueToothHelper._UUID)
            }
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
            hasBeenInit = false
           } finally {

           }
        }
         fun cancelThread() {
             try {
                 mmSocket?.close()
                 Log.i("Bluetooth", "socket is closed")
             } catch (e: IOException) {
                 Log.e(TAG, "Could not close the client socket", e)
             }
         }


}

class ConnectedThread(private var mmSocket: BluetoothSocket?) : Thread() {
    private var mmInStream: InputStream? = mmSocket?.inputStream
    private var mmOutStream: OutputStream? = mmSocket?.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
    private var lastPacketSentSuccessful = true
    private var _ackWait = ConditionVariable()


    init {

    }

    override fun run() {
        this.name = "Connected Thread"
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            // Read from the InputStream.
            try {
                mmInStream?.read(mmBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "Input stream was disconnected", e)
                BluetoothDataCenter.blueToothSocketIsDisconnected()
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

    internal fun write(bytes: ByteArray?) {
        val concurrentList = ConcurrentLinkedQueue<ByteArray>()

        if(!concurrentList.contains(bytes)){
            concurrentList.add(bytes)
            Log.i("Bluetooth", "Queue list length is ${concurrentList.size}")
        }
        _ackWait.close()
        try {
            Log.i("Bluetooth", "Writing bytes")
            mmOutStream?.write(concurrentList.first())
            mmOutStream?.flush()
        } catch (e: IOException) {
            Log.i("Bluetooth", "error writing bytes")
        }
        finally {
          if(concurrentList.count() > 0){
              if(_ackWait.block(3000)){
                  if(lastPacketSentSuccessful){
                      concurrentList.remove()
                      val queueSize = concurrentList.size
                      Log.i("Bluetooth", "last packet was successful. Removing last element in queue. new size is $queueSize")
                  } else {
                      // This means that the last packet was not successfully sent we need to send it again.
                      Log.i("Bluetooth", "last packet wasn't successful. resending packet")
                      write(bytes)
                  }
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