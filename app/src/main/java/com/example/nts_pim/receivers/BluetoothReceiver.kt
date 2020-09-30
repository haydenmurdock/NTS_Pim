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
    private var testBTAddress = "6C:00:6B:A8:5F:3C"

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(EXTRA_DEVICE)
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    Log.i(logTag, "Device Name: $deviceName, deviceBTAddress: $deviceHardwareAddress")
                    if(deviceHardwareAddress == testBTAddress){
                        BluetoothDataCenter.updateDriverTabletBTDevice(device)
                    }
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