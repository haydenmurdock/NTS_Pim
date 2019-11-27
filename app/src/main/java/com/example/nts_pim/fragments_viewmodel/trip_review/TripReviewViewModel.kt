package com.example.nts_pim.fragments_viewmodel.trip_review

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel

class TripReviewViewModel(
    val tripRepository: TripRepository):
    TripViewModel(tripRepository)  {

    internal fun getVehicleId() = tripRepository.getVehicleID()
}