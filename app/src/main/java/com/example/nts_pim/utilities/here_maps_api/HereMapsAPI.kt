package com.example.nts_pim.utilities.here_maps_api

import com.amazonaws.async.Callback
import com.example.nts_pim.data.repository.model_objects.GeoLocation
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

object HereRouteAPI {
    private var _origin: String? = null
    private var _dest: String? = null
    private var _waypoints: String? = null
    private var _bShortestDist = false
    private var _bWithTraffic = false
    var lastResults: HereRouteResult? = null
        private set
    private const val URL_BASE =
        "https://fekjfbc9rb.execute-api.us-east-2.amazonaws.com/prod/route?"
    private const val URL_ORIGIN = "&startAddr=%f,%f"
    private const val URL_DESTINATION = "&destAddr=%f,%f"

    init {
        resetParameters()
    }

    fun addWaypoint(lat: Double, lon: Double) {
        _waypoints = if (_waypoints!!.length > 0) {
            String.format(Locale.US, "%s;%f,%f", _waypoints, lat, lon)
        } else {
            String.format(Locale.US, "%f,%f", lat, lon)
        }
    }

    fun buildURL(): String? {
        if (_origin!!.length == 0 || _dest!!.length == 0) return null
        val sb = StringBuilder()
        sb.append(URL_BASE)
        sb.append("apiKey=")
        sb.append(HereConst.API_KEY)
        // Add origin, destination, and waypoints.  Since we are currently only using lat/lon values for these, no need to
// URL encode them.
        sb.append(_origin)
        sb.append(_dest)
        if (_waypoints!!.isNotEmpty()) {
            sb.append("&waypoints=")
            sb.append(_waypoints)
        }
        if (_bShortestDist) {
            sb.append("&routeType=s")
        } else {
            sb.append("&routeType=f")
        }
        if (_bWithTraffic) {
            sb.append("&trafficMode=e")
        }
        return sb.toString()
    }

    fun getResults(cb: Callback<HereRouteResult?>) {
        Thread(object : Runnable {
            override fun run() {
                try {
                    val url = buildURL()
////                    if (url != null) {
////                      //  val resp: GenericHTTPResponse = WSHelper.sendHTTPRequest(url, 5000, 10000) THIS IS WHERE YOU WOULD MAKE THE REQUEST
////                        // For now, not going to worry about timeout errors and just going to check if there is a response.
////                        if (resp.ResponseText.length() > 0) {
////                            parseResponse(resp.ResponseText)
////                        }
//                    }
                } catch (e: Exception) {
//                    LoggerHelper.writeToLog(context, "HereRouteAPI.getResults")
                }
//                cb.callback(lastResults)
           }
        }).start()
    }

    /**
     * Checks to see if the last results is set and if the status is OK.
     *
     * @return True if we have results.
     */
    fun hasValidResults(): Boolean {
        return lastResults != null && lastResults!!.Status === HereConst.HereResultStatus.OK
    }

    fun parseResponse(resp: String): HereRouteResult {
        lastResults = HereRouteResult()
        try {
            lastResults!!.parseResp(resp)
        } catch (e: Exception) {

        }
        return lastResults!!
    }

    private fun resetParameters() {
        _origin = ""
        _dest = ""
        _waypoints = ""
        _bShortestDist = false
        _bWithTraffic = false
        lastResults = null
    }

    fun setDestination(lat: Double, lon: Double) {
        _dest = String.format(
            Locale.US,
            URL_DESTINATION,
            lat,
            lon
        )
    }

    fun setOrigin(lat: Double, lon: Double) {
        _origin =
            String.format(Locale.US, URL_ORIGIN, lat, lon)
    }

    /**
     * Call this method to change the default behavior of the query.
     *
     * @param getShortestDist Pass in true to get the shortest result instead of the fastest. Default is fastest.
     * @param withTraffic     Pass in true to get results with traffic conditions (current time of day will be used).
     * Default is without traffic.
     */
    fun setOptionalParams(
        getShortestDist: Boolean,
        withTraffic: Boolean
    ) {
        _bShortestDist = getShortestDist
        _bWithTraffic = withTraffic
    }

   class HereRouteResult internal constructor() {
        var Status: HereConst.HereResultStatus
        /**
         * Distance of route in meters. Set to -1 if there was no results.
         */
        var Distance: Int
        /**
         * Time of route in seconds. Set to -1 if there was no results.
         */
        var Time: Int
        var OriginLoc: GeoLocation?
        var DestLoc: GeoLocation?
        var Points: ArrayList<GeoLocation>
        var SystemError: String
        var MaxLat = 0.0
        var MaxLon = 0.0
        var MinLat = 0.0
        var MinLon = 0.0

        @Throws(JSONException::class)
        private fun parsePoints(points: JSONArray) {
            var json: JSONObject
            var loc: GeoLocation
            var i = 0
            val n = points.length()
            while (i < n) {
                json = points.getJSONObject(i)
                loc = GeoLocation(0.0, 0.0)
                loc.Latitude = json.getDouble(HereConst.JSON_LAT)
                loc.Longitude = json.getDouble(HereConst.JSON_LNG)
                if (MaxLat == 0.0) {
                    MinLat = loc.Latitude!!
                    MaxLat = MinLat
                    MinLon = loc.Longitude!!
                    MaxLon = MinLon
                } else {
                    if (loc.Latitude!! > MaxLat) MaxLat = loc.Latitude!!
                    if (loc.Longitude!! < MinLon) MinLon = loc.Longitude!!
                    if (loc.Longitude!! > MaxLon) MaxLon = loc.Longitude!!
                }
                Points.add(loc)
                i++
            }
        }

        @Throws(JSONException::class)
        fun parseResp(resp: String) {
            val root: JSONObject
            var geo: JSONObject
            val points: JSONArray
            val temp: String
            // Example response:
// {"status":"OK","length":2565,"time":454,"startGeo":{"lat":33.7699909,"lng":-118.1923664},"destGeo":{"lat":33.763175,"lng":-118.1964111},"points":[{"lat":33.7699909,"lng":-118.1923664},{"lat":33.7671018,"lng":-118.1923985},{"lat":33.7673378,"lng":-118.2018614},{"lat":33.7646556,"lng":-118.2018507},{"lat":33.7650114,"lng":-118.1969882},{"lat":33.7640011,"lng":-118.195188},{"lat":33.7636685,"lng":-118.1946087},{"lat":33.7630141,"lng":-118.1958532},{"lat":33.763175,"lng":-118.1964111}]}
            root = JSONObject(resp)
            temp = root.getString(HereConst.JSON_STATUS)
            when (temp) {
                HereConst.STATUS_OK -> {
                    Status = HereConst.HereResultStatus.OK
                    Distance = root.getInt(Companion.JSON_DIST)
                    Time = root.getInt(HereConst.JSON_TIME)
                    geo = root.getJSONObject(Companion.JSON_START_GEO)
                    OriginLoc = GeoLocation(0.0,0.0)
                    OriginLoc!!.Latitude = geo.getDouble(HereConst.JSON_LAT)
                    OriginLoc!!.Longitude = geo.getDouble(HereConst.JSON_LNG)
                    geo = root.getJSONObject(Companion.JSON_DEST_GEO)
                    DestLoc = GeoLocation(0.0, 0.0)
                    DestLoc!!.Latitude = geo.getDouble(HereConst.JSON_LAT)
                    DestLoc!!.Longitude = geo.getDouble(HereConst.JSON_LNG)
                    points = root.getJSONArray(Companion.JSON_POINTS)
                    parsePoints(points)
                }
                HereConst.STATUS_INVALID_API_KEY -> Status =
                    HereConst.HereResultStatus.INVALID_API_KEY
                HereConst.STATUS_INVALID_ORIG_ADDR -> Status =
                    HereConst.HereResultStatus.INVALID_ORIGIN_ADDR
                HereConst.STATUS_INVALID_DEST_ADDR -> Status =
                    HereConst.HereResultStatus.INVALID_DEST_ADDR
                HereConst.STATUS_SYSTEM_ERROR -> {
                    Status = HereConst.HereResultStatus.SYSTEM_ERROR
                    SystemError = root.getString(HereConst.JSON_SYSTEM_ERROR)
                }
            }
        }

        companion object {
            private const val JSON_DIST = "length"
            private const val JSON_START_GEO = "startGeo"
            private const val JSON_DEST_GEO = "destGeo"
            private const val JSON_POINTS = "points"
        }

        init {
            Status = HereConst.HereResultStatus.UNKNOWN
            Distance = -1
            Time = -1
            OriginLoc = null
            DestLoc = null
            Points = ArrayList<GeoLocation>()
            SystemError = ""
        }
    }
} // end of class HereRouteAPI
