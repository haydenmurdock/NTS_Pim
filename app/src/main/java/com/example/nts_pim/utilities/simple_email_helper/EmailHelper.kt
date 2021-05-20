package com.example.nts_pim.utilities.simple_email_helper

import android.util.Log
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.trip.ReceiptPaymentInfo
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URL


object EmailHelper {

    fun sendEmail(tripId: String,  paymentMethod: String, transactionId: String){
        val receiptPaymentInfo: ReceiptPaymentInfo? =
            VehicleTripArrayHolder.getReceiptPaymentInfo(tripId) ?:
            return LoggerHelper.writeToLog("ReceiptPaymentInfo object was null. Not sending receipt to receiptAPI. Step 3 Failure", LogEnums.RECEIPT.tag)
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
            json.put("sendMethod","email")
            json.put("tripId","$tripId" )
            json.put("src","pim")
            json.put("paymentId", transactionId)
            json.put("pimPayAmt", receiptPaymentInfo?.pimPayAmount)
            json.put("owedPrice", receiptPaymentInfo?.owedPrice)
            json.put("tipAmt", receiptPaymentInfo?.tipAmt)
            json.put("tipPercent", receiptPaymentInfo?.tipPercent)
            json.put("airPortFee", receiptPaymentInfo?.airPortFee)
            json.put("discountAmt", receiptPaymentInfo?.discountAmt)
            json.put("toll", receiptPaymentInfo?.toll)
            json.put("discountPercent", receiptPaymentInfo?.discountAmt)
            json.put("destLat", receiptPaymentInfo?.destLat)
            json.put("destLon", receiptPaymentInfo?.destLon)

        } catch (e: JSONException){
            Log.i("ERROR", "JSON error $e")
        }
        val body = json.toString().toRequestBody(jSON)

      //  val url = "https://5s27urxc78.execute-api.us-east-2.amazonaws.com/prod/sendReceipt"
        val url = URL("https://5s27urxc78.execute-api.us-east-2.amazonaws.com/test/sendReceipt")
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        try {
           client.newCall(request).execute().use {response ->
                if (!response.isSuccessful) {
                    Log.i("Email Receipt", "Not successful: please check Email Helper function: response code: ${response.code}")
                }
                else {
                    Log.i("Email Receipt", "Receipt Successful: please check Email Helper function: response code: ${response.code}")
                    Log.i("Email Receipt", "sent receipt to email successfully. Step 3: complete")
                    TripDetails.isReceiptSent = true
                    TripDetails.receiptCode = response.code
                    TripDetails.receiptMessage = response.message
                }
            }
        }  catch (e: IOException) {
            Log.i("Email Receipt", "Not successful: please check Email Helper function")
            TripDetails.isReceiptSent = false
            TripDetails.receiptCode = e.hashCode()
            TripDetails.receiptMessage = e.localizedMessage ?: ""
        }
    }
}