package com.example.nts_pim.utilities.sms_helper

import android.util.Log
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import okhttp3.*
import java.util.concurrent.TimeUnit
import org.json.JSONException
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL

object SmsHelper {
    /**
    pimPayAmt
    owedPrice
    tipAmt
    tipPercent
    airportFee
    discountAmt
    toll
    discountPercent
    destLat
    destLon
     */
    fun sendSMS(tripId: String, paymentMethod: String, transactionId: String){
        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val jSON = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
            try{
                json.put("paymentMethod", "${paymentMethod.toLowerCase()}")
                json.put("sendMethod","text")
                json.put("tripId",tripId)
                json.put("src", "pim")
                json.put("paymentId",transactionId)
                json.put("pimPayAmt", 10.00)
                json.put("owdPrice", 10.00)
                json.put("tipAmt", 2.00)
                json.put("tipPercent", 0.20)
                json.put("airPortFee", 1.10)
                json.put("discountAmt", 1.00)
                json.put("toll", 1.0)
                json.put("discountPercent", 00.10)
                json.put("destLat", 123.00)
                json.put("destLon", -123.00)
            } catch (e: JSONException){
                Log.i("ERROR", "JSON error $e")
             }

        val body = json.toString().toRequestBody(jSON)
        Log.i("URL","Json body : $json")
        //val url = URL("https://5s27urxc78.execute-api.us-east-2.amazonaws.com/prod/sendReceipt")
        val url = URL("https://5s27urxc78.execute-api.us-east-2.amazonaws.com/test/sendReceipt")
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
          try {
              client.newCall(request).execute().use { response ->
                  LoggerHelper.writeToLog("SMS receipt response code : ${response.code} response message: ${response.message} response: $response", LogEnums.RECEIPT.tag)
                  if (response.isSuccessful){
                      LoggerHelper.writeToLog("Send Text receipt successful. Step 3: Complete", LogEnums.RECEIPT.tag)
                      TripDetails.isReceiptSent = true
                      TripDetails.receiptCode = response.code
                      TripDetails.receiptMessage = response.message
                  } else {
                      Log.i("Text_Receipt", "Send Text receipt unsuccessful. Step 3: Fail")
                      LoggerHelper.writeToLog("Send Text receipt unsuccessful. ${response.message} ${response.code}", LogEnums.RECEIPT.tag)
                      TripDetails.isReceiptSent = false
                      TripDetails.receiptCode = response.code
                      TripDetails.receiptMessage = response.message
                      Log.i("Text_Receipt", "${response.message} ${response.code}")
                  }
              }
          } catch (e: Error){
              Log.i("Text Receipt", "Send Text receipt unsuccessful. Step 3: Fail. Error")
              LoggerHelper.writeToLog("Send Text receipt unsuccessful. Client call error: $e", LogEnums.RECEIPT.tag)
              TripDetails.isReceiptSent = false
              TripDetails.receiptCode = e.hashCode()
              TripDetails.receiptMessage = e.localizedMessage ?: ""
          }
    }
}