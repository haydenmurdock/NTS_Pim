package com.example.nts_pim.utilities.bluetooth_helper

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.nts_pim.activity.MainActivity
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

                // This is the part tha would have an relevant data.
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

            "ACK" -> {
            // stop sending previous message
                Log.i("Bluetooth", "Command was an ACK from data parse")
                LoggerHelper.writeToLog("Command received was an ACK")
                return true
            }
            "NACK" -> {
                // we will want to resend package
                Log.i("Bluetooth", "Command was a NACK from data parse")
                LoggerHelper.writeToLog("Command received was a NACK")
                return false
            }

            "MDT_STATUS" -> {
                if(data != null){
                    sendACK("MDT_STATUS: Packet: $data")
                    LoggerHelper.writeToLog("MDT_ Status being received. Status JsonObject: $data")
                   NTSPimPacket.MdtStatusObj(null,null,null,null,null,null,null,null).fromJson(data)
                }
            }
            "PIM_PAYMENT" -> {
                LoggerHelper.writeToLog("PIM Received PIM_Payment packet. Something is wrong")
            }

            "PAYMENT_DECLINED" -> {
                LoggerHelper.writeToLog("PIM Received Payment_declined packet. Something is wrong")
            }


            "PIM_STATUS"-> {
                LoggerHelper.writeToLog("PIM Received PIM_Status packet. Something is wrong")
            }

            "STATUS_REQ" -> {
                sendACK("STATUS_REQ")
                val dataObject = NTSPimPacket.PimStatusObj()
                val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
                LoggerHelper.writeToLog("status request being sent: ${statusObj.command}")
                MainActivity.mainActivity.sendBluetoothPacket(statusObj)
            }
        }
        return false
    }

    internal fun sendPaymentInfo(activity: Activity){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            val dataObject = NTSPimPacket.PimPaymentObj()
            val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_PAYMENT, dataObject)
            Log.i("Bluetooth", "status request packet to be sent == $statusObj")
            LoggerHelper.writeToLog("Sending Payment Info: ${dataObject}")
            (activity as MainActivity).sendBluetoothPacket(statusObj)
        }
    }

    internal fun sendDeclinedCardInfo(activity: Activity){
        val isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value ?: false
        if(isBluetoothOn){
            val dataObject = NTSPimPacket.PaymentDeclinedObj()
            val statusObj =  NTSPimPacket(NTSPimPacket.Command.PAYMENT_DECLINED, dataObject)
            Log.i("Bluetooth", "status request packet to be sent == $statusObj")
            LoggerHelper.writeToLog("Sending Declined Card Info: $statusObj")
            (activity as MainActivity).sendBluetoothPacket(statusObj)
        }
    }
    private fun sendACK(command: String) {
        val data = NTSPimPacket(NTSPimPacket.Command.ACK, null)
        MainActivity.mainActivity.sendACKPacket(data)
        LoggerHelper.writeToLog("Sending ACK for $command")
        Log.i("Bluetooth", "Sending ACK for $command")
    }

    internal fun sendNACK(){
        val data = NTSPimPacket(NTSPimPacket.Command.NACK, null)
        MainActivity.mainActivity.sendBluetoothPacket(data)
        LoggerHelper.writeToLog("Sending ACK")
    }
}