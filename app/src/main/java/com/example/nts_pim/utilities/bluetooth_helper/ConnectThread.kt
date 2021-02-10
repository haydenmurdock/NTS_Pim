package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * We connect the socket and report the socket to be connected to the bluetooth data center. If it doesn't connect
 * we report and socket disconnection. If the read or write thread gets an error we report that the read/write thread have disconnected from the driver tablet
 * If disconnected from driver tablet we do a full socket reconnection process. If we get a socket error we just need to cancel the connect thread and restart it.
 */
class ConnectThread(device: BluetoothDevice, activity: MainActivity) : Thread() {
    private val logTag = "Bluetooth_Connect_Thread"
    private var mDevice = device
    private var mmSocket: BluetoothSocket? = null
    private var connectThreadHandler = Handler()
    private var hasBeenInit = false
    private var mActivity: MainActivity? = null
    private var isBluetoothON = false

    init {
        if(mmSocket == null){
            this.name = "Connect_Thread"
            Log.i("Bluetooth", "Socket was null so created socket")
            LoggerHelper.writeToLog("Connect_Thread, socket was null so created socket with device")
            if(!hasBeenInit){
                hasBeenInit = true
                mActivity = activity
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(BlueToothHelper._UUID)
            }
        }
    }

     override fun run() {
        // Cancel discovery because it otherwise slows down the connection.
        isBluetoothON = BluetoothDataCenter.isBluetoothOn().value ?: false
         if(!isBluetoothON){
             Log.i("Bluetooth", "Went to run connect thread but bluetooth is off")
             LoggerHelper.writeToLog("Went to run connect thread but bluetooth is off in AWS")
             return
         }
        try{
            mmSocket?.connect()
            if(mmSocket!!.isConnected){
                BluetoothDataCenter.clearNumberOfAttempts()
                LoggerHelper.writeToLog("Connected_Thread: Socket is connected")
                Log.i("Bluetooth", "Connect_ Thread is connected. Passing socket to BluetoothDataCenter/MainActivity to create write/read/ack threads")
                BluetoothDataCenter.blueToothSocketIsConnected(mmSocket!!)
            }
            } catch (e: IOException){
            Log.i("Bluetooth", "socket error. $e")
            LoggerHelper.writeToLog("Driver tablet bluetooth error: $e")
            hasBeenInit = false

           } finally {
                if(!mmSocket!!.isConnected) {
                    BluetoothDataCenter.thereWasUnsuccessfulBTConnection()
                    val numberOfAttempts = BluetoothDataCenter.howManyBTAttempts()
                    if(BluetoothDataCenter.wasBluetoothPaired() && numberOfAttempts == 6){
                        Log.i("Bluetooth", "There has been 6 unsuccessful attempts for socket connection. Turning on AWS Connection")
                        LoggerHelper.writeToLog("There has been 6 unsuccessful attempts (1:00 min) for socket  re-connection. Turning on AWS Connection")
                        (mActivity as MainActivity).restartDriverTabletAWSConnection()
                    }
                    connectThreadHandler.postDelayed(Runnable {
                        Log.i("Bluetooth", "Trying to re-connect socket via connect thread handler")
                        LoggerHelper.writeToLog("Trying to re-connect socket via connect thread handler")
                        ConnectThread(mDevice, mActivity!!).start()
                    }, 10000)
                }
           }
        }
    fun cancel() {
        try {
            connectThreadHandler.removeCallbacksAndMessages(null)
            mmSocket?.close()
            Log.i("Bluetooth", "Closing socket on connect thread")
        } catch (e: IOException) {
            Log.i("Bluetooth", "Issue closing bt socket on ${this.name}. ")
        }
    }
}

class ReadThread(private var mmSocket: BluetoothSocket?, activity: MainActivity) : Thread() {
    private var mmInStream: InputStream? = mmSocket?.inputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
    var lastPacketSentSuccessful = true
    private var testConnectionHandler = Handler()
    private var mActivity: MainActivity? = null

    init {
        this.name = "Read_Thread"
        Log.i("Bluetooth", "${this.name} initialized" )
        mActivity = activity
    }

    override fun run() {
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            // Read from the InputStream.
            try {
                mmInStream?.read(mmBuffer)
            } catch (e: IOException) {
                Log.i("Bluetooth", "Input stream was disconnected", e)
                LoggerHelper.writeToLog("Input stream was disconnected, $e")
                BluetoothDataCenter.disconnectedToDriverTablet()
                break
            }
            val receivedArrayMessage = mmBuffer
            val containsStart = NTSPimPacket.containsPacketStart(receivedArrayMessage)
            resetReceivedPacket()
            if(containsStart){
                Log.i("Bluetooth", "Packet contains a start")
                LoggerHelper.writeToLog("Packet was received from, contained start byte. Starting parse of data")
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
            } else {
                LoggerHelper.writeToLog("Received packet from driver tablet, but didn't contain a start byte")
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            testConnectionHandler.removeCallbacksAndMessages(null)
            mmSocket?.close()
            LoggerHelper.writeToLog("${this.name} bt socket closed.")
            Log.i("Bluetooth", "${this.name} bt socket closed. Most likely closed by a main activity")
        } catch (e: IOException) {
            LoggerHelper.writeToLog("Error closing ${this.name} socket. $e")
        }
    }
    private fun resetReceivedPacket(){
        testConnectionHandler.removeCallbacksAndMessages(null)
        LoggerHelper.writeToLog("test connection handler removed messages and callbacks. Resetting test connection handler")
        testConnectionHandler.postDelayed({
            LoggerHelper.writeToLog("Packet timer hit 1 min without getting a packet. Requesting driver tablet status.")
            mActivity?.requestDriverTabletStatus()
        },60000)
    }
}

class WriteThread(private var mmSocket: BluetoothSocket?, activity: MainActivity): Thread() {
    private var mmOutStream: OutputStream? = mmSocket?.outputStream
    private var mActivity: MainActivity? = null

    init {
        this.name = "Write_Thread"
        Log.i("Bluetooth", "${this.name} initialized")
        mActivity = activity
    }

    internal fun write(bytes: ByteArray?) {
        try {
            Log.i("Bluetooth", "Writing bytes")
            mmOutStream?.write(bytes)
            mmOutStream?.flush()
        } catch (e: IOException) {
            Log.i("Bluetooth", "Output stream was disconnected", e)
            LoggerHelper.writeToLog("Output stream was disconnected. $e")
            val readThread = mActivity?.readThread?.isAlive ?: false
            if(readThread){
                Log.i("Bluetooth", "read thread is active, but write thread threw $e. Running disconnected to driver tablet protocol via Write Thread")
                LoggerHelper.writeToLog("read thread is active, but write thread threw $e. Running disconnected to driver tablet protocol via Write Thread")
                BluetoothDataCenter.disconnectedToDriverTablet()
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket?.close()
            LoggerHelper.writeToLog("${this.name} bt socket closed.")
        } catch (e: IOException) {
            LoggerHelper.writeToLog("Error closing ${this.name} socket. $e")
        }
    }
}
class ACKThread(private var mmSocket: BluetoothSocket?, activity: MainActivity): Thread() {
    private var mmOutStream: OutputStream? = mmSocket?.outputStream

    init {
        this.name = "ACK_Thread"
        Log.i("Bluetooth", "${this.name} initialized")
    }

    internal fun write(bytes: ByteArray?) {
        try {
            Log.i("Bluetooth", "Writing bytes")
            mmOutStream?.write(bytes)
            mmOutStream?.flush()
        } catch (e: IOException) {
            Log.i("Bluetooth", "ACK/NACK stream was disconnected", e)
            LoggerHelper.writeToLog("ACK/NACK  was disconnected. $e")
        }
    }

  fun cancel() {
        try {
            mmSocket?.close()
            LoggerHelper.writeToLog("${this.name} bt socket closed.")
        } catch (e: IOException) {
            LoggerHelper.writeToLog("Error closing ${this.name} socket. $e")
        }
    }
}