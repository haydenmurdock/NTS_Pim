package com.example.nts_pim.fragments_viewmodel.email_or_text

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel

class EmailOrTextViewModel(
    var tripRepository: TripRepository
):  TripViewModel(tripRepository) {

    internal fun getVehicleID() = tripRepository.getVehicleID()


}
