package com.example.nts_pim.fragments_viewmodel.vehicle_setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.data.repository.trip_repository.TripRepository

class VehicleSetupModelFactory(
private val tripRepository: TripRepository
): ViewModelProvider.NewInstanceFactory() {

    // We have to suppress the function because we are casting the ViewModel as a Generic T

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return VehicleSetupViewModel(tripRepository) as T
    }
}
