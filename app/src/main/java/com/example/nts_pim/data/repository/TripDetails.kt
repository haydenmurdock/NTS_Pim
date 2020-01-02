package com.example.nts_pim.data.repository

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object TripDetails {
    // This is for debugging information.
    var tripStartTime: LocalDateTime = LocalDateTime.now()
    var tripEndTime: LocalDateTime = LocalDateTime.now()
    var isReceiptSent:Boolean = false
    var receiptCode = 0
    var receiptMessage = ""
    var receiptSentTo = ""

    init {
        tripStartTime = LocalDateTime.now()
        tripEndTime = LocalDateTime.now()
        isReceiptSent = false
        receiptCode = 0
        receiptMessage = ""
        receiptSentTo = ""
    }

    private fun resetAdvancedTripDetails(){
        tripStartTime = LocalDateTime.now()
        tripEndTime = LocalDateTime.now()
        isReceiptSent = false
        receiptCode = 0
        receiptMessage = ""
        receiptSentTo = ""
    }


}