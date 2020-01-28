package com.example.nts_pim.fragments_viewmodel.vehicle_settings_detail

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel
import com.example.nts_pim.internal.lazyDeferred

class VehicleSettingsDetailViewModel (
    private val tripRepository: TripRepository
) : TripViewModel(tripRepository) {

    internal fun getVehicleID() = tripRepository.getVehicleID()

    internal fun squareIsNotAuthorized() = tripRepository.squareIsNotAuthorized()

    internal fun vehicleIdDoesNotExist() = tripRepository.vehicleIdDoesNotExist()

    internal fun recheckAuth() = tripRepository.recheckAuthCode()

    internal fun companyNameNoLongerExists() = tripRepository.companyNameNoLongerExists()

}