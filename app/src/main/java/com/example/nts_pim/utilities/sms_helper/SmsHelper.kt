package com.example.nts_pim.utilities.sms_helper

import android.util.Log
import com.example.nts_pim.data.repository.TripDetails
import okhttp3.*
import java.util.concurrent.TimeUnit
import org.json.JSONException
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.net.URL

object SmsHelper {

    fun sendSMS(tripId: String, paymentMethod: String, transactionId: String){
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
                json.put("sendMethod","text")
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
                      Log.i("Text Receipt", "Send Text receipt successful. Step 3: Complete")
                      TripDetails.isReceiptSent = true
                      TripDetails.receiptCode = response.code
                      TripDetails.receiptMessage = response.message
                  } else {
                      Log.i("Text Receipt", "Send Text receipt unsuccessful. Step 3: Fail")
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