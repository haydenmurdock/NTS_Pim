package com.example.nts_pim.utilities.samsung_knox

import android.content.Context
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.samsung.android.knox.custom.CustomDeviceManager
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager


/**
 * Contains Samsung Knox features to test and ultimately use in PIM.
 */
object SamsungKnoxHelper {


    fun activateKnoxLicense(context: Context): Boolean {
        val klmManager = KnoxEnterpriseLicenseManager.getInstance(context)
        val knoxKey = "KLM09-HODYX-0GJYO-Q8HN0-TAW41-54YQY"
        try {

            LoggerHelper.writeToLog(
                "Activating Knox enterprise with $knoxKey",
                LogEnums.SAMSUNG_KNOX.tag
            )
            klmManager.activateLicense(knoxKey)
        } catch (e: Exception) {
            LoggerHelper.writeToLog(
                "Error activating Knox enterprise with key. Key: $knoxKey. Error: $e",
                LogEnums.SAMSUNG_KNOX.tag
            )
            return false
        }
        return true
    }

    fun turnOffToasts() {
        try {
            val cdm = CustomDeviceManager.getInstance()
            val kcsm = cdm.systemManager
            kcsm.toastEnabledState = false
        } catch (e: SecurityException) {
            LoggerHelper.writeToLog(
                "Security error turning off toasts. Error: $e",
                LogEnums.SAMSUNG_KNOX.tag
            )
        }

    }

    fun turnOnToasts() {
        try {
            val cdm = CustomDeviceManager.getInstance()
            val kcsm = cdm.systemManager
            kcsm.toastEnabledState = true
        } catch (e: SecurityException) {
            LoggerHelper.writeToLog(
                "Security error turning on toasts. Error: $e",
                LogEnums.SAMSUNG_KNOX.tag
            )
        }
    }

    fun turnOffTabletViaKnox(){
        try {
            val cdm = CustomDeviceManager.getInstance()
            val kcsm = cdm.systemManager
            kcsm.powerOff()
        } catch (e: SecurityException) {
            LoggerHelper.writeToLog(
                "Turning off power via knox error. Security exception: $e",
                LogEnums.SAMSUNG_KNOX.tag
            )
        }
    }

}