package com.example.nts_pim.utilities.bluetooth_helper

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.lang.IllegalArgumentException
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList


object BlueToothHelper {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    @SuppressLint("MissingPermission")
    private val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
    val _UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


    @SuppressLint("MissingPermission")
    internal fun getPairedDevicesAndRegisterBTReceiver(activity: Activity): MutableList<Pair<String, String>>{

        val deviceArrayWithNamePairs:MutableList<Pair<String, String>> = ArrayList()
        pairedDevices?.forEach {device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            val devicePair = Pair(deviceName, deviceHardwareAddress)
            deviceArrayWithNamePairs.add(devicePair)
        }
        return deviceArrayWithNamePairs
    }
}