package com.example.nts_pim

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper

class BatteryPowerReceiver : BroadcastReceiver() {
    companion object{
        var temp: Float = 0.0F
        var overHeating = false
        var isCharging = false
    }

    // We will want to get the current heat level of the battery. If we get an overheat we will want to send an overheating timeStamp
    private val logTag = "Battery"
    private val thermalOverheatLevel = 127.0F
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(p0: Context?, p1: Intent?) {
       val mTemp =  p1?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)
        if(mTemp != null) {
            Log.i(logTag, "Battery temp raw number: $mTemp")
            val celsius = (mTemp/ 10).toFloat()
            val fahrenheit = (celsius / 5) * 9 + 32
            temp = fahrenheit
            batteryOverheatingCheck(temp)
            Log.i(logTag, "Battery temp C = $celsius. Battery temp F = $fahrenheit")
        }
        val status: Int = p1?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
         isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        // we could run logic here for if Pim is not Charging and overheating we could turn pim off...
    }

    private fun batteryOverheatingCheck(temp: Float){
        overHeating = if(temp > thermalOverheatLevel){
            PIMMutationHelper.sendPIMOverheatTime()
            true
        }else { false }
    }
}
