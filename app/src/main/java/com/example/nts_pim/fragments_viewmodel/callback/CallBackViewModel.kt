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



    internal fun addTripId(tripId: String, context: Context){
        vehicleTripArrayHolder.addTripId(tripId, context)
    }

    internal fun getTripId() = vehicleTripArrayHolder.getTripId()

    internal fun hasNewTripStarted() = vehicleTripArrayHolder.hasNewTripStarted()

    internal fun tripWasPickedUp() = vehicleTripArrayHolder.newTripWasPickedUp()

    internal fun addTripNumber(enteredNumber: Int){
        vehicleTripArrayHolder.addTripNumber(enteredNumber)
    }

    internal fun getTripNumber() = vehicleTripArrayHolder.getTripNumber()

    internal fun clearAllTripValues() = vehicleTripArrayHolder.clearAllNonPersistentData()

    internal fun enableOrDisableBatteryPower() = vehicleTripArrayHolder.batteryPowerEnabledOrDisable()

    internal fun batteryPowerStatePermission() = vehicleTripArrayHolder.getBatteryPowerPermission()

    internal fun createCurrentTrip(isTripActive: Boolean, tripId: String, lastMeterState: String, context: Context){
        vehicleTripArrayHolder.createCurrentTrip(isTripActive,tripId,lastMeterState,context)
    }

    internal fun updateCurrentTrip(isTripActive: Boolean?, tripId: String,lastMeterState: String, context: Context){
        vehicleTripArrayHolder.updateCurrentTrip(isTripActive,tripId,lastMeterState,context)
    }

    internal fun getReSyncStatus() = vehicleTripArrayHolder.getReSyncStatus()

    internal fun reSyncComplete() = vehicleTripArrayHolder.reSyncComplete()

    internal fun reSyncTrip() = vehicleTripArrayHolder.reSyncTrip()


    internal fun hasSquareTimedOut() = vehicleTripArrayHolder.hasSquareTimedOut()

    internal fun squareTimedOut() = vehicleTripArrayHolder.squareHasTimedOut()

    internal fun setTransactionId(string: String){
        vehicleTripArrayHolder.setTransactionID(string)
    }
    internal fun getTransactionId() = VehicleTripArrayHolder.getTransactionID()

    internal fun setTipAmount(enteredTip: Double){
        vehicleTripArrayHolder.setTipAmount(enteredTip)
    }
    internal fun getTipAmount() = vehicleTripArrayHolder.getTipAmount()

    internal fun setPimPayAmount(enterPayAmount: Double){
        vehicleTripArrayHolder.setPimPayment(enterPayAmount)
    }
    internal fun getPimPaySubscriptionAmount() = vehicleTripArrayHolder.getPimPaymentSubscriptionAmount()

    internal fun getPimPayAmount() = vehicleTripArrayHolder.getPimPayAmount()

    internal fun setPimPaidAmount(enterPimPaidAmount: Double){
        vehicleTripArrayHolder.setPimPaidAmount(enterPimPaidAmount)
    }

    internal fun getPimPaidAmount() = vehicleTripArrayHolder.getPimPaidAmount()

    internal fun pimDoesNotNeedToDoReceipt(boolean: Boolean){
       vehicleTripArrayHolder.pimDoesNotNeedToDoReceipt(boolean)
    }

    internal fun getPimNoReceipt() = vehicleTripArrayHolder.getPimNoReceipt()

    internal fun tripHasEnded() = vehicleTripArrayHolder.tripHasEnded()

    internal fun getTripHasEnded() = vehicleTripArrayHolder.getTripHasEnded()

    fun setAmountForSquareDisplay(double: Double){
        VehicleTripArrayHolder.setAmountForSquareDisplay(double)
    }

    fun getIsPimOnline() = VehicleTripArrayHolder.isPimOnline()
    internal fun setDriverId(driverId: Int){
        VehicleTripArrayHolder.setDriverId(driverId)
    }
    internal fun getDriverId() = VehicleTripArrayHolder.getDriverId()

    internal fun deviceIsBondedViaBT() = VehicleTripArrayHolder.deviceIsBondedViaBluetooth()

    internal fun doWeNeedToReAuthorizeSquare() = VehicleTripArrayHolder.doWeNeedToReAuthorizeSquare()

    internal fun isReaderConnected() = VehicleTripArrayHolder.isReaderStatusConnected()

    internal fun pimPairingValueChangedViaFMP(change: Boolean){
        VehicleTripArrayHolder.pimPairingViaFMPChanged(change)
    }

    internal fun isPimPaired() = VehicleTripArrayHolder.isPimPaired()

    internal fun isPIMOverheating() = vehicleTripArrayHolder.isPIMOverHeating()

    internal fun setPIMStartTime(startTime: String){
        vehicleTripArrayHolder.setPIMStartTime(startTime)
    }

    internal fun setPaymentMethod(paymentMethod: String){
        VehicleTripArrayHolder.insertPaymentMethod(paymentMethod)
    }

    internal fun checkForDriverRejection() = vehicleTripArrayHolder.checkForDriverRejection()


}