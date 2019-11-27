package com.example.nts_pim.utilities.simple_email_helper
import android.util.Log
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType


object EmailHelper {

    fun sendEmail(email: String, tripNumber: Int, tripId: String, tripTotal: Double, vehicleId: String, paymentMethod: String){
        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val JSON = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
        try{
            json.put("method","email")
            json.put("email", "$email")
            json.put("tripNbr", tripNumber)
            json.put("tripId","$tripId" )
            json.put("totalFareAmt", tripTotal)
            json.put("vehicleId", "$vehicleId")
            json.put("paymentMethod", "$paymentMethod")

        } catch (e: JSONException){
            Log.i("ERROR", "JSON error $e")
        }
        val body = RequestBody.create(JSON, json.toString())

        val url = "https://5s27urxc78.execute-api.us-east-2.amazonaws.com/prod/receipt"

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