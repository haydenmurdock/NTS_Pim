package com.example.nts_pim.data.repository.model_objects.trip

import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import org.json.JSONObject

class Passenger(var name: String):
    NTSPimPacket.PimDataObj{
    override fun fromJson(obj: JSONObject) {

    }

    override fun toJson(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put(JSON_PASSENGER_NAME, name)
        } catch (ignore: Exception) {}
        return obj
    }
    companion object{
        const val JSON_PASSENGER_NAME = "name"
    }
}