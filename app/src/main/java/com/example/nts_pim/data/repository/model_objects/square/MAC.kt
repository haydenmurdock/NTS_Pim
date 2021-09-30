package com.example.nts_pim.data.repository.model_objects.square

import android.app.Activity
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class MAC(id: String, receivedTime: LocalTime) {
    private var mId = id
    private var mReceivedTime = receivedTime
    private val logTag = "SQUARE"

    init {
        LoggerHelper.writeToLog("MAC Address: $mId, Time: $mReceivedTime", logTag)
    }

    fun isMACExpired(checkTime: LocalTime): Boolean {
        val timeBetween = ChronoUnit.HOURS.between(mReceivedTime, checkTime)
        LoggerHelper.writeToLog("MAC - Hours between last use: $timeBetween", logTag)
        if (timeBetween > 0) {
            LoggerHelper.writeToLog("MAC is expired", logTag)
            return true
        }
       LoggerHelper.writeToLog("MAC is not expired", logTag)
        return false
    }
    fun getId(): String {
        return mId
    }
}