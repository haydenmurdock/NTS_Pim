package com.example.nts_pim.utilities.simple_email_helper
import android.util.Log
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType


object EmailHelper {

    fun sendEmail( tripId: String,  paymentMethod: String){
        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val JSON = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
        try{
            json.put("paymentMethod", "$paymentMethod")
            json.put("sendMethod","email")
            json.put("tripId","$tripId" )
            json.put("src","pim")

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
            val respone = client.newCall(request).execute()
            Log.i("URL","response code${respone.code}")
        }  catch (e: Error){
            print("line 45 sms helper $e" )
        }
    }
}