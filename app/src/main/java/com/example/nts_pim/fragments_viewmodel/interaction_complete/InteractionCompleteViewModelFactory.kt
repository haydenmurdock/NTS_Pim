package com.example.nts_pim.fragments_viewmodel.interaction_complete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.data.repository.trip_repository.TripRepository

class InteractionCompleteViewModelFactory(
    private val tripRepository: TripRepository
): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return InteractionCompleteViewModel(tripRepository) as T
    }
}