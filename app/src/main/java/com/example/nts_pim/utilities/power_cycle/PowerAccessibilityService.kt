package com.example.nts_pim.utilities.power_cycle

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PowerAccessibilityService: AccessibilityService() {

    private var loggingEnabled = false
    private val receiver: BroadcastReceiver? = null
    private var samsungVolumneWarning = false
    private var bluetoothPairing = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rtn = super.onStartCommand(intent, flags, startId)

        // https://stackoverflow.com/questions/8421430/reasons-that-the-passed-intent-would-be-null-in-onstartcommand
        // https://stackoverflow.com/questions/40818244/passing-data-from-accessibilityservice-to-activity
        if (intent != null && intent.action != null) {
            try {
                val action = intent.action
                if (!action.isNullOrBlank()) {
                    if (action == "com.claren.tablet_control.shutdown") {
                        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                    }
                }
            } catch (e: Exception) {
            }
            return START_STICKY
        } else {
            return rtn
        }
    }
    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try{
            samsungVolumneWarning = false
            bluetoothPairing = false

            if (loggingEnabled) {
                try {

                } catch (e: Exception) {

                }

            }
            logViewHierarchy(rootInActiveWindow, 2)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun logViewHierarchy(nodeInfo: AccessibilityNodeInfo, depth: Int) {
        try {
            if (nodeInfo == null) return

            if (nodeInfo.className != null) {

                //install APK - SAMSUNG
                if (nodeInfo.viewIdResourceName != null && nodeInfo.text != null) {
                    if (nodeInfo.viewIdResourceName.contains("ok_button") && nodeInfo.text.toString().contains(
                            "INSTALL"
                        ) && nodeInfo.packageName.toString().contains("com.google.android.packageinstaller")
                    ) {
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }

                // "Allow Permission from this source" - bypassing nougat security
                // @todo This is too permissive...will press any dialog with "button1" of "ALLOW" .. hits both install and pairing
                /* Disable for ClarenTabletControl
                if (nodeInfo.getViewIdResourceName() != null && nodeInfo.getText() != null) {
                    if (nodeInfo.getViewIdResourceName().contains("button1") && nodeInfo.getText().toString().contains("ALLOW"))
                    {
                        Log.i(TAG,"**** Press ALLOW");
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
                 */

                //click on "Done" after installation
                /* Disable for ClarenTabletControl
                if (nodeInfo.getViewIdResourceName() != null && nodeInfo.getText() != null) {
                    if (nodeInfo.getViewIdResourceName().contains("done_button") && nodeInfo.getText().toString().contains("DONE") && nodeInfo.getPackageName().toString().contains("com.google.android.packageinstaller"))
                    {
                        Log.i(TAG,"**** Press DONE");
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
                 */

                // Samsung power off
                if (nodeInfo.viewIdResourceName != null && nodeInfo.contentDescription != null) {

                    if (nodeInfo.className.toString().contains("ImageView") && nodeInfo.viewIdResourceName.contains(
                            "sec_global_actions_icon"
                        ) && nodeInfo.contentDescription.toString().contains("Power")
                    ) {


                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS) // needed for Android 7 on samsung
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK) // 2 clicks for android 8
                    }


                    if (nodeInfo.className.toString().contains("FrameLayout") && nodeInfo.viewIdResourceName.contains(
                            "global_actions_item_image_layout"
                        ) && nodeInfo.contentDescription.toString().contains("Power")
                    ) {


                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS) // needed for Android 7 on samsung
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK) // 2 clicks for android 8
                    }

                    if (nodeInfo.className.toString().contains("TextView") && nodeInfo.text.toString().contains(
                            "Power off"
                        )
                    ) {


                        nodeInfo.parent.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }

                }

                // Alcatel power off
                if (nodeInfo.text != null) {

                    if (nodeInfo.className.toString().contains("TextView") && nodeInfo.text.toString().contains(
                            "Power off"
                        )
                    ) {

                        nodeInfo.parent.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }

                }



                if (loggingEnabled) {

                    /*if (nodeInfo.getClassName().toString().contains("Button") && nodeInfo.getViewIdResourceName().contains("button1") && nodeInfo.getText().toString().contains("OK")) {
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }*/

                    var spacerString = ""

                    for (i in 0 until depth) {
                        spacerString += '-'.toString()
                    }
                    //Log the info you care about here... I picked a classname and view resource name, because they are simple, but interesting.


                    if (nodeInfo.className.toString().contains("TextView")) {

                        val cs = nodeInfo.text

                    }
                }

                for (i in 0 until nodeInfo.childCount) {
                    logViewHierarchy(nodeInfo.getChild(i), depth + 1)
                }

            }
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
                mContext.applicationContext.contentResolver,
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED)
            Log.v(TAG, "accessibilityEnabled = $accessibilityEnabled")
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.message)
        }

        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')

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