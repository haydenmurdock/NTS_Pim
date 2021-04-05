package com.example.nts_pim.data.repository

import com.example.nts_pim.data.repository.model_objects.trip.*
import java.time.LocalDate
import java.time.LocalDateTime

object UpfrontPriceRepository {
    var trip: Trip? = null



    fun createUpfrontTrip(destination: Destination){
        val date = LocalDateTime.now().toString()
        trip = Trip(null,
                    destination,
                    null,
                    null,
                    false,
                    date
        )
    }

    fun updateTrip(pickup: Pickup, upfrontPrice: UpFrontPrice){
        trip?.pickup = pickup
        trip?.upfrontPrice = upfrontPrice
    }

    fun updateNameForTrip(passenger: Passenger){
        trip?.passengerName = passenger.name
    }

    fun getUpfrontPriceDetails() = trip

}