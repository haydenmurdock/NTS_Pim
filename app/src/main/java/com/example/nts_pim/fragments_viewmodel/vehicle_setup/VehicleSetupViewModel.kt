package com.example.nts_pim.fragments_viewmodel.vehicle_setup

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel
import com.example.nts_pim.internal.lazyDeferred

class VehicleSetupViewModel(
private val tripRepository: TripRepository
) : TripViewModel(tripRepository) {
    internal fun doesVehicleIDExist() = tripRepository.doesVehicleIDExist()

    internal fun vehicleIDExists() = tripRepository.vehicleIDSaved()

    internal fun squareIsAuthorized() = tripRepository.squareIsAuthorized()

    internal fun isSquareAuthorized() = tripRepository.isSquareAuthorized()

    internal fun isThereAuthCode() = tripRepository.isThereAuthCode()

    internal fun pinWasEnteredWrong() = tripRepository.pinWasEnteredWrong()

    internal fun isPinEnteredWrong() = tripRepository.isPinEnteredWrong()

    internal fun successfulAuthCode() = tripRepository.authCodeSuccess()
    val getDeviceID by lazyDeferred{
        tripRepository.getDeviceID()
    }
    val getPin by lazyDeferred {
        tripRepository.getVehiclePIN()
    }

    internal fun getVehicleID() = tripRepository.getVehicleID()

    internal fun isSetUpComplete()= tripRepository.isSetupComplete()

    internal fun watchSetUpComplete() = tripRepository.watchSetUpComplete()

    internal fun squareIsNotAuthorized() = tripRepository.squareIsNotAuthorized()

    internal fun vehicleIdDoesNotExist() = tripRepository.vehicleIdDoesNotExist()

    internal fun recheckAuth() = tripRepository.recheckAuthCode()

    internal fun companyNameNoLongerExists() = tripRepository.companyNameNoLongerExists()

}