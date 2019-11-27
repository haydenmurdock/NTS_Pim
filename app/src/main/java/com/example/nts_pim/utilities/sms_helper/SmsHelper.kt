package com.example.nts_pim.utilities.sms_helper

import android.util.Log
import okhttp3.*
import java.util.concurrent.TimeUnit
import org.json.JSONException
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import java.net.URL


object SmsHelper {

    fun sendSMS(phoneNumber: String, tripNumber: Int, tripId: String, tripTotal: Double, vehicleId: String, paymentMethod: String){
        val client = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val JSON = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
            try{
                json.put("method","text")
                json.put("phone", "1$phoneNumber")
                json.put("tripNbr", tripNumber)
                json.put("tripId","$tripId" )
                json.put("totalFareAmt", tripTotal)
                json.put("vehicleId", "$vehicleId")
                json.put("paymentMethod", "$paymentMethod")

            } catch (e: JSONException){
                Log.i("ERROR", "JSON error $e")
            }
        val body = RequestBody.create(JSON, json.toString())

        val url = URL("https://5s27urxc78.execute-api.us-east-2.amazonaws.com/prod/receipt")

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
          try {
              val respone = client.newCall(request).execute()
              Log.i("URL","response code${respone.code}")
              respone.body.toString()
          }  catch (e: Error){
              print("line 45 sms helper $e" )
          }
    }
}