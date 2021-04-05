package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

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
                   var timeEst: Double,
                   var timeOfArrival: Double,
                   var error: String?
): NTSPimPacket.PimDataObj{

    override fun fromJson(obj: JSONObject) {
        error = obj.optString(JSON_UPFRONT_ERROR)
        price = obj.optDouble(JSON_UPFRONT_PRICE)
        airportFee = obj.optDouble(JSON_UPFRONT_AIRPORT_FEE)
        processingFee = obj.optDouble(JSON_UPFRONT_PROCESSING_FEE)
        miles = obj.optDouble(JSON_UPFRONT_MILES)
        timeEst = obj.optDouble(JSON_UPFRONT_TIME_EST)
        timeOfArrival = timeEst
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

    fun getTripLength(timeEst: Double): Double {

        val calendar = Calendar.getInstance()
        //val currentTime = currentDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        return 0.0
    }
    companion object{
        private const val JSON_UPFRONT_PRICE = "price"
        private const val JSON_UPFRONT_AIRPORT_FEE = "airportFee"
        private const val JSON_UPFRONT_PROCESSING_FEE = "processingFee"
        private const val JSON_UPFRONT_MILES = "miles"
        private const val JSON_UPFRONT_TIME_EST= "timeEst"
        private const val JSON_UPFRONT_ERROR = "error"
    }
}


