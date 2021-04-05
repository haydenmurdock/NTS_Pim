package com.example.nts_pim.data.repository.model_objects.trip

/**
 * Trip is a data class that is used for requesting an upfront price and scheduling a trip from the enterDestination Screen.
 * Driver tablet is responsible for entering Trip into NTS and getting upfront price.
 * This is a bluetooth only communication object
 */

data class Trip(var pickup: Pickup?,
                val dest: Destination,
                var passengerName: String?,
                var upfrontPrice: UpFrontPrice?,
                var driverAccepted: Boolean,
                var dateOfTrip: String)