package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.data.repository.UpfrontPriceRepository
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import org.json.JSONObject

/**
 * Trip is a data class that is used for requesting an upfront price and scheduling a trip from the enterDestination Screen.
 * Driver tablet is responsible for entering Trip into NTS and getting upfront price.
 * This is a bluetooth only communication object
 */
 class UpfrontTrip(var pickup: Pickup?,
                   var dest: Destination?,
                   var passengerName: String?,
                   var upfrontPrice: UpFrontPrice?,
                   var driverAccepted: Boolean,
                   var errorMsg: String?):NTSPimPacket.PimDataObj {
    override fun fromJson(obj: JSONObject) {
        try {
           errorMsg = obj.optString(JSON_ERROR_MSG)
            LoggerHelper.writeToLog(
                "Upfront Price Error: $errorMsg",
                LogEnums.BLUETOOTH.tag
            )
            UpfrontPriceRepository.errorForUpfrontPrice(errorMsg)
        } finally {
        }
        if (errorMsg.isNullOrEmpty()) {
            val pickupJson = obj.getJSONObject(JSON_PICK_UP_ADDRESS)
            val destJson = obj.getJSONObject(JSON_DEST_ADDRESS)
            val upfrontPriceJson = obj.getJSONObject(JSON_UPFRONT_PRICE)
            val pickUp = Pickup("", "", "", "", "", 0.0, 0.0)
            pickUp.fromJson(pickupJson)
            pickup = pickUp

            val destination = Destination("", "", "", "", "", 0.0, 0.0)
            destination.fromJson(destJson)
            dest = destination

            val upfrontP = UpFrontPrice(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "")
            upfrontP.fromJson(upfrontPriceJson)
            upfrontPrice = upfrontP

            UpfrontPriceRepository.updateTrip(pickup!!, upfrontPrice!!)
        }
    }

    override fun toJson(): JSONObject {
        return JSONObject()
    }

    companion object {
        const val JSON_PICK_UP_ADDRESS = "pickupAddr"
        const val JSON_DEST_ADDRESS = "destAddr"
        const val JSON_UPFRONT_PRICE = "upfrontPrice"
        const val JSON_ERROR_MSG = "errorMsg"
    }
}
