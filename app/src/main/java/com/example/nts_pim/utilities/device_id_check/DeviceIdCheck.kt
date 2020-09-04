package com.example.nts_pim.utilities.device_id_check

import android.annotation.SuppressLint
import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.model_objects.DeviceID
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import android.provider.Settings.Secure

object DeviceIdCheck {
    //This will handle the changing the imei to the Android id and be the location to get the device Id

   private var deviceIdObject = ModelPreferences(PimApplication.pimContext)
        .getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)


    @SuppressLint("HardwareIds")
    fun needToUpdateBackendForDeviceId():Boolean{
        //We are going to call this on start up and update the backend if we need to
        val androidId = Secure.getString(PimApplication.pimContext.contentResolver, Secure.ANDROID_ID)
        if(deviceIdObject != null && deviceIdObject!!.number != androidId){
            //The saved device id exists, but is not the android id. Most likely the device id is still the imei
            deviceIdObject!!.number = androidId
            ModelPreferences(PimApplication.pimContext).putObject(SharedPrefEnum.DEVICE_ID.key, deviceIdObject)
            return true
        }
        return false
    }

    fun getDeviceId(): String? {
        return ModelPreferences(PimApplication.pimContext)
            .getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)?.number
    }
}