package com.example.nts_pim.receivers

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter


class BluetoothReceiver: BroadcastReceiver() {
    private val logTag = "BlueTooth_Receiver"

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                }
                ACTION_ACL_CONNECTED -> {
                    Log.i(logTag, "Bluetooth device connected")
                }
                ACTION_ACL_DISCONNECTED -> {
                    Log.i(logTag, "Bluetooth device disconnected")
                }
            }
        }
}