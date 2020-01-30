package com.example.nts_pim.utilities.logging_service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import com.example.nts_pim.utilities.phone_state_listener.MyPhoneStateListener
import java.lang.Exception

class LoggingService: Service(){
    val TAG = "LoggingService"
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        try {
            val filter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
            //registar Logging
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
//            telephonyManager.listen(
////                MyPhoneStateListener.getInstance(telephonyManager),
////                MyPhoneStateListener.LISTEN_EVENTS
//            )
        } catch (e: Exception) {
            Log.i(TAG, "WatchdogService.onCreate Error: $e")
        }
        super.onCreate()
    }
 }
