package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

/**
price
airportFee
processingFee (this is included in price and shouldn't be displayed anywhere)
miles
timeEst (minutes)
 */

class UpFrontPrice(
    var price: Double,
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

    fun getFormattedTime(): String {
        val format = DecimalFormat("0")
        return format.format(timeEst).toString()
    }

    fun getRoundedMiles(): String {
      return  BigDecimal(miles).setScale(1, RoundingMode.HALF_EVEN).toString()
    }

    fun getTripLength(): String {
        val cal = Calendar.getInstance()
        // Time estimate is returned as an integer when getting upfront price so casting to int here won't loose
        // precision here.
        cal.add(Calendar.MINUTE, timeEst.toInt())
        val formatTime = "%1"+"$"+"tI:%1"+"$"+"tM%1"+"$"+"tp" // "1$" makes it so format only needs one parameter
        return String.format(Locale.US,formatTime,cal)
    }
    fun getTripPrice(): String{
        //We need to include the airport fee to the trip price
        val decimalFormat = DecimalFormat("####00.00")
        val totalTripPrice = price + airportFee
        return decimalFormat.format(totalTripPrice)
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


