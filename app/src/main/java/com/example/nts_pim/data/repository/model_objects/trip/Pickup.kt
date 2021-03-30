package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import org.json.JSONObject


class Pickup(
    var pickupName: String,
    var pickupStreet: String,
    var pickupCity: String,
    var pickupState: String,
    var pickupZip: String,
    var pickupLat: Double,
    var pickupLong: Double
): NTSPimPacket.PimDataObj{

    init {
        print("destination created: $pickupName, $pickupStreet, $pickupCity, $pickupState, $pickupZip, $pickupLat, $pickupLong")
    }

    override fun fromJson(obj: JSONObject) {
        pickupName = obj.optString(JSON_PICKUP_NAME)
        pickupStreet = obj.optString(JSON_PICKUP_STREET)
        pickupCity = obj.optString(JSON_PICKUP_CITY)
        pickupState = obj.optString(JSON_PICKUP_STATE)
        pickupZip = obj.optString(JSON_PICKUP_ZIP)
        pickupLat = obj.optDouble(JSON_PICKUP_LAT)
        pickupLong = obj.optDouble(JSON_PICKUP_LONG)
    }

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put(JSON_PICKUP_NAME, pickupName)
            obj.put(JSON_PICKUP_STREET, pickupStreet)
            obj.put(JSON_PICKUP_CITY, pickupCity)
            obj.put(JSON_PICKUP_STATE, pickupState)
            obj.put(JSON_PICKUP_ZIP, pickupZip)
            obj.put(JSON_PICKUP_LAT, pickupLat)
            obj.put(JSON_PICKUP_LONG, pickupLong)
        } catch (ignore: Exception) {}
        return obj
    }

    companion object{
        private const val JSON_PICKUP_NAME = "pickupName"
        private const val JSON_PICKUP_STATE = "pickupState"
        private const val JSON_PICKUP_CITY = "pickupCity"
        private const val JSON_PICKUP_STREET = "pickupStreet"
        private const val JSON_PICKUP_ZIP = "pickupZip"
        private const val JSON_PICKUP_LAT = "pickupLat"
        private const val JSON_PICKUP_LONG = "pickupLong"
    }

}


