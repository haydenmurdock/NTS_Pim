package com.example.nts_pim.utilities.simple_email_helper

import android.util.Log
import com.example.nts_pim.data.repository.TripDetails
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException


object EmailHelper {

    fun sendEmail(tripId: String,  paymentMethod: String, transactionId: String){
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
            json.put("sendMethod","email")
            json.put("tripId","$tripId" )
            json.put("src","pim")
            json.put("paymentId", transactionId)

        } catch (e: JSONException){
            Log.i("ERROR", "JSON error $e")
        }
        val body = RequestBody.create(JSON, json.toString())

        val url = "https://5s27urxc78.execute-api.us-east-2.amazonaws.com/prod/sendReceipt"
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        try {
           client.newCall(request).execute().use {response ->
                if (!response.isSuccessful) throw IOException("Unexpected code")
                else {
                    TripDetails.isReceiptSent = true
                    TripDetails.receiptCode = response.code
                    TripDetails.receiptMessage = response.message
                }
            }
        }  catch (e: IOException) {
            TripDetails.isReceiptSent = false
            TripDetails.receiptCode = e.hashCode()
            TripDetails.receiptMessage = e.localizedMessage
        }
    }
}