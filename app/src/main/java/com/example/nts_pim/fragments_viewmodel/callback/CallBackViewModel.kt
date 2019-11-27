package com.example.nts_pim.fragments_viewmodel.callback

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.nts_pim.data.repository.VehicleTripArrayHolder

class CallBackViewModel(
    private var vehicleTripArrayHolder: VehicleTripArrayHolder
): ViewModel() {

    internal fun getTripStatus() = vehicleTripArrayHolder.getTripStatus()


    internal fun addTripStatus(string: String){
            vehicleTripArrayHolder.addStatus(string)
    }

    internal fun getMeterState() = vehicleTripArrayHolder.getMeterState()

    internal fun addMeterState(string: String){
        vehicleTripArrayHolder.addMeterState(string)
    }

    internal fun getMeterOwed() = vehicleTripArrayHolder.getMeterValue()

    internal fun addMeterValue(double: Double){
        vehicleTripArrayHolder.addMeterValue(double)
    }

    internal fun squareChangeTransaction(){
        vehicleTripArrayHolder.squareTransactionChange()
    }

    internal fun getIsTransactionComplete() = vehicleTripArrayHolder.getIsTransactionComplete()

    internal fun getIsTransactionStarted() = vehicleTripArrayHolder.getIsTransactionStarted()

    internal fun addTripId(tripId: String, context: Context){
        vehicleTripArrayHolder.addTripId(tripId, context)
    }

    internal fun getTripId() = vehicleTripArrayHolder.getTripId()

    internal fun hasNewTripStarted() = vehicleTripArrayHolder.hasNewTripStarted()

    internal fun addTripNumber(enteredNumber: Int){
        vehicleTripArrayHolder.addTripNumber(enteredNumber)
    }

    internal fun getTripNumber() = vehicleTripArrayHolder.getTripNumber()

    internal fun clearAllTripValues() = vehicleTripArrayHolder.clearAllNonPersistentData()

    internal fun enableOrDisableBatteryPower() = vehicleTripArrayHolder.batteryPowerEnabledOrDisable()

    internal fun batteryPowerStatePermission() = vehicleTripArrayHolder.getBatteryPowerPermission()

    internal fun createCurrentTrip(isTripActive: Boolean, tripId: String, context: Context){
        vehicleTripArrayHolder.createCurrentTrip(isTripActive,tripId,context)
    }

    internal fun updateCurrentTrip(isTripActive: Boolean?, tripId: String, context: Context){
        vehicleTripArrayHolder.updateCurrentTrip(isTripActive,tripId,context)
    }

    internal fun getReSyncStatus() = vehicleTripArrayHolder.getReSyncStatus()

    internal fun reSyncComplete() = vehicleTripArrayHolder.reSyncComplete()

    internal fun reSyncTrip() = vehicleTripArrayHolder.reSyncTrip()

    internal fun isConnectedToInternet() = vehicleTripArrayHolder.internetConnectionStatus()

    internal fun hasSquareTimedOut() = vehicleTripArrayHolder.hasSquareTimedOut()

    internal fun squareTimedOut() = vehicleTripArrayHolder.squareHasTimedOut()

    internal fun setTransactionId(string: String){
        vehicleTripArrayHolder.setTransactionID(string)
    }

    internal fun getTransactionId() = vehicleTripArrayHolder.getTransactionId()
}