package com.example.nts_pim.data.repository.model_objects

/**
 * Trip is a data class that is used for requesting an upfront price and scheduling a trip from the enterDestination Screen.
 * Driver tablet is responsible for entering Trip into NTS and getting upfront price.
 * This is a bluetooth only communication object
 */

data class Trip(var pickupDest: String?,
                val dropOffDest: String,
                var firstName: String?,
                var lastName: String?,
                var tripPrice: Float?,
                var driverAccepted: Boolean,
                var dateOfTrip: String)