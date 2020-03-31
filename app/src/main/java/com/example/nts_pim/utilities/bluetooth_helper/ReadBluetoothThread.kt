package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.nts_pim.utilities.enums.BluetoothEnums
import com.google.gson.JsonObject
import org.json.JSONObject

class ReadBluetoothThread(private val socket: BluetoothSocket): Thread() {
    private val inputStream = this.socket.inputStream
    private val packet: ntsPimPacket? = null
    override fun run() {
        Log.i("BlueTooth", "Read BlueTooth Thread Started")
        while (true){
            try {
                val available = inputStream.available()
                val bytes = ByteArray(available)
                Log.i("BlueTooth Server" , "Reading. $bytes, $available ${socket.isConnected}")
                inputStream.read(bytes, 0, available)
                val message = String(bytes)
                val isThereAPacketStart = ntsPimPacket.containsPacketStart(bytes)
                val didTheParseWork = ntsPimPacket().parseData(bytes)
                BlueToothHelper.getBlueToothData(message)

  //              val data = ntsPimPacket().packetData.toString()
                Log.i("BlueTooth Server", "Message Received: $message")
                if(message.isNotEmpty()){
                    Log.i("BlueTooth", "Updating response to BlueToothDataCenter")
                    BluetoothDataCenter.updateResponseMessage(message)
                    val json = JSONObject()
                    json.put("Response", BluetoothEnums.ACK.name)
                    try {
                        Log.i("BlueTooth", "Trying to stop Write thread with read thread")
                        WriteBluetoothThread(socket, json, true).start()
                    } catch (e: Exception){
                        Log.e("BlueTooth Error", "issue trying to send back success response for message received")
                    }
                }
            } catch (e: Exception) {
                Log.e("BlueTooth", "Cannot read data", e)
            }
        }

    }
}