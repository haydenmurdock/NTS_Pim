package com.example.nts_pim.data.repository.model_objects.trip

import org.json.JSONObject

class DetailDestination(json: String): JSONObject(json) {

    val title = this.optString(JSON_TITLE)
    val strNbr = this.optString(JSON_STR_NBR)
    val strName = this.optString(JSON_STR_NAME)
    val city = this.optString(JSON_CITY)
    val county = this.optString(JSON_COUNTY)
    val state = this.optString(JSON_STATE)
    val zip = this.optString(JSON_ZIP)
    val lat = this.optDouble(JSON_LAT)
    val lng = this.optDouble(JSON_LNG)
    val categories = this.optJSONArray(JSON_CATEGORIES)

    companion object {
        private const val JSON_TITLE = "title"
        private const val JSON_STR_NBR = "strNbr"
        private const val JSON_STR_NAME = "strName"
        private const val JSON_CITY = "city"
        private const val JSON_COUNTY = "county"
        private const val JSON_STATE = "state"
        private const val JSON_ZIP = "zip"
        private const val JSON_LAT = "lat"
        private const val JSON_LNG = "lng"
        private const val JSON_CATEGORIES = "categories"
    }

    fun convertToDestination(): Destination {
        val streetAddress = strNbr + strName
       return Destination(title, streetAddress,city, state, zip, lat, lng)
    }

}