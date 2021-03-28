package com.example.nts_pim.data.repository

import com.example.nts_pim.data.repository.model_objects.Trip
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.util.*

/**
 * This is where we create/update/delete/hold trips that the Pim requests
 */

object TripController {
    var trip: Trip? = null

    fun createTrip(dropOffAddress: String){
        val currentDate = Date().toString()
        trip = Trip(null, dropOffAddress.trim(), null, null,null, false, currentDate)
        LoggerHelper.writeToLog("Trip created for upfront price on PIM. Trip Destination: ${trip!!.dropOffDest}", LogEnums.TRIP.tag)
    }

    fun updatePIMTrip(pickupAddress: String?, firstName: String?, lastName: String?, tripPrice: Float?, driverAccepted: Boolean){
        if(!pickupAddress.isNullOrBlank() && !pickupAddress.isNullOrEmpty()){
            trip?.pickupDest = pickupAddress
        }
        if(firstName.isNullOrBlank() && !firstName.isNullOrEmpty()){
            trip?.firstName = firstName.trim()
        }
        if(!lastName.isNullOrBlank() && !lastName.isNullOrEmpty()){
            trip?.lastName = lastName.trim()
        }
        if(tripPrice != null && !tripPrice.isNaN()){
            trip?.tripPrice = tripPrice
        }
        trip?.driverAccepted = driverAccepted
        LoggerHelper.writeToLog("PIM Requested Trip Updated. Trip: $trip", LogEnums.TRIP.tag)
    }

    fun getPIMCreatedTrip() = trip as Trip

}