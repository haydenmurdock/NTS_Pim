package com.example.nts_pim.data.repository

import androidx.lifecycle.ViewModel

class UpfrontPriceViewModel(
    private var upfrontPriceRepo: UpfrontPriceRepository
): ViewModel() {

    internal fun getUpfrontPriceSuggestDest() = upfrontPriceRepo.getSuggestedDestinations()

    internal fun getUpfrontPriceDest() = upfrontPriceRepo.getUpfrontPriceDestination()

    internal  fun hasUpfrontPriceUpdated() = upfrontPriceRepo.hasUpfrontPriceBeenUpdated()

    internal fun needToSendDestUpfrontPrice() = upfrontPriceRepo.needToSendDestinationUpfrontPrice()

    internal fun sentDestForUpfrontPrice()  = upfrontPriceRepo.sentDestinationUpfrontPrice()

    internal fun getUpFrontPriceDetails() = UpfrontPriceRepository.getUpfrontPriceDetails()

    internal fun clearUpfrontPriceTrip() = upfrontPriceRepo.clearUpfrontPrice()

    internal fun updateNameOfPassenger(name: String) {
        upfrontPriceRepo.updateNameForTrip(name)
    }

    internal fun getPassengersName() = upfrontPriceRepo.getPassengerName()

    internal fun driverSignedIn() =  upfrontPriceRepo.driverSignedIn()

    internal fun driverSignedOut() = upfrontPriceRepo.driverSignedOut()

    internal fun isDriverSignedIn() = upfrontPriceRepo.getIsDriverSignedIn()

    internal fun upfrontPriceError(errMsg: String){
        upfrontPriceRepo.errorForUpfrontPrice(errMsg)
    }

    internal fun upfrontPriceErrorDisplayed() = upfrontPriceRepo.errorDisplayed()

    internal fun wasThereUpfrontPriceError() = upfrontPriceRepo.wasThereAnUpfrontError()

    internal fun getUpfrontPriceError() = upfrontPriceRepo.upfrontTrip?.errorMsg


}