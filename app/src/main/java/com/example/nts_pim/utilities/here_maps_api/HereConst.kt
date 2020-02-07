package com.example.nts_pim.utilities.here_maps_api

object HereConst {
    const val API_KEY = "020141f3-9758-43f0-9838-5420a2609e0e"
    // Parsing constants shared by multiple APIs (unique constants for an API will be inside the API's class).
    const val JSON_STATUS = "status"
    const val STATUS_OK = "OK"
    const val STATUS_INVALID_API_KEY = "API_KEY_NOT_VALID"
    const val STATUS_INVALID_ORIG_ADDR = "START_ADDR_NOT_VALID"
    const val STATUS_INVALID_DEST_ADDR = "DEST_ADDR_NOT_VALID"
    const val STATUS_SYSTEM_ERROR = "SYSTEM_ERROR"
    const val JSON_SYSTEM_ERROR = "error"
    const val JSON_TIME = "time"
    const val JSON_LAT = "lat"
    const val JSON_LNG = "lng"
    const val JSON_TITLE = "title"
    const val JSON_STREET_NBR = "strNbr"
    const val JSON_STREET_NAME = "strName"
    const val JSON_CITY = "city"
    const val JSON_COUNTY = "county"
    const val JSON_STATE = "state"
    const val JSON_ZIP = "zip"

    enum class HereResultStatus {
        UNKNOWN, OK, INVALID_API_KEY, INVALID_ORIGIN_ADDR, INVALID_DEST_ADDR, SYSTEM_ERROR
    }
}