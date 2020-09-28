package com.example.nts_pim.utilities.bluetooth_helper

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Note_
 * The logic is to see if it is connected to bluetooth in aws during start up. Connect socket during bluetooth pairing if needed. Use connected to Driver tablet value to send/receive and make threads.
 */
object BluetoothDataCenter {
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
    private var btDriverTablet: BluetoothDevice? = null
    val logtag = "Bluetooth_Data_Center"

    init {
        responseMessage = ""
        connectedToDriverTabletMLD.postValue(connectedToDriverTablet)
    }


    internal fun updateResponseMessage(message: String){
        if(message != responseMessage) {
            responseMessage = message
            responseMessageMutableLiveData.postValue(responseMessage)
        }
    }

    internal fun updateDriverTabletBTDevice(device: BluetoothDevice){
        if(device != btDriverTablet){
            btDriverTablet = device
            Log.i("$logtag", "Data Center has been updated with driver tablet for connection.")
            blueToothDeviceFound()
        }
    }
    private fun blueToothDeviceFound(){
        driverTabletFound = true
        driverTabletFoundMLD.value = driverTabletFound
    }

    internal fun getIsDeviceFound() = driverTabletFoundMLD

    internal fun getDriverTablet(): BluetoothDevice?{
        return btDriverTablet
    }
    internal fun getBTSocket():BluetoothSocket? = blueToothSocket

    internal fun turnOnBlueTooth() {
        useBluetooth = true
        useBlueToothMLD.value = useBluetooth
        Log.i("$logtag", "bluetooth pairing is turning on internally")
    }
    internal fun turnOffBlueTooth(){
        useBluetooth = false
        useBlueToothMLD.value = useBluetooth
        Log.i("$logtag", "bluetooth pairing is turning off internally")
    }

    internal fun isBluetoothOn():LiveData<Boolean> = useBlueToothMLD

    internal fun blueToothSocketIsConnected(socket: BluetoothSocket){
        if(blueToothSocket == null){
            blueToothSocket = socket
            Log.i(logtag, "blueTooth socket set on Bluetooth Data center")
        }
        blueToothSocketAccepted = true
        bluetoothSocketAcceptedMLD.postValue(blueToothSocketAccepted)

    }

    internal fun blueToothSocketIsDisconnected(){
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