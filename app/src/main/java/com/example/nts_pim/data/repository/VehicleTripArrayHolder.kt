package com.example.nts_pim.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.model_objects.trip.ReceiptPaymentInfo
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.enums.VehicleStatusEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper

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

    var numberOfReaderChecks = 0

    //Trip data
    private var tripStatus = "no status assigned"
    private var tripStatusMutableLiveData = MutableLiveData<String>()

    private var meterOwed = 00.00
    private var meterOwedMutableLiveData = MutableLiveData<Double>()

    private var meterStatePIM = "off"
    private val meterStatePIMMutableLiveData = MutableLiveData<String>()

    private var batteryPowerIsSafe = false

    private var tripId = ""
    private var tripIdForPayment = ""
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

    private var isPimPaired = true
    private var isPimPairedMLD = MutableLiveData<Boolean>()

    //We use this for Pim Status Request
    private var internalPIMStatus = ""

    private var sendOverHeatEmail = false
    private var sendOverheatEmailMLD = MutableLiveData<Boolean>()

    var pimStartTime:String? = null
    var pimOverHeat:String? = null

    private var paymentMethod = "none"

    private var transDate: String? = null
    private var cardInfo: String? = null
    private var declinedMessageForTrip: String? = null
    var receiptPaymentInfo: ReceiptPaymentInfo? = null

    private const val logFragment = "Vehicle_Trip_Array_Holder"

    private var allowUpfrontPrice: Boolean = true
    private var checkForDriverRejectionMLD = MutableLiveData<Boolean>()



    var flaggedTestVehicles: MutableList<String> = mutableListOf("ccsi_U_1496", "ccsi_Y_6801")



// Adds the status from the main activity app sync subscription. It goes to a live data array to be watched for changes. There is only 1 status in the array at all times.
//E.g. "Trip_On_Site", "Trip_Assigned", "Trip_Picked_UP"

    init {
        meterStatePIMMutableLiveData.value = meterStatePIM
        tabletNeedsReySyncMutableLiveData.value = tabletNeedsReSync
        newTripHasStartedMutableLiveData.value = newTripHasStarted
        squareHasBeenSetUp = false
        numberOfReaderChecks = 0
    }


   fun addStatus(status: String){
        if(status != this.tripStatus){
            val checkForBTValue = checkForBluetoothStatus(status)
            this.tripStatus = checkForBTValue
            tripStatusMutableLiveData.postValue(this.tripStatus)
            LoggerHelper.writeToLog("Internal pim status was changed. Trip Status: ${this.tripStatus}", LogEnums.TRIP_STATUS.tag)
        }
   }
// Returns the trip status as Live Data
    fun getTripStatus() = tripStatusMutableLiveData

    private fun checkForBluetoothStatus(status: String): String {
       if(status == VehicleStatusEnum.TRIP_CLOSED_BLUETOOTH.status){
           return VehicleStatusEnum.TRIP_CLOSED.status
       }
        if(status == VehicleStatusEnum.TRIP_END_BLUETOOTH.status){
            return VehicleStatusEnum.TRIP_END.status
        }
        return status
    }

//Adds the meter state from the main activity app sync subscription. It goes to live data array to be watched for changes. There is only 1 status in the array at all times.
//E.g. "ON", "OFF"
    // need to add last part of where the meter change status is added for the first time.
    fun addMeterState(meterStateAWS: String) {
    if (meterStatePIM != meterStateAWS){
        meterStatePIM = meterStateAWS
        meterStatePIMMutableLiveData.postValue(meterStateAWS)
        LoggerHelper.writeToLog("Internal pim meter was changed. Meter Status: $meterStatePIM", LogEnums.TRIP_STATUS.tag)
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
            }
   }
// Returns the meter value as Live Data
    fun getMeterValue() = meterOwedMutableLiveData as LiveData<Double>
// Changes the square transaction from either the Main_activity or the Transaction complete screen.
// E.g. Boolean-True/False
    internal fun squareTransactionChange(): Boolean {
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

    fun addTripId(enteredTripId: String, context: Context){
        if(enteredTripId != tripId && enteredTripId != ""){
            TripDetails.insertTripIdIntoCompleted(tripId)
            tripId = enteredTripId
            newTripHasStarted = true
            newTripHasStartedMutableLiveData.postValue(newTripHasStarted)
            createCurrentTrip(false, enteredTripId, "none", context)
            tripEnded = false
            tripEndedMutableLiveData.postValue(tripEnded)
            LoggerHelper.writeToLog("Internal Trip Id was changed. Trip Id: $tripId", LogEnums.TRIP_STATUS.tag)
        }
    }
    fun newTripWasPickedUp(){
        newTripHasStarted = false
        newTripHasStartedMutableLiveData.value = newTripHasStarted
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
        tripIdForPayment = ""
        paymentMethod = "none"
        cardInfo = ""
        transDate = ""
        declinedMessageForTrip = null
        LoggerHelper.writeToLog("All Trip Information has been cleared", LogEnums.TRIP_STATUS.tag)
    }

    fun updateInternalPIMStatus(pimStatus: String){
        if(pimStatus != internalPIMStatus){
            internalPIMStatus = pimStatus
            LoggerHelper.writeToLog("Pim status was updated internally to $internalPIMStatus", LogEnums.TRIP_STATUS.tag)
        }

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

    fun setPimPayment(receivedPamPayAmount: Double){
        val meterState = getMeterState().value
        pimPayAmount = receivedPamPayAmount
        pimPayAmountMutableLiveDate.postValue(pimPayAmount)
        LoggerHelper.writeToLog("pim pay amount has changed to $pimPayAmount internally. Meter State is $meterState", LogEnums.TRIP_STATUS.tag)
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

    fun setCardInfoPlusDate(info: String, date: String){
        if(info != cardInfo){
            cardInfo = info
        }
        if(date != transDate){
            transDate = date
        }
    }


    fun getCardInfo() = cardInfo
    fun getTransDate() = transDate

    fun updateDeclinedCardMessage(message: String){
        if(message != declinedMessageForTrip){
            declinedMessageForTrip = message
        }
    }
    fun getDeclinedCardMessage() = declinedMessageForTrip

    fun updateReaderStatus(status: String){
        cardReaderStatus = status
        LoggerHelper.writeToLog("Square, Card Reader Status updated: $cardReaderStatus", LogEnums.SQUARE.tag)
    }

    fun readerStatusHasBeenChecked(){
        cardReaderStatusHasBeenChecked = true
        cardReaderStatusHasBeenCheckedMLD.postValue(cardReaderStatusHasBeenChecked)
        LoggerHelper.writeToLog("Square, Card Reader Status has been checked: $cardReaderStatusHasBeenChecked", LogEnums.SQUARE.tag)
    }

    fun readerStatusNeedsToBeCheckedAgain(){
        cardReaderStatusHasBeenChecked = false
        cardReaderStatusHasBeenCheckedMLD.postValue(cardReaderStatusHasBeenChecked)
        LoggerHelper.writeToLog("Square, needs to be checked again: readerStatusHasBeenChecked: $cardReaderStatusHasBeenChecked", LogEnums.SQUARE.tag)
    }

    fun needToReAuthorizeSquare(){
        needToReAuthSquare = true
        needToReAuthSquareMLD.postValue(needToReAuthSquare)
        LoggerHelper.writeToLog("Square, Square needs needs to be to be reAuthorized", LogEnums.SQUARE.tag)
    }

    fun doWeNeedToReAuthorizeSquare() = needToReAuthSquareMLD as LiveData<Boolean>

    fun isReaderStatusConnected() = cardReaderStatusHasBeenCheckedMLD as LiveData<Boolean>

    fun pimPairingViaFMPChanged(change: Boolean){
        if(change != isPimPaired){
            isPimPaired = change
            isPimPairedMLD.value = isPimPaired
        }
    }

    fun isPimPaired() = isPimPairedMLD as LiveData<Boolean>

   internal fun setTripIdForPayment(){
        if(tripId != ""){
            tripIdForPayment = tripId
        }
    }

    internal fun getTripIdForPayment() = tripIdForPayment

    internal fun sendOverHeatEmail(){
        pimOverHeat = PIMMutationHelper.getCurrentDateFormattedDateUtcIso()
        sendOverHeatEmail = true
        sendOverheatEmailMLD.value = sendOverHeatEmail
        sendOverHeatEmail = false
        sendOverheatEmailMLD.value = sendOverHeatEmail
    }

    internal fun isPIMOverHeating() = sendOverheatEmailMLD

    internal fun setPIMStartTime(startTime: String){
        pimStartTime = startTime
    }

    internal fun insertPaymentMethod(mPaymentMethod: String){
        if(mPaymentMethod != paymentMethod){
            LoggerHelper.writeToLog("$logFragment, payment method was updated from $paymentMethod to $mPaymentMethod", LogEnums.TRIP_STATUS.tag)
            paymentMethod = mPaymentMethod
        }
    }

    internal fun getPaymentMethod(): String {
        return paymentMethod
    }

    internal fun allowUpfrontPrice(boolean: Boolean) {
        allowUpfrontPrice = boolean
        LoggerHelper.writeToLog("allow upfront price hit on vehicle Trip Array. Value: $boolean", LogEnums.BLUETOOTH.tag)
        if(allowUpfrontPrice && meterStatePIM == "off" || allowUpfrontPrice && meterStatePIM == ""){
           checkForDriverRejectionMLD.postValue(allowUpfrontPrice)
            LoggerHelper.writeToLog("allow upfront price hit on vehicle Trip Array. Value: $boolean", LogEnums.BLUETOOTH.tag)
        } else {
            LoggerHelper.writeToLog("allow upfront price: $allowUpfrontPrice. meterStatePim $meterStatePIM", LogEnums.BLUETOOTH.tag)
        }
        checkForDriverRejectionMLD.postValue(false)
    }

    fun checkForDriverRejection() = checkForDriverRejectionMLD

    @JvmName("setReceiptPaymentInfo1")
    fun setReceiptPaymentInfo(info: ReceiptPaymentInfo){

       receiptPaymentInfo = info
        LoggerHelper.writeToLog("set Receipt Payment hit.", LogEnums.RECEIPT.tag)
    }

    fun getReceiptPaymentInfo(tripId: String): ReceiptPaymentInfo? {
        if(receiptPaymentInfo != null && tripId == receiptPaymentInfo?.tripId){
            return receiptPaymentInfo as ReceiptPaymentInfo
        }
        return null
    }

    fun updateReceiptPaymentInfo(tripId: String, pimPayAmount: Double?, owedPrice: Double?, tipAmt: Double?, tipPercent: Double?, airPortFee: Double?, discountAmt: Double?, toll: Double?, discountPercent: Double?, destLat: Double?, destLon: Double?){
        val receiptPaymentInfo = getReceiptPaymentInfo(tripId)
        if(receiptPaymentInfo != null){
            if(pimPayAmount != null){
                receiptPaymentInfo.pimPayAmount = pimPayAmount
            }
            if(owedPrice != null){
                receiptPaymentInfo.owedPrice = owedPrice
            }
            if(tipAmt != null){
                receiptPaymentInfo.tipAmt = tipAmt
            }
            if(tipPercent != null){
                receiptPaymentInfo.tipPercent = tipPercent
            }
            if(airPortFee != null){
                receiptPaymentInfo.airPortFee = airPortFee
            }
            if(discountAmt != null){
                receiptPaymentInfo.discountAmt = discountAmt
            }
            if(toll != null){
                receiptPaymentInfo.toll = toll
            }
            if(discountPercent != null){
                receiptPaymentInfo.discountPercent = discountPercent
            }
            if(destLat != null && destLat != receiptPaymentInfo.destLat){
                receiptPaymentInfo.destLat = destLat
            }
            if(destLon != null && destLon != receiptPaymentInfo.destLon){
                receiptPaymentInfo.destLon = destLon
            }
        }
        if(receiptPaymentInfo == null && pimPayAmount
            != null && owedPrice != null
            && airPortFee != null && discountAmt != null
            && toll != null && discountPercent != null &&
            destLat != null && destLon != null){
            setReceiptPaymentInfo(ReceiptPaymentInfo(tripId, pimPayAmount,owedPrice,0.0,0.0,airPortFee, discountAmt, toll, discountPercent,destLat, destLon))
            }
        }
    }


