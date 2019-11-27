package com.example.nts_pim.fragments_viewmodel.taxi_number

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel

class TaxiNumberViewModel(
    private val tripRepository: TripRepository
) : TripViewModel(tripRepository) {
    internal fun getVehicleSettings() = tripRepository.getVehicleSettings()
}