package com.example.nts_pim.data.repository

import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.time.LocalDateTime

object TripDetails {
    // This is for debugging information.
    var textToSpeechActivated = false
    var tripStartTime: LocalDateTime = LocalDateTime.now()
    var tripEndTime: LocalDateTime = LocalDateTime.now()
    var isReceiptSent:Boolean = false
    var receiptCode = 0
    var receiptMessage = ""
    var receiptSentTo = ""
    var completedTripIds = mutableListOf<String>()
    var tripIncompleteIdForDriverReceipt = ""

    init {
        textToSpeechActivated = false
        tripStartTime = LocalDateTime.now()
        tripEndTime = LocalDateTime.now()
        isReceiptSent = false
        receiptCode = 0
        receiptMessage = ""
        receiptSentTo = ""
    }


    fun insertTripIdIntoCompleted(tripId: String){
        // This puts the recent trip at the end of the list. So the oldest trip is index zero.
        if(tripId.isEmpty() || tripId.isBlank()){
            LoggerHelper.writeToLog("Trip id not added since it was not formatted as a trip id. Trip id: $tripId", LogEnums.TRIP_STATUS.tag)
            return
        }
       if(!completedTripIds.contains(tripId)) {
           completedTripIds.add(tripId)
           LoggerHelper.writeToLog("$tripId added to completedTrips", LogEnums.TRIP_STATUS.tag)
        } else {
           LoggerHelper.writeToLog("$tripId was not re-added to Completed Trips", LogEnums.TRIP_STATUS.tag)
       }

        if(completedTripIds.count() == 4){
            removeOldestTripId()
            LoggerHelper.writeToLog("There should be 3 completed trip ids now: Count = ${completedTripIds?.count()}", LogEnums.TRIP_STATUS.tag)
        }
    }

    fun isThisForAnOldTrip(tripId: String): Boolean {
        if(completedTripIds.contains(tripId)){
            LoggerHelper.writeToLog("Trip id: $tripId is a completed tripId", LogEnums.TRIP_STATUS.tag)
            return true
        }
        return false
    }

    fun tripIncompleteUseThisTripIdForDriverReceipt(tripId: String){
        tripIncompleteIdForDriverReceipt = tripId
    }
    fun getIncompleteDriverReceiptTripId() = tripIncompleteIdForDriverReceipt


   private fun removeOldestTripId(){
        val tripIdToRemove = completedTripIds.first()
        LoggerHelper.writeToLog("Oldest trip id is being removed from completed trips list. Removing $tripIdToRemove", LogEnums.TRIP_STATUS.tag)
        completedTripIds.remove(tripIdToRemove)
    }

}