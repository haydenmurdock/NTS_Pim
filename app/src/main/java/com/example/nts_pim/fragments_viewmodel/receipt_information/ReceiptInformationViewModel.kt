package com.example.nts_pim.fragments_viewmodel.receipt_information

import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.fragments_viewmodel.base.TripViewModel

class ReceiptInformationViewModel(
    var tripRepository: TripRepository
):  TripViewModel(tripRepository) {

    internal fun getVehicleID() = tripRepository.getVehicleID()


}