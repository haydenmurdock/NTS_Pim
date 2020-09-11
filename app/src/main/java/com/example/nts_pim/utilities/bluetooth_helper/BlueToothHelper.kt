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


    internal fun pairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod(
                "createBond",
                Any::class.java
            )
            method.invoke(device, null as Array<Any?>?)
            Log.i("BlueToothHelper", "Trying to pair to ${device.name}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun unpairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod(
                "removeBond",
                Any::class.java
            )
            method.invoke(device, null as Array<Any?>?)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    internal fun getPairedDevicesAndRegisterBTReceiver(activity: Activity): MutableList<Pair<String, String>>{
        val intent = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        activity.registerReceiver(mPairReceiver, intent)

        val deviceArrayWithNamePairs:MutableList<Pair<String, String>> = ArrayList()
        pairedDevices?.forEach {device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            val devicePair = Pair(deviceName, deviceHardwareAddress)
            deviceArrayWithNamePairs.add(devicePair)
        }
        return deviceArrayWithNamePairs
    }
    internal fun getBlueToothData(string: String){
        Log.i("Bluetooth", "received message: $string")

    }

    // This is the receiver that you would use to receive the bluetoothReceiver.
    private val mPairReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val state =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                val prevState = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                    BluetoothDevice.ERROR
                )
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                   Toast.makeText(context,
                       "Paired",
                       Toast.LENGTH_SHORT)
                       .show()
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(context,
                        "unpaired",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    internal fun unregisterBlueToothReceiver(activity: Activity){
        try{
            activity.unregisterReceiver(mPairReceiver)
        } catch (e: IllegalArgumentException){
            Log.i("Bluetooth", "Issue unregistering bluetooth receiver")
            LoggerHelper.writeToLog("Issue unregistering bluetooth receiver")
        }
    }
}