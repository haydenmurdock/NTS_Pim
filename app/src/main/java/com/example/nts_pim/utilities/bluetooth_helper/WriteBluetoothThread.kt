package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothSocket
import android.util.Log
import org.json.JSONObject

class WriteBluetoothThread(private val blueToothSocket: BluetoothSocket?, val json: JSONObject?, isResponse: Boolean): Thread(){
    private val infoReceived = isResponse
    private val outputStream = this.blueToothSocket?.outputStream
    override fun run() {
        while(!infoReceived){
            val bytes = json.toString().toByteArray()
            outputStream?.write(bytes)
            Log.i("Bluetooth","Sending ${json.toString()} to PIM")
        }
        if(infoReceived){
            val bytes = json.toString().toByteArray()
            outputStream?.write(bytes)
            Log.i("Bluetooth","Sending ${json.toString()} to PIM")
        }
    }
}