package com.example.nts_pim.utilities.bluetooth_helper

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.model_objects.trip.Destination
import com.example.nts_pim.data.repository.model_objects.trip.Passenger
import com.example.nts_pim.data.repository.model_objects.trip.UpfrontTrip
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


object BlueToothHelper {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    @SuppressLint("MissingPermission")
    private val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
    val _UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


    @SuppressLint("MissingPermission")
    internal fun getPairedDevicesAndRegisterBTReceiver(): MutableList<Pair<String, String>>{

        val deviceArrayWithNamePairs:MutableList<Pair<String, String>> = ArrayList()
        pairedDevices?.forEach {device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            val devicePair = Pair(deviceName, deviceHardwareAddress)
            deviceArrayWithNamePairs.add(devicePair)
        }
        return deviceArrayWithNamePairs
    }

    internal fun parseBlueToothData(byteArray: ByteArray): Boolean {
        val mutableList = arrayListOf<Byte>()
        val endByte = 0x03
        val JSON_COMMAND = "Command"
        val JSON_DATA = "Data"
        //will take a byte array - set it into a mutable array- remove start byte and remove byte or the substring in-between. turn that into a string and we should get message.
        for ((index,byte) in byteArray.withIndex()){
            if(index != 0 && byte != endByte.toByte()){
                mutableList.add(byte)
            }
            if(byte == endByte.toByte()){
                val string = String(mutableList.toByteArray())
                //This is the whole packet without start and end byte
                val json = JSONObject(string)
                //This is the command from the packet
                val command = json.getString(JSON_COMMAND)
                //check the command Here

                // This is the part that would have an relevant data.
                val data = json.optJSONObject(JSON_DATA)

                return checkForACKNAKCommands(command, data)
            }
        }
        return  false
    }

    private fun checkForACKNAKCommands(command: String, data: JSONObject?): Boolean{
        // we are going to check to see if this is an ACK or Nak
        Log.i("Bluetooth", "Filtered command from data parse == $command")
        when(command){
            NTSPimPacket.Command.ACK.command -> {
            // stop sending previous message
                Log.i("Bluetooth", "Command was an ACK from data parse")
                LoggerHelper.writeToLog("Command received was an ACK", LogEnums.BLUETOOTH.tag)
                return true
            }
            NTSPimPacket.Command.NACK.command -> {
                // we will want to resend package
                Log.i("Bluetooth", "Command was a NACK from data parse")
                LoggerHelper.writeToLog("Command received was a NACK", LogEnums.BLUETOOTH.tag)
                return false
            }

            NTSPimPacket.Command.MDT_STATUS.command -> {
                if(data != null){
                    sendACK("MDT_STATUS: Packet: $data")
                    LoggerHelper.writeToLog("MDT_ Status being received. Status JsonObject: $data", LogEnums.BLUETOOTH.tag)
                   NTSPimPacket.MdtStatusObj(null,null,null,null,null,null,null,null).fromJson(data)
                }
            }
            NTSPimPacket.Command.PIM_PAYMENT.command -> {
                LoggerHelper.writeToLog("PIM Received PIM_Payment packet. Something is wrong", LogEnums.BLUETOOTH.tag)
            }

            NTSPimPacket.Command.PAYMENT_DECLINED.command -> {
                LoggerHelper.writeToLog("PIM Received Payment_declined packet. Something is wrong", LogEnums.BLUETOOTH.tag)
            }
            NTSPimPacket.Command.PIM_STATUS.command-> {
                LoggerHelper.writeToLog("PIM Received PIM_Status packet. Something is wrong", LogEnums.BLUETOOTH.tag)
            }
            NTSPimPacket.Command.STATUS_REQ.command -> {
                sendACK("STATUS_REQ")
                val dataObject = NTSPimPacket.PimStatusObj()
                val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
                LoggerHelper.writeToLog("status request being sent: ${statusObj.command}", LogEnums.BLUETOOTH.tag)
                MainActivity.mainActivity.sendBluetoothPacket(statusObj)
            }
            NTSPimPacket.Command.GET_UPFRONT_PRICE.command -> {

            }
            NTSPimPacket.Command.UPFRONT_PRICE.command -> {
                LoggerHelper.writeToLog("Upfront_Price_ received. Status JsonObject: $data", LogEnums.BLUETOOTH.tag)
                sendACK(NTSPimPacket.Command.UPFRONT_PRICE.command)
                if(data != null){

                    UpfrontTrip(null, null, null, null, false, null).fromJson(data)
                }
            }
            NTSPimPacket.Command.UPDATE_TRIP.command -> {

            }
        }
        return false
    }

    internal fun sendPaymentInfo(activity: Activity){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            val dataObject = NTSPimPacket.PimPaymentObj()
            val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_PAYMENT, dataObject)
            LoggerHelper.writeToLog("Sending Payment Info: ${dataObject}", LogEnums.BLUETOOTH.tag)
            (activity as MainActivity).sendBluetoothPacket(statusObj)
        }
    }

    internal fun sendDeclinedCardInfo(activity: Activity){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            val dataObject = NTSPimPacket.PaymentDeclinedObj()
            val statusObj =  NTSPimPacket(NTSPimPacket.Command.PAYMENT_DECLINED, dataObject)
            LoggerHelper.writeToLog("Sending Declined Card Info: $statusObj", LogEnums.BLUETOOTH.tag)
            (activity as MainActivity).sendBluetoothPacket(statusObj)
        }
    }
    private fun sendACK(command: String) {
        val data = NTSPimPacket(NTSPimPacket.Command.ACK, null)
        MainActivity.mainActivity.sendACKPacket(data)
        LoggerHelper.writeToLog("Sending ACK for $command", LogEnums.BLUETOOTH.tag)
    }

    internal fun sendNACK(){
        val data = NTSPimPacket(NTSPimPacket.Command.NACK, null)
        MainActivity.mainActivity.sendBluetoothPacket(data)
        LoggerHelper.writeToLog("Sending ACK", LogEnums.BLUETOOTH.tag)
    }

    fun sendGetUpFrontPricePacket(destination: Destination, activity: Activity){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn) {
            LoggerHelper.writeToLog("Sending getUpFrontBTPacket for Destination: $destination", LogEnums.BLUETOOTH.tag)
            val getUpFrontPricePacket =
                NTSPimPacket(NTSPimPacket.Command.GET_UPFRONT_PRICE, destination)
            (activity as MainActivity).sendBluetoothPacket(getUpFrontPricePacket)
        }
    }

    fun sendUpdateTripPacket(name: String, activity: Activity){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            LoggerHelper.writeToLog("Sending Update Trip BT Packet. PassengerName: $name", LogEnums.BLUETOOTH.tag)
            val passenger = Passenger(name)
            val updateTripPacket = NTSPimPacket(NTSPimPacket.Command.UPDATE_TRIP, passenger)
            (activity as MainActivity).sendBluetoothPacket(updateTripPacket)
        }
    }
}