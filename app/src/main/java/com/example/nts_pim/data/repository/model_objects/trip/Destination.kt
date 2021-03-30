package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import org.json.JSONObject

class Destination(
    var destName: String,
    var destStreet: String,
    var destCity: String,
    var destState: String,
    var destZip: String,
    var destLat: Double,
    var destLong: Double
): NTSPimPacket.PimDataObj{
    init {
        print("destination created: $destName, $destStreet, $destCity, $destState, $destZip, $destLat, $destLong")
    }

    override fun fromJson(obj: JSONObject) {
        destName = obj.optString(JSON_DEST_NAME)
        destStreet = obj.optString(JSON_DEST_STREET)
        destCity = obj.optString(JSON_DEST_CITY)
        destState = obj.optString(JSON_DEST_STATE)
        destZip = obj.optString(JSON_DEST_ZIP)
        destLat = obj.optDouble(JSON_DEST_LAT)
        destLong = obj.optDouble(JSON_DEST_LONG)
    }

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put(JSON_DEST_NAME, destName)
            obj.put(JSON_DEST_STREET, destStreet)
            obj.put(JSON_DEST_CITY, destCity)
            obj.put(JSON_DEST_STATE, destState)
            obj.put(JSON_DEST_ZIP, destZip)
            obj.put(JSON_DEST_LAT, destLat)
            obj.put(JSON_DEST_LONG, destLong)
        } catch (ignore: Exception) {}
        return obj
    }

    companion object{
        private const val JSON_DEST_NAME = "destName"
        private const val JSON_DEST_STATE = "destState"
        private const val JSON_DEST_CITY = "destCity"
        private const val JSON_DEST_STREET = "destStreet"
        private const val JSON_DEST_ZIP = "destZip"
        private const val JSON_DEST_LAT = "destLat"
        private const val JSON_DEST_LONG = "destLong"
    }

}