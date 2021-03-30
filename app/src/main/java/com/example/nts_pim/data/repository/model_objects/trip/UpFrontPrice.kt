package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import org.json.JSONObject

/**
price
airportFee
processingFee (this is included in price and shouldn't be displayed anywhere)
miles
timeEst (minutes)
 */

class UpFrontPrice(var price: Double,
                   var airportFee: Double,
                   var processingFee: Double,
                   var miles: Double,
                   var timeEst: Double
): NTSPimPacket.PimDataObj{

    override fun fromJson(obj: JSONObject) {
        price = obj.optDouble(JSON_UPFRONT_PRICE)
        airportFee = obj.optDouble(JSON_UPFRONT_AIRPORT_FEE)
        processingFee = obj.optDouble(JSON_UPFRONT_PROCESSING_FEE)
        miles = obj.optDouble(JSON_UPFRONT_MILES)
        timeEst = obj.optDouble(JSON_UPFRONT_TIME_EST)
    }

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put(JSON_UPFRONT_PRICE, price)
            obj.put(JSON_UPFRONT_AIRPORT_FEE, airportFee)
            obj.put(JSON_UPFRONT_PROCESSING_FEE, processingFee)
            obj.put(JSON_UPFRONT_MILES, miles)
            obj.put(JSON_UPFRONT_TIME_EST, timeEst)
        } catch (ignore: Exception) {}
        return obj
    }
    companion object{
        private const val JSON_UPFRONT_PRICE = "price"
        private const val JSON_UPFRONT_AIRPORT_FEE = "airportFee"
        private const val JSON_UPFRONT_PROCESSING_FEE = "processingFee"
        private const val JSON_UPFRONT_MILES = "miles"
        private const val JSON_UPFRONT_TIME_EST= "timeEst"
    }
}


