package com.example.nts_pim.utilities.here_maps

import android.util.Log
import androidx.core.text.HtmlCompat
import com.example.nts_pim.data.repository.UpfrontPriceRepository
import com.example.nts_pim.data.repository.model_objects.here_maps.SuggestionResults
import com.example.nts_pim.data.repository.model_objects.trip.DetailDestination
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import okhttp3.*
import java.io.IOException
import org.json.JSONObject

object HerePlacesAPI {
    private const val HERE_ENGINE_BASE_URL = "https://aza9uj33tf.execute-api.us-east-2.amazonaws.com/1"
    private const val PLACES_SUGGEST_PARAMETER = "/places/suggest?"
    private const val PLACES_DETAIL_PARAMETER = "/places/details?"
    private const val API_KEY = "apiKey=020141f3-9758-43f0-9838-5420a2609e0e"
    private const val NUMBER_OF_RESULTS = 6

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



