package com.example.nts_pim.utilities.here_maps

/**
 * Suggest
 * lat = [latitude of center point for search]
lng = [longitude of center point for search]
query = [query string to search for]
Optional parameters:
radius = [radius in meters around search center point form which to draw results, defaults to 50000]
size = [number of results to return; defaults to 10]
Optional filters parameter:
options = [comma delimited list of options to filter request results]
Current options:
'sn'; only return results that have a street number in their title
Examples:
https://aza9uj33tf.execute-api.us-east-2.amazonaws.com/1/places/suggest?apiKey=020141f3-9758-43f0-9838-5420a2609e0e&lat=34.0231&lng=-118.3751&query=renaisssance+long+beach+hotel
Response:
{
"status": "OK",
"results": [
{
"title": "Renaissance-Long Beach Hotel",
"highlightedTitle": "<b>Renaissance</b>-<b>Long</b> <b>Beach</b> <b>Hotel</b>",
"vicinity": "111 E Ocean Blvd<br/>Long Beach, CA 90802",
"highlightedVicinity": "111 E Ocean Blvd<br/>Long Beach, CA 90802",
"tag": "ODQwOXE1Ym4tOTg3N2U1Mzg2N2I5N2ZiYzI4NWQwZWE4YmViOTFlOTQ7Y29udGV4dD1abXh2ZHkxcFpEMDVNVEUzWW1Vek1pMWxOamxsTFRWbU5HRXRPV1k1TmkweU5XUXpZekE0WW1ZeFltTmZNVFl4TnpnME9EWTNOVFUxT1Y4eE1EazBYekV3TXpJbWNtRnVhejB3",
"lat": 33.76754,
"lng": -118.19171,
"distance": 33035
},
{
"title": "Hotel Queen Mary (Queen Mary Long Beach)",
"highlightedTitle": "<b>Hotel</b> Queen Mary (Queen Mary <b>Long</b> <b>Beach</b>)",
"vicinity": "1126 Queens Hwy<br/>Long Beach, CA 90802",
"highlightedVicinity": "1126 Queens Hwy<br/>Long Beach, CA 90802",
"tag": "ODQwOXE1Ym4tNjYyZmM0YzRhY2Q0NDY3MGEyZTJmNjYxZTk5YmNmNzY7Y29udGV4dD1abXh2ZHkxcFpEMDVNVEUzWW1Vek1pMWxOamxsTFRWbU5HRXRPV1k1TmkweU5XUXpZekE0WW1ZeFltTmZNVFl4TnpnME9EWTNOVFUxT1Y4eE1EazBYekV3TXpJbWNtRnVhejB4",
"lat": 33.7526,
"lng": -118.19041,
"distance": 34527
}
]


Details

Returns the detail information for a single location response, based on its 'tag' value.
Parameter:
tag = [value of 'tag' element in /suggest response item]
Example:
https://aza9uj33tf.execute-api.us-east-2.amazonaws.com/1/places/detail?apiKey=020141f3-9758-43f0-9838-5420a2609e0e&tag=ODQwOXE1Ym4tOTg3N2U1Mzg2N2I5N2ZiYzI4NWQwZWE4YmViOTFlOTQ7Y29udGV4dD1abXh2ZHkxcFpEMWxNMlZrTWpBNVlTMWtaakEzTFRWaE1tSXRZVFUxWVMwNE5UUTFOR05oWTJJeU16ZGZNVFl4TnpnME9UQTJOVEl4TjE4ME5qWXhYemszTXpVbWNtRnVhejB3
Response:
{
"status": "OK",
"detail": {
"title": "Renaissance-Long Beach Hotel",
"strNbr": "111",
"strName": "E Ocean Blvd",
"city": "Long Beach",
"county": "Los Angeles",
"state": "CA",
"zip": "90802",
"lat": 33.76754,
"lng": -118.19171,
"categories": [
"500-5000-0000",
"500-5000-0053",
"700-7400-0284"
]
}
}
 */


import android.util.Log
import androidx.core.text.HtmlCompat
import com.example.nts_pim.data.repository.UpfrontPriceRepository
import com.example.nts_pim.data.repository.model_objects.here_maps.SuggestionResults
import com.example.nts_pim.data.repository.model_objects.trip.Destination
import com.example.nts_pim.data.repository.model_objects.trip.DetailDestination
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import java.util.stream.Collectors
import java.util.stream.IntStream


object HerePlacesAPI {
    private const val HERE_ENGINE_BASE_URL = "https://aza9uj33tf.execute-api.us-east-2.amazonaws.com/1"
    private const val PLACES_SUGGEST_PARAMETER = "/places/suggest?"
    private const val PLACES_DETAIL_PARAMETER = "/places/details?"
    private const val API_KEY = "apiKey=020141f3-9758-43f0-9838-5420a2609e0e"
    private const val NUMBER_OF_RESULTS = 15
    private const val RADIUS = 30000

    val client = OkHttpClient().newBuilder()
        .build()

    fun getSuggestedAddress(lat: Double, lng: Double, query: String){
        val listOfSuggestedLocations = mutableListOf<SuggestionResults>()
        val formattedQuery = query.filter { !it.isWhitespace() }.replace(" ", "+")
        val request =  Request.Builder()
          .url("$HERE_ENGINE_BASE_URL$PLACES_SUGGEST_PARAMETER$API_KEY&lat=$lat&lng=$lng&query=$formattedQuery&size=$NUMBER_OF_RESULTS")
          .build()
        Log.i("URL", "URL SUGGEST: ${request.url}")
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                LoggerHelper.writeToLog(
                    "Error connecting to get suggestion api. $e",
                    LogEnums.UPFRONT_PRICE.tag
                )
            }

            override fun onResponse(call: Call, response: Response) {
                println(response)
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body!!.string()
                val removedBrackets = android.text.Html.fromHtml(responseBody, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                val jsonObject = JSONObject(removedBrackets)
                Log.i("URL", "After JSON OBJECT: $jsonObject")
                val status: String? = jsonObject.optString("status")
                val results = jsonObject.getJSONArray("results")
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i).toString()
                    Log.i("URL", "Obj in results for index $i: $obj")
                    val suggestionResult = SuggestionResults(obj)
                    listOfSuggestedLocations.add(suggestionResult)
                }
                UpfrontPriceRepository.setSuggestedDestinations(listOfSuggestedLocations)
            }
        })

    }

    fun getDetailAddress(tag: String){

        /**
         * "status": "OK",
        "detail": {
        "title": "Renaissance-Long Beach Hotel",
        "strNbr": "111",
        "strName": "E Ocean Blvd",
        "city": "Long Beach",
        "county": "Los Angeles",
        "state": "CA",
        "zip": "90802",
        "lat": 33.76754,
        "lng": -118.19171,
        "categories": [
        "500-5000-0000",
        "500-5000-0053",
        "700-7400-0284"
         */
        val request =  Request.Builder()
            .url("$HERE_ENGINE_BASE_URL$PLACES_DETAIL_PARAMETER$API_KEY&tags=$tag")
            .build()

        Log.i("URL", "URL Detail: ${request.url}")
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                LoggerHelper.writeToLog(
                    "Error connecting to get detail api. $e",
                    LogEnums.UPFRONT_PRICE.tag
                )
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body!!.string()
                val removedBrackets = android.text.Html.fromHtml(responseBody, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                val updatedString = removedBrackets.replace("^[\n\r]", "").replace("[\n\r]$", "")
                Log.i("URL", "Before JSON OBJECT: $removedBrackets")
                val jsonObject = JSONObject(updatedString)
                Log.i("URL", "After JSON OBJECT: $jsonObject")
                val status: String? = jsonObject.optString("status")
                val results = jsonObject.getJSONArray("details")
                if(status == "OK"){
                    for (i in 0 until results.length()){
                        val obj = results.getJSONObject(i).toString()
                        val destinationDetails =  DetailDestination(obj)
                        Log.i("URL", "detail address: $destinationDetails")
                        val btDestination = destinationDetails.convertToDestination()
                        UpfrontPriceRepository.createUpfrontTrip(btDestination)
                    }
                }
                if(status == "QUERY_NOT_3_OR_MORE_CHARS"){
                    LoggerHelper.writeToLog("Error: $status", LogEnums.ERROR.tag)
                }
            }
        })
    }
}



