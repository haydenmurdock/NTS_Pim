package com.example.nts_pim.fragments_viewmodel.check_vehicle_info

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel
import com.example.nts_pim.internal.lazyDeferred

class CheckVehicleInfoViewModel(
    private val tripRepository: TripRepository
) : TripViewModel(tripRepository) {

    internal fun getVehicleID() = tripRepository.getVehicleID()

    internal fun doesCompanyNameExist() = tripRepository.doesCompanyNameExist()

    internal fun companyNameExists() = tripRepository.companyNameExists()

}
