package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.utilities.logging_service.LoggerHelper

/**
 * The logic is to see if it is connected to bluetooth in aws during start up.
 * Connect socket during bluetooth pairing if needed.
 * Uses driver tablet bt socket to make Read/write threads.
 */
object BluetoothDataCenter {
    private const val logTag = "Bluetooth_Data_Center"
    internal var useBluetooth:Boolean = false
    private var useBlueToothMLD = MutableLiveData<Boolean>()
    private var blueToothSocketAccepted:Boolean = false
    private var bluetoothSocketAcceptedMLD = MutableLiveData<Boolean>()
    private var connectedToDriverTablet: Boolean = false
    private var connectedToDriverTabletMLD = MutableLiveData<Boolean>()
    private var driverTabletFound:Boolean = false
    private var driverTabletFoundMLD = MutableLiveData<Boolean>()
    private var responseMessage = ""
    private var responseMessageMutableLiveData =  MutableLiveData<String>()
    private var blueToothSocket: BluetoothSocket? = null
    private var btDriverTabletAddress: String? = null
    private var startupBTPairSuccess = false
    private var connectThreadInit = false
    private var numberOfBTAttempts = 0

    init {
        responseMessage = ""
        connectedToDriverTabletMLD.postValue(connectedToDriverTablet)
        useBlueToothMLD.postValue(useBluetooth)
        bluetoothSocketAcceptedMLD.postValue(blueToothSocketAccepted)
    }



    internal fun thereWasUnsuccessfulBTConnection(){
        numberOfBTAttempts += 1
        Log.i("Bluetooth", "Added to total BT attempts. Total attempts: $numberOfBTAttempts")
    }
    internal fun resetBluetoothPairedObserver(){
        startupBTPairSuccess = false
    }

    internal fun clearNumberOfAttempts(){
        numberOfBTAttempts = 0
        Log.i("Bluetooth", "Total BT attempts cleared. Total attempts: $numberOfBTAttempts")
    }

    internal fun howManyBTAttempts():Int {
        return numberOfBTAttempts
    }
    internal fun startUpBTPairSuccessful(){
        startupBTPairSuccess = true
    }
    internal fun wasBluetoothPaired(): Boolean{
        return startupBTPairSuccess
    }
    internal fun updateDriverTabletBTDevice(device: String){
            if(device != btDriverTabletAddress){
                btDriverTabletAddress = device
                Log.i("$logTag", "Data Center has been updated with driver tablet for connection.")
                blueToothDeviceFound()
            }
    }
    private fun blueToothDeviceFound(){
        driverTabletFound = true
        driverTabletFoundMLD.postValue(driverTabletFound)
    }

    internal fun restartConnectionWithSameDevice(){
        blueToothDeviceFound()
    }

    internal fun getIsDeviceFound() = driverTabletFoundMLD

    internal fun getDriverTabletAddress(): String?{
        return btDriverTabletAddress
    }
    internal fun getBTSocket():BluetoothSocket? = blueToothSocket

    internal fun turnOnBlueTooth() {
        useBluetooth = true
        useBlueToothMLD.postValue(useBluetooth)
        Log.i("$logTag", "bluetooth pairing is turning on internally")
    }
    internal fun turnOffBlueTooth(){
        useBluetooth = false
        useBlueToothMLD.postValue(useBluetooth)
        Log.i("$logTag", "bluetooth pairing is turning off internally")
    }

    internal fun isBluetoothOn():LiveData<Boolean> = useBlueToothMLD

    internal fun blueToothSocketIsConnected(socket: BluetoothSocket){
        blueToothSocket = socket
        Log.i("Bluetooth", "bluetooth socket set on Bluetooth Data center")
        LoggerHelper.writeToLog("bluetooth socket set on Bluetooth Data center")
        blueToothSocketAccepted = true
        bluetoothSocketAcceptedMLD.postValue(blueToothSocketAccepted)

    }

    internal fun blueToothSocketIsDisconnected(){
        blueToothSocket = null
        blueToothSocketAccepted = false
        bluetoothSocketAcceptedMLD.postValue(blueToothSocketAccepted)
    }

    internal fun isBluetoothSocketConnected():MutableLiveData<Boolean> = bluetoothSocketAcceptedMLD

    internal fun connectedToDriverTablet() {
        connectedToDriverTablet = true
        connectedToDriverTabletMLD.postValue(connectedToDriverTablet)
    }

    internal fun disconnectedToDriverTablet(){
        connectedToDriverTablet = false
        connectedToDriverTabletMLD.postValue(connectedToDriverTablet)
    }

    internal fun isConnectedToDriverTablet():LiveData<Boolean> = connectedToDriverTabletMLD
}