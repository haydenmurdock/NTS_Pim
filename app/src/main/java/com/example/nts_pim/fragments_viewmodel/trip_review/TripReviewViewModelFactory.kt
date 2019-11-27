package com.example.nts_pim.fragments_viewmodel.trip_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.data.repository.trip_repository.TripRepository

class TripReviewViewModelFactory(
    private val tripRepository: TripRepository
): ViewModelProvider.NewInstanceFactory() {

    // We have to suppress the function because we are casting the ViewModel as a Generic T

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TripReviewViewModel(tripRepository) as T
    }
}