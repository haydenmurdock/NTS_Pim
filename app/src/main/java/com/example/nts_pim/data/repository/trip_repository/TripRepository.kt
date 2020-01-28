package com.example.nts_pim.data.repository.trip_repository

import androidx.lifecycle.LiveData
import com.example.nts_pim.data.repository.model_objects.VehicleSettings



interface TripRepository {

    suspend fun checkForVehicleID():Boolean

    fun getVehicleSettings(): VehicleSettings?

    suspend fun getVehiclePIN():String

    suspend fun checkForPIN():Boolean

    suspend fun getDeviceID(): String

    fun getVehicleID(): String

    fun doesVehicleIDExist(): Boolean

    fun vehicleIDSaved()

    fun vehicleIdDoesNotExist()

    fun isThereAuthCode(): LiveData<Boolean>

    fun recheckAuthCode()

    fun authCodeSuccess()

    fun isSquareAuthorized(): LiveData<Boolean>

    fun squareIsAuthorized()

    fun squareIsNotAuthorized()

    fun doesCompanyNameExist(): LiveData<Boolean>

    fun companyNameNoLongerExists()

    fun companyNameExists()

    fun isPinEnteredWrong(): LiveData<Boolean>

    fun pinWasEnteredWrong()

    fun isSetupComplete(): Boolean

    fun watchSetUpComplete(): LiveData<Boolean>
}