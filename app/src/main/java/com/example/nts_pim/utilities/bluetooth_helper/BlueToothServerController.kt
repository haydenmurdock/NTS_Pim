package com.example.nts_pim.utilities.bluetooth_helper

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException

class BlueToothServerController(act: Activity): Thread() {
    private val _uuid = BlueToothHelper._UUID
    private var cancelled: Boolean = false
    private var serverSocket: BluetoothServerSocket? = null
    private val activity = act

         init {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
           serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("NTS", _uuid) // 1
        }
    }

    override fun run() {
        var socket: BluetoothSocket

        while (true) {

            try {
                socket = serverSocket!!.accept() // 2
            } catch (e: IOException) {
                Log.e("server", "socket error: $e")
                break
            }

            if (!this.cancelled && socket != null) {
                Log.i("server", "Connecting")
                if (socket.isConnected) {
                    BluetoothServer(this.activity, socket).start() // 3
                }
            }
        }
    }
}