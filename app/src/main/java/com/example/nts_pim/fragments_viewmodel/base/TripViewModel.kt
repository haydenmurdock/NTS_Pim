package com.example.nts_pim.fragments_viewmodel.base

import androidx.lifecycle.ViewModel
import com.example.nts_pim.data.repository.trip_repository.TripRepository

abstract class TripViewModel(
   private val tripRepository: TripRepository
) : ViewModel () {
    // We use some like this here...
    // val weatherLocation by lazyDeferred{
    //            forecastRepository.getWeatherLocation()
    //       }
}