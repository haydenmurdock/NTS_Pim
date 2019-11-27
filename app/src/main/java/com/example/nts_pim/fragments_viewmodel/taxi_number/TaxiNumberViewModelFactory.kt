package com.example.nts_pim.fragments_viewmodel.taxi_number

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.data.repository.trip_repository.TripRepository

class TaxiNumberViewModelFactory (
    private val tripRepository: TripRepository
): ViewModelProvider.NewInstanceFactory() {

    // We have to suppress the function because we are casting the ViewModel as a Generic T

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TaxiNumberViewModel(tripRepository) as T
    }
}