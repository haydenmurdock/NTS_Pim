package com.example.nts_pim.fragments_viewmodel.check_vehicle_info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.data.repository.trip_repository.TripRepository

class CheckVehicleInfoModelFactory(
    private val tripRepository: TripRepository
): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CheckVehicleInfoViewModel(tripRepository) as T
    }
}