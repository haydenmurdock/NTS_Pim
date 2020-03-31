package com.example.nts_pim.utilities.bluetooth_helper

import android.app.Activity
import android.bluetooth.BluetoothSocket
import android.util.Log

class BluetoothServer(private val activity: Activity, private val socket: BluetoothSocket): Thread() {
    private val inputStream = this.socket.inputStream
    private val outputStream = this.socket.outputStream
    var message = ""
    override fun run() {
        try {
            Log.i("BlueTooth Server", "started Read Thread")
            ReadBluetoothThread(socket).start()
        } catch (e: Exception) {
            Log.e("client", "Cannot read data", e)
        } finally {
                Log.i("BlueTooth Server", "never gonna close this shit")
        }
    }
}