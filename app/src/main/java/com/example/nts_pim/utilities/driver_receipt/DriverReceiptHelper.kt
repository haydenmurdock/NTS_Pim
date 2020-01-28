package com.example.nts_pim.utilities.driver_receipt

import android.util.Log
import com.example.nts_pim.data.repository.TripDetails
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

object DriverReceiptHelper {
    fun sendReceipt(tripId: String, paymentMethod: String, transactionId: String){
        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val JSON = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
        try{
            json.put("paymentMethod", "${paymentMethod.toLowerCase()}")
            json.put("sendMethod","none")
            json.put("tripId",tripId)
            json.put("src", "pim")
            json.put("paymentId",transactionId)
        } catch (e: JSONException){
            Log.i("ERROR", "JSON error $e")
        }

        val body = RequestBody.create(JSON, json.toString())
        Log.i("URL","Json body :  ${json}")
        val url = URL("https://5s27urxc78.execute-api.us-east-2.amazonaws.com/prod/sendReceipt")

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                Log.i("URL","response code : ${response.code} response message: ${response.message}")

                if (response.isSuccessful){
                    Log.i("Driver Receipt", "Sent Driver receipt successful.")
                    TripDetails.isReceiptSent = true
                    TripDetails.receiptCode = response.code
                    TripDetails.receiptMessage = response.message
                } else {
                    Log.i("Driver Receipt", "Sent Driver receipt successful.")
                    TripDetails.isReceiptSent = false
                    TripDetails.receiptCode = response.code
                    TripDetails.receiptMessage = response.message
                }
            }
        } catch (e: Error){
            TripDetails.isReceiptSent = false
            TripDetails.receiptCode = e.hashCode()
            TripDetails.receiptMessage = e.localizedMessage
        }
    }
}