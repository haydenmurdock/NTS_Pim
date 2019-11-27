package com.example.nts_pim.fragments_viewmodel.welcome

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel

class WelcomeViewModel(
   private val tripRepository: TripRepository
) : TripViewModel(tripRepository) {
    internal fun getVehicleId() = tripRepository.getVehicleID()

    internal  fun getvehicleSettings() = tripRepository.getVehicleSettings()

    internal fun isSetupComplete() = tripRepository.isSetupComplete()

}