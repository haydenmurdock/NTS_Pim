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
    //Square data options
    private var isSquareTransactionComplete = false
    private var isSquareTransactionCompleteMutableLiveData = MutableLiveData<Boolean>()

    private var isSquareTransactionStarted = false
    private var isSquareTransactionStartedMutableLiveData =  MutableLiveData<Boolean>()
    //This is and card reader are not private since we use them for the square service
    var squareHasBeenSetUp = false

    var cardReaderStatusHasBeenChecked = false
    private var cardReaderStatusHasBeenCheckedMLD = MutableLiveData<Boolean>()

    private var squareHasTimedOut = false
    private var squareHasTimedOutMutableLiveData = MutableLiveData<Boolean>()

    private var amountForSquareDisplay = "0"

    private var needToReAuthSquare = false
    private var needToReAuthSquareMLD = MutableLiveData<Boolean>()

    var cardReaderStatus = "default"

    //Trip data
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

    private var newTripHasStarted = false
    private var newTripHasStartedMutableLiveData = MutableLiveData<Boolean>()

    private var isOnline = false
    private var isOnlineMutableLiveData = MutableLiveData<Boolean>()

    private var transactionID = ""

    private var tripTipAmount:Double = 0.0

    private var pimPayAmount:Double = 0.0
    private var pimPayAmountMutableLiveDate = MutableLiveData<Double>()

    private var pimPaidAmount:Double = 0.0
    private var pimNoReceipt = false

    private var tripEnded = false
    private var tripEndedMutableLiveData = MutableLiveData<Boolean>()

    var paymentTypeSelected = "none"

    private var driverId = 0

    private var deviceIsBondedBT = false
    private var deviceIsBondedBTMutableLiveData = MutableLiveData<Boolean>()

    //We use this for Pim Status Request
    private var internalPIMStatus = ""

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
        Log.i("Results","METER STATE has changed from $meterStatePIM to $meterStateAWS")
        meterStatePIM = meterStateAWS
        meterStatePIMMutableLiveData.value = meterStatePIM

    }
}
// Returns the meter status as Live Data
    fun getMeterState() = meterStatePIMMutableLiveData as LiveData<String>

// Adds the meter value from the main activity app sync subscription. Ibid previous liveData arrays
//E.g. Double - 00.01
   fun addMeterValue(enteredMeterValue: Double){
            if(enteredMeterValue != meterOwed){
                meterOwed = enteredMeterValue
                meterOwedMutableLiveData.value = meterOwed
                Log.i("Results","METER VALUE has changed and $enteredMeterValue has been added to LiveData")
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
        if(enteredTripId != tripId &&
                enteredTripId != ""){
            tripId = enteredTripId
            newTripHasStarted = true
            newTripHasStartedMutableLiveData.value = newTripHasStarted
            createCurrentTrip(false, enteredTripId, "none", context)
            tripEnded = false
            tripEndedMutableLiveData.value = tripEnded
            newTripHasStarted = false
            newTripHasStartedMutableLiveData.value = newTripHasStarted
        }
    }

    fun getTripId() = tripId

    fun hasNewTripStarted() = newTripHasStartedMutableLiveData as LiveData<Boolean>

    fun addTripNumber(enteredNumber: Int){
        tripNumber = enteredNumber
    }

    fun tripHasEnded(){
        tripEnded = true
        tripEndedMutableLiveData.value = tripEnded
    }

    fun getTripHasEnded() = tripEndedMutableLiveData as LiveData<Boolean>

    fun getTripNumber() = tripNumber

    fun batteryPowerEnabledOrDisable(){
        batteryPowerIsSafe = !batteryPowerIsSafe
    }

    fun getBatteryPowerPermission() = batteryPowerIsSafe
    // clears arrays.
    fun clearAllNonPersistentData(){
        meterOwed = 00.00
        tripTipAmount = 0.0
        pimPayAmount = 0.0
        pimPaidAmount = 0.0
        pimNoReceipt = false
        meterOwedMutableLiveData.value = meterOwed
        isSquareTransactionComplete = false
        tripNumber = 0
        transactionID = ""
        meterStatePIM = "off"
        meterStatePIMMutableLiveData.value = meterStatePIM
        paymentTypeSelected = "none"
        driverId = 0
        Log.i("Results", "All Trip Information has been cleared")
    }

    fun updateInternalPIMStatus(pimStatus: String){
        internalPIMStatus = pimStatus
    }

    fun getInternalPIMStatus() = internalPIMStatus

    fun createCurrentTrip(isTripActive: Boolean, tripID: String, lastMeterState: String, context: Context){
        val currentTrip = CurrentTrip(isTripActive, tripID, lastMeterState)
        val oldTrip = ModelPreferences(context).getObject(SharedPrefEnum.CURRENT_TRIP.key, CurrentTrip::class.java)
        if (oldTrip == null){
            // This is the first trip so it is current
            ModelPreferences(context)
                .putObject(SharedPrefEnum.CURRENT_TRIP.key, currentTrip)
        }
        if(currentTrip.tripID != oldTrip?.tripID && currentTrip.tripID != ""){
            // this is a new trip and needs to update Trip id
            Log.i("currentTrip", "Current Trip Created: ${currentTrip.tripID}")
            ModelPreferences(context)
                .putObject(SharedPrefEnum.CURRENT_TRIP.key, currentTrip)
        }
    }

    fun updateCurrentTrip(isTripActive: Boolean?, tripID: String, lastMeterState: String, context: Context){
        val  oldCurrentTrip =  ModelPreferences(context).getObject(
            SharedPrefEnum.CURRENT_TRIP.key,
            CurrentTrip::class.java
        )
        if (isTripActive != null){
            val  newCurrentTrip = CurrentTrip(isTripActive, tripID, lastMeterState)
            if (newCurrentTrip != oldCurrentTrip){
                ModelPreferences(context).putObject(SharedPrefEnum.CURRENT_TRIP.key, newCurrentTrip)
            }
        } else{
            if (oldCurrentTrip != null){
                val newCurrentTrip = CurrentTrip(oldCurrentTrip.isActive, tripID, lastMeterState)
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
    fun getTransactionID() = transactionID

    fun setTipAmount(tip: Double){
        tripTipAmount = tip
    }
    fun getTipAmount() = tripTipAmount

    fun setPimPayment(awsPimPayAmount: Double){
        pimPayAmount = awsPimPayAmount
        pimPayAmountMutableLiveDate.value = pimPayAmount
        Log.i("pim pay amount", "pim pay amount has changed to $pimPayAmount")
    }

    fun getPimPayAmount() = pimPayAmount

    fun getPimPaymentSubscriptionAmount() = pimPayAmountMutableLiveDate as LiveData<Double>

    fun setPimPaidAmount(awsPimPaidAmount: Double){
        pimPaidAmount = awsPimPaidAmount
    }

    fun getPimPaidAmount() = pimPaidAmount

    fun pimDoesNotNeedToDoReceipt(boolean: Boolean){
        pimNoReceipt = boolean
    }
    fun getPimNoReceipt() = pimNoReceipt

    fun setAmountForSquareDisplay(double: Double){
        amountForSquareDisplay = double.toString()
    }

    fun getAmountForSquareDisplay()  = amountForSquareDisplay

    fun pimIsOnline(){
        isOnline = true
        isOnlineMutableLiveData.value = isOnline
    }

    fun pimIsOffline(){
        isOnline = false
        isOnlineMutableLiveData.value = isOnline
    }

    fun isPimOnline() = isOnlineMutableLiveData as LiveData<Boolean>

    fun setDriverId(enteredDriverId: Int){
        if(enteredDriverId != driverId){
            driverId = enteredDriverId
        }
    }

    fun getDriverId() = driverId

    fun deviceIsBondedViaBluetooth(){
        deviceIsBondedBT = true
        deviceIsBondedBTMutableLiveData.value = deviceIsBondedBT
    }

    fun deviceIsNotBondedViaBluetooth(){
        deviceIsBondedBT = false
        deviceIsBondedBTMutableLiveData.value = deviceIsBondedBT
    }

    fun isDeviceBondedViaBluetooth(): LiveData<Boolean>{
        return deviceIsBondedBTMutableLiveData
    }

    fun updateReaderStatus(status: String){
        cardReaderStatus = status
        Log.i("Square", "Internal data: Reader Status updated: $cardReaderStatus")
    }

    fun readerStatusHasBeenChecked(){
        cardReaderStatusHasBeenChecked = true
        cardReaderStatusHasBeenCheckedMLD.postValue(cardReaderStatusHasBeenChecked)
        Log.i("Square", "Internal data: Reader Status has been checked: $cardReaderStatusHasBeenChecked")
    }

    fun needToReAuthorizeSquare(){
        needToReAuthSquare = true
        needToReAuthSquareMLD.postValue(needToReAuthSquare)
        Log.i("Square", "Internal data: needToReAuthSquare: $needToReAuthSquare")
    }

    fun doWeNeedToReAuthorizeSquare() = needToReAuthSquareMLD as LiveData<Boolean>

    fun isReaderStatusConnected() = cardReaderStatusHasBeenCheckedMLD as LiveData<Boolean>
}

