package com.example.nts_pim.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.SharedPrefEnum


// The vehicleTripArrayHolder object holds all the arrays for app-sync and LiveData
object VehicleTripArrayHolder {

    private var isSquareTransactionComplete = false
    private var isSquareTransactionCompleteMutableLiveData = MutableLiveData<Boolean>()

    private var isSquareTransactionStarted = false
    private var isSquareTransactionStartedMutableLiveData =  MutableLiveData<Boolean>()

    private var tripStatus = "no status assigned"
    private var tripStatusMutableLiveData = MutableLiveData<String>()


    private var meterOwed = 00.00
    private var meterOwedMutableLiveData = MutableLiveData<Double>()

    private var meterStatePIM = "off"
    private val meterStatePIMMutableLiveData = MutableLiveData<String>()

    private var batteryPowerIsSafe = false

    private var tripId = ""
    private var tripNumber = 0

    private var tabletNeedsReSync = false
    private var tabletNeedsReySyncMutableLiveData = MutableLiveData<Boolean>()
    //We use this for Pim Status Request
    private var internalPIMStatus = ""

    var squareHasBeenSetUp = false

    var internetIsConnected = false

    private var newTripHasStarted = false
    private var newTripHasStartedMutableLiveData = MutableLiveData<Boolean>()

    private var squareHasTimedOut = false
    private var squareHasTimedOutMutableLiveData = MutableLiveData<Boolean>()

    private var transactionID = ""


// Adds the status from the main activity app sync subscription. It goes to a live data array to be watched for changes. There is only 1 status in the array at all times.
//E.g. "Trip_On_Site", "Trip_Assigned", "Trip_Picked_UP"

    init {
        meterStatePIMMutableLiveData.value = meterStatePIM
        tabletNeedsReySyncMutableLiveData.value = tabletNeedsReSync
        newTripHasStartedMutableLiveData.value = newTripHasStarted
        squareHasBeenSetUp = false
    }

   fun addStatus(appsyncTripStatus: String){
        if(appsyncTripStatus != tripStatus){
            tripStatus = appsyncTripStatus
            tripStatusMutableLiveData.value = tripStatus
        }
   }
// Returns the trip status as Live Data
    fun getTripStatus() = tripStatusMutableLiveData

//Adds the meter state from the main activity app sync subscription. It goes to live data array to be watched for changes. There is only 1 status in the array at all times.
//E.g. "ON", "OFF"
    // need to add last part of where the meter change status is added for the first time.
    fun addMeterState(meterStateAWS: String) {
    if (meterStatePIM != meterStateAWS){
        meterStatePIM = meterStateAWS
        meterStatePIMMutableLiveData.value = meterStatePIM
    }
}
// Returns the meter status as Live Data
    fun getMeterState() = meterStatePIMMutableLiveData as LiveData<String>

// Adds the meter value from the main activity app sync subscription. Ibid previous liveData arrays
//E.g. Double - 00.01
   fun addMeterValue(double: Double){
            if(double != meterOwed){
                meterOwed = double
                meterOwedMutableLiveData.value = meterOwed
                Log.i("Results","METER VALUE has changed and $double has been added to LiveData")
            }
   }
// Returns the meter value as Live Data
    fun getMeterValue() = meterOwedMutableLiveData as LiveData<Double>
// Changes the square transaction from either the Main_activity or the Transaction complete screen.
// E.g. Boolean-True/False
    internal fun squareTransactionChange(): Boolean{
        isSquareTransactionComplete = !isSquareTransactionComplete
        isSquareTransactionCompleteMutableLiveData.value = isSquareTransactionComplete

        return isSquareTransactionComplete
    }
// Returns the square transaction status as a liveData Bool.
    fun getIsTransactionComplete() = isSquareTransactionCompleteMutableLiveData as LiveData<Boolean>

    internal fun squareTransactionHasStarted(){
        isSquareTransactionStarted = true
        isSquareTransactionStartedMutableLiveData.value = isSquareTransactionStarted
    }

    fun getIsTransactionStarted() = isSquareTransactionStartedMutableLiveData as LiveData<Boolean>


    fun addTripId(enteredTripId: String, context: Context){
        if(enteredTripId != tripId){
            tripId = enteredTripId
            newTripHasStarted = true
            newTripHasStartedMutableLiveData.value = newTripHasStarted
            createCurrentTrip(true, enteredTripId, context)
        }
    }

    fun getTripId() = tripId

    fun hasNewTripStarted() = newTripHasStartedMutableLiveData as LiveData<Boolean>

    fun addTripNumber(enteredNumber: Int){
        tripNumber = enteredNumber
    }

    fun getTripNumber() = tripNumber

    fun batteryPowerEnabledOrDisable(){
        batteryPowerIsSafe = !batteryPowerIsSafe
    }

    fun getBatteryPowerPermission() = batteryPowerIsSafe
// clears arrays.
    fun clearAllNonPersistentData(){
        meterOwed = 00.00
        meterOwedMutableLiveData.value = meterOwed
        isSquareTransactionComplete = false
        tripNumber = 0
        Log.i("Results", "All Arrays have been cleared")
    }

    fun updateInternalPIMStatus(pimStatus: String){
        internalPIMStatus = pimStatus
    }

    fun getInternalPIMStatus() = internalPIMStatus

    fun createCurrentTrip(isTripActive: Boolean, tripID: String, context: Context){
        val currentTrip = CurrentTrip(isTripActive, tripID)
        val oldTrip = ModelPreferences(context).getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
        if(currentTrip.tripID != oldTrip?.tripID){
            ModelPreferences(context)
                .putObject(SharedPrefEnum.CURRENT_TRIP.key, currentTrip)
        }
    }

    fun updateCurrentTrip(isTripActive: Boolean?, tripID: String, context: Context){
        val  oldCurrentTrip =  ModelPreferences(context).getObject(
            SharedPrefEnum.CURRENT_TRIP.key,
            CurrentTrip::class.java
        )
        if (isTripActive != null){
            val  newCurrentTrip = CurrentTrip(isTripActive, tripID)
            if (newCurrentTrip != oldCurrentTrip){
                ModelPreferences(context).putObject(SharedPrefEnum.CURRENT_TRIP.key, newCurrentTrip)
            }
        } else{
            if (oldCurrentTrip != null){
                val newCurrentTrip = CurrentTrip(oldCurrentTrip.isActive, tripID)
                if (newCurrentTrip != oldCurrentTrip){
                    ModelPreferences(context).putObject(SharedPrefEnum.CURRENT_TRIP.key, newCurrentTrip)
                }
            }
        }
    }

    fun reSyncTrip() {
        tabletNeedsReSync = true
        tabletNeedsReySyncMutableLiveData.value = tabletNeedsReSync

    }

    fun reSyncComplete(){
        tabletNeedsReSync = false
        tabletNeedsReySyncMutableLiveData.value = tabletNeedsReSync
    }

    fun getReSyncStatus() = tabletNeedsReySyncMutableLiveData as LiveData<Boolean>

    fun internetConnectionStatus() = internetIsConnected

    fun squareHasTimedOut(){
        squareHasTimedOut = true
        squareHasTimedOutMutableLiveData.value = squareHasTimedOut

        squareHasTimedOut = false
        squareHasTimedOutMutableLiveData.value = squareHasTimedOut
    }

    fun hasSquareTimedOut() = squareHasTimedOutMutableLiveData as LiveData<Boolean>

    fun setTransactionID(awsTransactionId: String){
        if(awsTransactionId != transactionID){
            transactionID = awsTransactionId
        }
    }
    fun getTransactionId() = transactionID
}

