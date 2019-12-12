package com.example.nts_pim.utilities.sms_helper

import android.util.Log
import okhttp3.*
import java.util.concurrent.TimeUnit
import org.json.JSONException
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.net.URL


object SmsHelper {

    fun sendSMS(tripId: String, paymentMethod: String){
        val client = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val JSON = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
            try{
                json.put("paymentMethod", "${paymentMethod.toLowerCase()}")
                json.put("sendMethod","text")
                json.put("tripId","$tripId" )
                json.put("src", "pim")
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

                  if (!response.isSuccessful) throw IOException("Unexpected code $response")


              }
          } catch (e: Error){
              println("error with client request")
          }
    }
}