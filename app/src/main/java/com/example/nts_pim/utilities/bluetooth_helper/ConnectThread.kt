package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.SetupHolder
import com.example.nts_pim.utilities.enums.LogEnums
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
    private var mDevice = device
    private var mmSocket: BluetoothSocket? = null
    private var connectThreadHandler = Handler()
    private var hasBeenInit = false
    private var mActivity: MainActivity? = null
    private var isBluetoothON = false

    init {
        if(mmSocket == null){
            this.name = "Connect_Thread"
            LoggerHelper.writeToLog("Connect_Thread, socket was null so created socket with device", LogEnums.BLUETOOTH.tag)
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
             LoggerHelper.writeToLog("Went to run connect thread but bluetooth is off in AWS", LogEnums.BLUETOOTH.tag)
             return
         }
         try{
             mmSocket?.connect()
             if(mmSocket!!.isConnected){
                     BluetoothDataCenter.clearNumberOfAttempts()
                     LoggerHelper.writeToLog("Connected_Thread: Socket is connected", LogEnums.BLUETOOTH.tag)
                     SetupHolder.foundDriverTablet()
                     BluetoothDataCenter.blueToothSocketIsConnected(mmSocket!!)
                 }
             } catch (e: IOException){
                 LoggerHelper.writeToLog("Driver tablet bluetooth error: $e", LogEnums.BLUETOOTH.tag)
                 hasBeenInit = false
             } finally {
                 if(!mmSocket!!.isConnected) {
                     BluetoothDataCenter.thereWasUnsuccessfulBTConnection()
                     val numberOfAttempts = BluetoothDataCenter.howManyBTAttempts()
                     if(BluetoothDataCenter.wasBluetoothPaired() && numberOfAttempts == 6){
                         LoggerHelper.writeToLog("There has been 6 unsuccessful attempts (1:00 min) for socket  re-connection. Turning on AWS Connection", LogEnums.BLUETOOTH.tag)
                         (mActivity as MainActivity).restartDriverTabletAWSConnection()
                     }
                     connectThreadHandler.postDelayed(Runnable {
                         LoggerHelper.writeToLog("Trying to re-connect socket via connect thread handler", LogEnums.BLUETOOTH.tag)
                         ConnectThread(mDevice, mActivity!!).start()
                     }, 10000)
                 }
             }
         }

    fun cancel() {
        try {
            connectThreadHandler.removeCallbacksAndMessages(null)
            mmSocket?.close()
        } catch (e: IOException) {
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
        mActivity = activity
    }

    override fun run() {
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            // Read from the InputStream.
            try {
                mmInStream?.read(mmBuffer)
            } catch (e: IOException) {
                LoggerHelper.writeToLog("Input stream was disconnected, $e", LogEnums.BLUETOOTH.tag)
                BluetoothDataCenter.disconnectedToDriverTablet()
                break
            }
            val receivedArrayMessage = mmBuffer
            val containsStart = NTSPimPacket.containsPacketStart(receivedArrayMessage)
            resetReceivedPacket()
            if(containsStart){
                LoggerHelper.writeToLog("Packet was received from, contained start byte. Starting parse of data", LogEnums.BLUETOOTH.tag)
               val isPacketACK = BlueToothHelper.parseBlueToothData(receivedArrayMessage)
                if(isPacketACK){
                    lastPacketSentSuccessful = true
                }
                if(!isPacketACK){
                    lastPacketSentSuccessful = false
                }
                val isDriverTabletFound = BluetoothDataCenter.isConnectedToDriverTablet().value
                if(!isDriverTabletFound!!){
                    BluetoothDataCenter.connectedToDriverTablet()
                }
            } else {
                LoggerHelper.writeToLog("Received packet from driver tablet, but didn't contain a start byte", LogEnums.BLUETOOTH.tag)
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            testConnectionHandler.removeCallbacksAndMessages(null)
            mmSocket?.close()
            LoggerHelper.writeToLog("${this.name} bt socket closed.", LogEnums.BLUETOOTH.tag)
        } catch (e: IOException) {
            LoggerHelper.writeToLog("Error closing ${this.name} socket. $e", LogEnums.BLUETOOTH.tag)
        }
    }
    private fun resetReceivedPacket(){
        testConnectionHandler.removeCallbacksAndMessages(null)
        LoggerHelper.writeToLog("test connection handler removed messages and callbacks. Resetting test connection handler", LogEnums.BLUETOOTH.tag)
        testConnectionHandler.postDelayed({
            LoggerHelper.writeToLog("Packet timer hit 1 min without getting a packet. Requesting driver tablet status.", LogEnums.BLUETOOTH.tag)
            mActivity?.requestDriverTabletStatus()
        },60000)
    }
}

class WriteThread(private var mmSocket: BluetoothSocket?, activity: MainActivity): Thread() {
    private var mmOutStream: OutputStream? = mmSocket?.outputStream
    private var mActivity: MainActivity? = null

    init {
        this.name = "Write_Thread"
        mActivity = activity
    }

    internal fun write(bytes: ByteArray?) {
        try {
            mmOutStream?.write(bytes)
            mmOutStream?.flush()
        } catch (e: IOException) {
            LoggerHelper.writeToLog("Output stream was disconnected. $e", LogEnums.BLUETOOTH.tag)
            val readThread = mActivity?.readThread?.isAlive ?: false
            if(readThread){
                LoggerHelper.writeToLog("read thread is active, but write thread threw $e. Running disconnected to driver tablet protocol via Write Thread", LogEnums.BLUETOOTH.tag)
                BluetoothDataCenter.disconnectedToDriverTablet()
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket?.close()
            LoggerHelper.writeToLog("${this.name} bt socket closed.", LogEnums.BLUETOOTH.tag)
        } catch (e: IOException) {
            LoggerHelper.writeToLog("Error closing ${this.name} socket. $e", LogEnums.BLUETOOTH.tag)
        }
    }
}
class ACKThread(private var mmSocket: BluetoothSocket?, activity: MainActivity): Thread() {
    private var mmOutStream: OutputStream? = mmSocket?.outputStream

    init {
        this.name = "ACK_Thread"
    }

    internal fun write(bytes: ByteArray?) {
        try {
            mmOutStream?.write(bytes)
            SetupHolder.receivedTestPacket()
            mmOutStream?.flush()
        } catch (e: IOException) {
            LoggerHelper.writeToLog("ACK/NACK  was disconnected. $e", LogEnums.BLUETOOTH.tag)
        }

    }

  fun cancel() {
        try {
            mmSocket?.close()
            LoggerHelper.writeToLog("${this.name} bt socket closed.", LogEnums.BLUETOOTH.tag)
        } catch (e: IOException) {
            LoggerHelper.writeToLog("Error closing ${this.name} socket. $e", LogEnums.BLUETOOTH.tag)
        }
    }
}