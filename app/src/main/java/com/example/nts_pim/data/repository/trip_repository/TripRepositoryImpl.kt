package com.example.nts_pim.data.repository.trip_repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.data.repository.model_objects.*
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.data.repository.providers.PreferenceProvider
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum


class TripRepositoryImpl(
    context: Context
) : PreferenceProvider(context),
    TripRepository {

    private var doesVehicleIDExist = false

    private var doesCompanyNameExist = false
    private var doesCompanyNameExistLiveData = MutableLiveData<Boolean>()

    private var isThereAuthCode = false
    private var isThereAuthCodeLiveData = MutableLiveData<Boolean>()

    private var squareIsAuthorized = false
    private var squareIsAuthorizedLiveData = MutableLiveData<Boolean>()

    private var pinWasEnteredWrong = false
    private var pinWasEnteredWrongLiveData = MutableLiveData<Boolean>()

    private var isSetupCompleteLiveData = MutableLiveData<Boolean>()

    private val appContext = context.applicationContext

    init {
        doesCompanyNameExistLiveData.value = doesCompanyNameExist
        isThereAuthCodeLiveData.value = isThereAuthCode
        squareIsAuthorizedLiveData.value = squareIsAuthorized
        pinWasEnteredWrongLiveData.value = pinWasEnteredWrong
    }

    override suspend fun checkForVehicleID(): Boolean {
        val vehicleID = ModelPreferences(appContext.applicationContext)
            .getObject(SharedPrefEnum.VEHICLE_ID.key,
                VehicleID::class.java)

        Log.i(LogEnums.PIM_SETTING.tag, "Vehicle ID is ${vehicleID?.vehicleID}")
        return (vehicleID?.vehicleID != null && vehicleID.vehicleID != "")
    }


    override fun getVehicleSettings(): VehicleSettings? {
        val vehicleSettings = ModelPreferences(appContext.applicationContext)
            .getObject(SharedPrefEnum.VEHICLE_SETTINGS.key,
            VehicleSettings::class.java)
        if(vehicleSettings != null)
        return vehicleSettings
        return null
    }

    override suspend fun getVehiclePIN(): String {
        val pin = ModelPreferences(appContext.applicationContext)
            .getObject(SharedPrefEnum.PIN_PASSWORD.key, PIN::class.java)
        if (pin != null) {
            return pin.password
        }
        return ""
    }

    override suspend fun checkForPIN(): Boolean {
        val pin = ModelPreferences(appContext.applicationContext)
            .getObject(SharedPrefEnum.PIN_PASSWORD.key, PIN::class.java)
        if (pin != null) {
            return true
        }
        return false
    }

    override suspend fun getDeviceID(): String {
        val deviceID = ModelPreferences(appContext.applicationContext)
            .getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)
        if(deviceID != null){
            return deviceID.number
        }
        return ""
    }

    override fun getVehicleID(): String {
        val vehicleID =  ModelPreferences(appContext.applicationContext)
            .getObject(SharedPrefEnum.VEHICLE_ID.key, VehicleID::class.java)
            if(vehicleID != null){
                if (vehicleID.vehicleID != ""){
                    doesVehicleIDExist = true
                    return vehicleID.vehicleID
                }
            }
        return ""
    }
    override fun doesVehicleIDExist() = doesVehicleIDExist

    override fun vehicleIDSaved() {
        doesVehicleIDExist = true
    }

    override fun vehicleIdDoesNotExist() {
        doesVehicleIDExist = false
    }


    override fun isThereAuthCode() = isThereAuthCodeLiveData as LiveData<Boolean>

    override fun authCodeSuccess() {
        isThereAuthCode = true
        isThereAuthCodeLiveData.value = isThereAuthCode
    }

    override fun recheckAuthCode(){
        isThereAuthCode = false
        isThereAuthCodeLiveData.value = isThereAuthCode
    }


    override fun isSquareAuthorized() = squareIsAuthorizedLiveData as LiveData<Boolean>

    override fun squareIsAuthorized() {
        squareIsAuthorized = true
        squareIsAuthorizedLiveData.value = squareIsAuthorized
    }

    override fun squareIsNotAuthorized(){
        squareIsAuthorized = false
        squareIsAuthorizedLiveData.value = squareIsAuthorized
    }


    override fun doesCompanyNameExist() = doesCompanyNameExistLiveData as LiveData<Boolean>

    override fun companyNameExists() {
        doesCompanyNameExist = true
        doesCompanyNameExistLiveData.value = doesCompanyNameExist
    }

    override fun companyNameNoLongerExists(){
        doesCompanyNameExist = false
        doesCompanyNameExistLiveData.value = doesCompanyNameExist
    }

    override fun isPinEnteredWrong() = pinWasEnteredWrongLiveData as LiveData<Boolean>

    override fun pinWasEnteredWrong() {
        pinWasEnteredWrong = !pinWasEnteredWrong
        pinWasEnteredWrongLiveData.value = pinWasEnteredWrong
    }

    override fun isSetupComplete(): Boolean {
       val statusObject = ModelPreferences(appContext).getObject(SharedPrefEnum.SETUP_COMPLETE.key, SetupComplete::class.java)
        if (statusObject != null){
            isSetupCompleteLiveData.value = statusObject.status
            return statusObject.status
        } else {
            return false
        }
    }

    override fun watchSetUpComplete() = isSetupCompleteLiveData as LiveData<Boolean>
}

