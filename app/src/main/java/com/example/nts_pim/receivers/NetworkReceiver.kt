package com.example.nts_pim.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper


class NetworkReceiver:BroadcastReceiver() {
    val logFragment = "Network Receiver"
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (isOnline(context)) {
                VehicleTripArrayHolder.pimIsOnline()
                LoggerHelper.writeToLog("${logFragment}, TABLET IS ONLINE", LogEnums.INTERNET_CONNECTION.tag)
            } else {
                VehicleTripArrayHolder.pimIsOffline()
                LoggerHelper.writeToLog("${logFragment}, TABLET IS OFFLINE", LogEnums.INTERNET_CONNECTION.tag)
            }
        } catch (e: java.lang.NullPointerException) {
            e.printStackTrace()
            LoggerHelper.writeToLog("${logFragment}, TABLET IS OFFLINE ERROR: ${e.message}", LogEnums.INTERNET_CONNECTION.tag)
        }
    }
    @SuppressLint("MissingPermission")
    private fun isOnline(context: Context?): Boolean {
        return try {
            val cm =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            // val netInfo = cm.activeNetwork
            //should check null because in airplane mode it will be null
            netInfo != null && netInfo.isConnected
        } catch (e: NullPointerException) {
           LoggerHelper.writeToLog("$logFragment, error for network receiver. $e", LogEnums.INTERNET_CONNECTION.tag)
            false
        }
    }
}
