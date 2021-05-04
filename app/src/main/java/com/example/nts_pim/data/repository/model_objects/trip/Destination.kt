package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.google.gson.JsonObject
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
        val destObj = JSONObject()
        try {
            destObj.put(JSON_DEST_ADDRESS, obj)
        }catch (ignore: Exception){}
        return destObj
    }

    companion object{
        //Bluetooth
        private const val JSON_DEST_NAME = "name"
        private const val JSON_DEST_STATE = "state"
        private const val JSON_DEST_CITY = "city"
        private const val JSON_DEST_STREET = "street"
        private const val JSON_DEST_ZIP = "zip"
        private const val JSON_DEST_LAT = "latitude"
        private const val JSON_DEST_LONG = "longitude"
        private const val JSON_DEST_ADDRESS = "destAddr"

    }

}