package com.example.nts_pim.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.CountDownTimer
import android.util.Log
import com.example.nts_pim.PimApplication
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper

class BatteryPowerReceiver : BroadcastReceiver() {
    companion object {
        var temp: Float = 0.0F
        var overHeating = false
        var isCharging = false
        var overheatingProtocolInitiated = false
    }
    // We will want to get the current heat level of the battery. If we get an overheat we will want to send an overheating timeStamp
    private val logTag = "Battery"
    private val thermalOverheatLevel = 126.0F
    private val thermalLevelNormal = 124.0F
    private var overHeatMessageTimer: CountDownTimer? = null

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(p0: Context?, p1: Intent?) {
        val mTemp = p1?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        if (mTemp != null) {
            Log.i(logTag, "Battery temp raw number: $mTemp")
            val celsius = (mTemp / 10).toFloat()
            val fahrenheit = (celsius / 5) * 9 + 32
            temp = fahrenheit
            batteryOverheatingCheck(temp, p0)
            Log.i(logTag, "Battery temp C = $celsius. Battery temp F = $fahrenheit")
        }
    }

    private fun batteryOverheatingCheck(temp: Float, context: Context?){
        overHeating = if(temp > thermalOverheatLevel){
            val overheatTimestamp = PIMMutationHelper.getCurrentDateFormattedDateUtcIso()
            if(overheatTimestamp != null && !overheatingProtocolInitiated){
                overheatingProtocolInitiated = true
                PIMMutationHelper.sendPIMOverheatTime(overheatTimestamp)
                VehicleTripArrayHolder.sendOverHeatEmail()
                if(context != null && overHeatMessageTimer == null){
                    startOverHeatTimer(PimApplication.pimContext)
                }
        }
            true
        }else if(temp < thermalLevelNormal && overheatingProtocolInitiated){
            LoggerHelper.writeToLog("temp $temp is lower then 122 so we are resetting overheating protocol")
            overheatingProtocolInitiated = false
            false
        } else {
            false
        }
    }
    private fun startOverHeatTimer(context: Context){
        if(overHeatMessageTimer == null){
            overHeatMessageTimer = object :CountDownTimer(30000, 10000){
                override fun onTick(millisUntilFinished: Long) {
                    if(!overheatingProtocolInitiated){
                        stopOverHeatTimer()
                    }
                }

                override fun onFinish() {
                    playSafetyMessage(context)
                }
            }.start()
        }
    }
    private fun stopOverHeatTimer(){
        overHeatMessageTimer?.cancel()
        overHeatMessageTimer = null
    }

    private fun playSafetyMessage(context: Context){
        val mediaPlayer = MediaPlayer.create(context, R.raw.overheating_message)
        mediaPlayer.setOnCompletionListener { mP ->
            LoggerHelper.writeToLog("${logTag}: Finished overheatWarning")
            mP.release()
            if(overheatingProtocolInitiated){
                overHeatMessageTimer?.cancel()
                overHeatMessageTimer = null
                startOverHeatTimer(context)
            }
        }
        mediaPlayer.start()
        LoggerHelper.writeToLog("${logTag}t: Started Overheat warning")
    }
}
