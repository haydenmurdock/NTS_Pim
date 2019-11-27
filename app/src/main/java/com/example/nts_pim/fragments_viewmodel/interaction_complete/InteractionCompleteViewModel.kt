package com.example.nts_pim.fragments_viewmodel.interaction_complete

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel

class InteractionCompleteViewModel(
    val tripRepository: TripRepository
):TripViewModel(tripRepository){
    internal fun getVehicleID() = tripRepository.getVehicleID()
}