package com.example.nts_pim.utilities.here_maps

import androidx.annotation.NonNull
import com.example.nts_pim.utilities.here_maps.HereConsts.HereResultStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit

object HerePlacesAPI {
    private const val URL_SUGGESTION =
        "https://2ebsozhfhb.execute-api.us-east-2.amazonaws.com/prod/suggest"
    private const val URL_DETAIL =
        "https://2ebsozhfhb.execute-api.us-east-2.amazonaws.com/prod/detail"

    /**
     * Gets a list of place suggestions based on info passed in. To get more details on a place, call getDetails().
     *
     * @param lat    Latitude of center location to start searching for places.
     * @param lon    Longitude of center location to start searching for places.
     * @param radius Radius in meters to limit the search results.
     * @param query  Address or place name text entered by user.
     * @param size   Number of suggestions to limity the results to.
     * @param cb     Callback function to call when the query is done.
     */
    fun getSuggestions(
        lat: Double, lon: Double, radius: Int, query: String?,
        size: Int, @NonNull cb: CallbackFunction<SuggestionResults>
    ) {
        Thread(Runnable {
            var results: SuggestionResults? = null
            val url: String
            val client = OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()
            try {
                url = String.format(
                    Locale.US,
                    "%s?apiKey=%s&lat=%f&lng=%f&radius=%d&query=%s&size=%d",
                    URL_SUGGESTION,
                    HereConsts.API_KEY,
                    lat,
                    lon,
                    radius,
                    URLEncoder.encode(query, "UTF-8"),
                    size
                )
                // Since this API call maybe be called every couple seconds as keys are typed, make the timeouts short.
                val request = Request.Builder()
                    .url(url)
                    .build()

                    client.newCall(request).execute().use { response ->
                      val responseString =  results?.parseResponse(response.toString())
                        results = SuggestionResults(query)
                        // The vicinity and highlighted vicinity sometimes have <br/> in them. Replace them with ", ".
                       // resp.ResponseText = resp.ResponseText.replace("<br/>", ", ")
                       // results.parseResponse(resp.ResponseText)
                    }
            } catch (e: java.lang.Exception) {

            }
            cb.callback(results)
        }).start()
    }

    /**
     * Gets more information for a suggestion result.
     *
     * @param tag Tag from place suggestion.
     * @param cb  Callback function to call when query is done.
     */
    fun getDetails(tag: String?, @NonNull cb: CallbackFunction<DetailResults?>
    ) {
        Thread(Runnable {
            var results: DetailResults? = null
            val url: String
            val client = OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()
            try {
                url = String.format(
                    "%s?apiKey=%s&tag=%s", URL_DETAIL, HereConsts.API_KEY,
                    URLEncoder.encode(tag, "UTF-8")
                )
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseString = results?.parseResponse(response.toString())
                    results = DetailResults()
                    results?.parseResponse(response.toString())
                }
            } catch (e: Exception) {
               // Common.logError(e, "HerePlacesAPI.getDetails")
            }
            cb.callback(results)
        }).start()
        }
    }
    data class PlaceSuggestion(var title: String, var highlightedTitle: String, var vicinity: String, var hightedLighted: String, var tag: String) {
        var Latitude = 0.0
        var Longitude = 0.0
        var Distance = 0
    }

   data class SuggestionResults(val Query: String?) {
        var Status: HereResultStatus
        var SystemError: String
        var Suggestions: ArrayList<PlaceSuggestion>

        @Throws(JSONException::class)
        fun parseResponse(resp: String?) {
            val root: JSONObject
            val array: JSONArray
            val temp: String
            root = JSONObject(resp)
            temp = root.getString(HereConsts.JSON_STATUS)
            when (temp) {
                HereConsts.STATUS_OK -> {
                    Status = HereResultStatus.OK
                    array = root.getJSONArray(JSON_RESULTS)
                    parseResults(array)
                }
                HereConsts.STATUS_INVALID_API_KEY -> Status =
                    HereResultStatus.INVALID_API_KEY
                HereConsts.STATUS_SYSTEM_ERROR -> {
                    Status = HereResultStatus.SYSTEM_ERROR
                    SystemError = root.getString(HereConsts.JSON_SYSTEM_ERROR)
                }
            }
        }

        @Throws(JSONException::class)
        private fun parseResults(array: JSONArray) {
            var obj: JSONObject
            var sug: PlaceSuggestion
            var i = 0
            val n = array.length()
            while (i < n) {
                obj = array.getJSONObject(i)
                sug = PlaceSuggestion(obj.optString(HereConsts.JSON_TITLE), obj.optString(JSON_HIGHLIGHTED_TITLE), obj.optString(JSON_VICINITY),obj.optString(JSON_HIGHLIGHTED_VICINITY), obj.getString(JSON_TAG))
                sug.Latitude = obj.optDouble(HereConsts.JSON_LAT, 0.0)
                sug.Longitude = obj.optDouble(HereConsts.JSON_LNG, 0.0)
                sug.Distance = obj.optInt(JSON_DISTANCE)
                Suggestions.add(sug)
                i++
            }
        }

        companion object {
            private const val JSON_RESULTS = "results"
            private const val JSON_HIGHLIGHTED_TITLE = "highlightedTitle"
            private const val JSON_VICINITY = "vicinity"
            private const val JSON_HIGHLIGHTED_VICINITY = "highlightedVicinity"
            private const val JSON_TAG = "tag"
            private const val JSON_DISTANCE = "distance"
        }

        init {
            Status = HereResultStatus.UNKNOWN
            SystemError = ""
            Suggestions = ArrayList()
        }
    } // end of class SuggestionResults

    class DetailResults internal constructor() {
        var Status: HereResultStatus
        var SystemError: String
        var Title: String
        var StreetNbr: String
        var StreetName: String
        var City: String
        var County: String
        var State: String
        var ZipCode: String
        var Latitude = 0.0
        var Longitude = 0.0
        val errorMessage: String
            get() {
                if (Status == HereResultStatus.INVALID_API_KEY) return "Invalid API key"
                return if (Status == HereResultStatus.SYSTEM_ERROR) SystemError else ""
            }

        @Throws(JSONException::class)
        fun parseResponse(resp: String?) {
            val root: JSONObject
            val obj: JSONObject
            val temp: String

            // Example response: {"status":"OK","detail":{"title":"Calle Tomás Aquino 100","strNbr":"100","strName":"Calle Tomás Aquino","city":"Tijuana","county":"","state":"BC","zip":"22204","lat":32.528969,"lng":-116.857651}}
            root = JSONObject(resp)
            temp = root.getString(HereConsts.JSON_STATUS)
            when (temp) {
                HereConsts.STATUS_OK -> {
                    Status = HereResultStatus.OK
                    obj = root.getJSONObject(JSON_DETAIL)
                    parseResults(obj)
                }
                HereConsts.STATUS_INVALID_API_KEY -> Status =
                    HereResultStatus.INVALID_API_KEY
                HereConsts.STATUS_SYSTEM_ERROR -> {
                    Status = HereResultStatus.SYSTEM_ERROR
                    SystemError = root.getString(HereConsts.JSON_SYSTEM_ERROR)
                }
            }
        }

        private fun parseResults(obj: JSONObject) {
            Title = obj.optString(HereConsts.JSON_TITLE)
            StreetNbr = obj.optString(JSON_STREET_NBR)
            StreetName = obj.optString(JSON_STREET_NAME)
            City = obj.optString(JSON_CITY)
            County = obj.optString(JSON_COUNTY)
            State = obj.optString(JSON_STATE)
            ZipCode = obj.optString(JSON_ZIP)
            Latitude = obj.optDouble(HereConsts.JSON_LAT, 0.0)
            Longitude = obj.optDouble(HereConsts.JSON_LNG, 0.0)
        }

        companion object {
            private const val JSON_DETAIL = "detail"
            private const val JSON_STREET_NBR = "strNbr"
            private const val JSON_STREET_NAME = "strName"
            private const val JSON_CITY = "city"
            private const val JSON_COUNTY = "county"
            private const val JSON_STATE = "state"
            private const val JSON_ZIP = "zip"
        }

        init {
            Status = HereResultStatus.UNKNOWN
            SystemError = ""
            Title = ""
            StreetNbr = ""
            StreetName = ""
            City = ""
            County = ""
            State = ""
            ZipCode = ""
        }
    } // end of class DetailResults
