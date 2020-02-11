package com.example.nts_pim.fragments_viewmodel.live_meter

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel

class LiveMeterViewModel(
    private val tripRepository: TripRepository
) : TripViewModel(tripRepository) {

    internal fun getVehicleID() = tripRepository.getVehicleID()
    internal fun getVehicleSettings() = tripRepository.getVehicleSettings()

}