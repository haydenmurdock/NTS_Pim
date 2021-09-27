package com.example.nts_pim.utilities.sms_helper

import android.util.Log
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.trip.ReceiptPaymentInfo
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
    fun sendSMS(tripId: String, paymentMethod: String, transactionId: String, custPhoneNumber: String?){
        if(transactionId.isNullOrEmpty() || transactionId == " "){
            LoggerHelper.writeToLog("Issue with transactionId: transactionId == $transactionId", LogEnums.RECEIPT.tag)
        }

        val receiptPaymentInfo: ReceiptPaymentInfo? =
            VehicleTripArrayHolder.getReceiptPaymentInfo(tripId)
        LoggerHelper.writeToLog("Sending to receipt API." +
                " tripId: $tripId," +
                " paymentMethod: ${paymentMethod.toLowerCase()}," +
                " transactionId: $transactionId," +
                "custPhoneNumber: $custPhoneNumber," +
                " pimPayAmount: ${receiptPaymentInfo?.pimPayAmount}," +
                " owedPrice: ${receiptPaymentInfo?.owedPrice}," +
                " tipAmt: ${receiptPaymentInfo?.tipAmt}," +
                " tipPercent: ${receiptPaymentInfo?.tipPercent}," +
                " airportFee: ${receiptPaymentInfo?.airPortFee}, " +
                " discountAmt: ${receiptPaymentInfo?.discountAmt}," +
                " toll: ${receiptPaymentInfo?.toll}," +
                " discountPercent: ${receiptPaymentInfo?.discountPercent}," +
                " destLat: ${receiptPaymentInfo?.destLat}, " +
                " destLon: ${receiptPaymentInfo?.destLon}", LogEnums.RECEIPT.tag)
        val client = OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
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
                json.put("custPhoneNbr", custPhoneNumber)
                json.put("custEmail", null)
                json.put("pimPayAmt", receiptPaymentInfo?.pimPayAmount)
                json.put("owedPrice", receiptPaymentInfo?.owedPrice)
                json.put("tipAmt", receiptPaymentInfo?.tipAmt)
                json.put("tipPercent", receiptPaymentInfo?.tipPercent)
                json.put("airportFee", receiptPaymentInfo?.airPortFee)
                json.put("discountAmt", receiptPaymentInfo?.discountAmt)
                json.put("toll", receiptPaymentInfo?.toll)
                json.put("discountPercent", receiptPaymentInfo?.discountPercent)
                json.put("destLat", receiptPaymentInfo?.destLat)
                json.put("destLon", receiptPaymentInfo?.destLon)
            } catch (e: JSONException){
                Log.i("ERROR", "JSON error $e")
             }

        val body = json.toString().toRequestBody(jSON)
        Log.i("URL","Json body : $json")
        val url = URL("https://5s27urxc78.execute-api.us-east-2.amazonaws.com/prod/sendReceipt")
        //val url = URL("https://5s27urxc78.execute-api.us-east-2.amazonaws.com/test/sendReceipt")
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
              LoggerHelper.writeToLog("Send Text receipt unsuccessful. Client call error: $e", LogEnums.RECEIPT.tag)
              TripDetails.isReceiptSent = false
              TripDetails.receiptCode = e.hashCode()
              TripDetails.receiptMessage = e.localizedMessage ?: ""
          }
    }
}