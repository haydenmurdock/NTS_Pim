package com.example.nts_pim.utilities.power_cycle

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nts_pim.R
import com.example.nts_pim.utilities.logging_service.LoggerHelper

class PowerAccessibilityService: AccessibilityService() {

    private var loggingEnabled = false
    private var volumeWarning = false
    private var bluetoothPairing = false
    private val clarenTablet = "com.claren.tablet_control.shutdown"
//    private var serviceLooper: Looper? = null
//    private var serviceHandler: ServiceHandler? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rtn = super.onStartCommand(intent, flags, startId)

        // https://stackoverflow.com/questions/8421430/reasons-that-the-passed-intent-would-be-null-in-onstartcommand
        // https://stackoverflow.com/questions/40818244/passing-data-from-accessibilityservice-to-activity
        if (intent != null && intent.action != null) {
            try {
                val action = intent.action
                if (!action.isNullOrBlank()) {
                    if (action == clarenTablet) {
                        Log.i("Power", "Ready to perform global action dialog")
                        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error with perform global action. Error: $e",Toast.LENGTH_LONG).show()
            }
            return START_STICKY
        } else {
            Log.i("Power", "intent or action intent was null")
            return rtn
        }
    }
    override fun onInterrupt() {
        Log.i("LOGGER", "Power service interruped.")
    }
    override fun onServiceConnected() {
        Log.i("Power", "The power accessiblity service has been connected")
        super.onServiceConnected()

    }
    override fun onCreate() {
        val pi = PendingIntent.getActivity(this, 0, Intent(), 0)
        val notification = NotificationCompat.Builder(this, "powerservicechannel")
            .setContentTitle("NTS PIM")
            .setContentText("Power service is running")
            .setSmallIcon(R.drawable.ic_wrench)
            .setContentIntent(pi)
            .build()
        try {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED){
                startForeground(1, notification)
                Log.i("LOGGER", "FOREGROUND SERVICE STARTED")
                LoggerHelper.writeToLog("FOREGROUND SERVICE STARED")
            }
        } catch (e: Exception){
            Log.i("LOGGER", "FOREGROUND SERVICE NOT STARTED. EXCEPTION:$e")
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try{
            volumeWarning = false
            bluetoothPairing = false

            if (loggingEnabled) {
                try {

                } catch (e: Exception) {

                }

            }
            logViewHierarchy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun logViewHierarchy() {
        try {
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }
    internal fun isAccessibilitySettingsOn(mContext: Context): Boolean {
        val TAG = "isAcc"
        var accessibilityEnabled = 0
        val service =
            mContext.getPackageName() + "/" + PowerAccessibilityService::class.java.getCanonicalName()
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                mContext.applicationContext.contentResolver,Settings.Secure.ACCESSIBILITY_ENABLED)
            Log.v(TAG, "accessibilityEnabled = $accessibilityEnabled")
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.message)
        }
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                mContext.getApplicationContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val lowerProviders = settingValue.toLowerCase()
            if (lowerProviders.contains("poweraccessibilityservice")) {
                Log.i(TAG, "powerAccessibilityService Provider is installed ************")
                return true
            } else {
                Log.v(TAG, "***ACCESSIBILITY IS DISABLED***")
                return false
            }
        }
        return false
    }
}