package com.example.nts_pim.utilities.Square_Service

import android.app.Activity
import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.PimApplication
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.model_objects.JsonAuthCode
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.google.gson.Gson
import com.squareup.sdk.reader.ReaderSdk
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.Error

object SquareHelper {
    //variable to check if we have successfully init square reader sdk.
    private var isReaderSdkInit = false
    val logTag = LogEnums.SQUARE.tag
    val application = PimApplication.instance
    var isSquareAuthorized = false
    var isSquareAuthorizedMLD: MutableLiveData<Boolean>? = null


    private fun getSdkStatus() = isReaderSdkInit


   fun getAuthStatus():Boolean {
        isSquareAuthorized = ReaderSdk.authorizationManager().authorizationState.isAuthorized
        isSquareAuthorizedMLD?.postValue(isSquareAuthorized)
        return isSquareAuthorized
    }

    fun reauthorizeSquare(vehicleId: String?, mainActivity: Activity){
        val readerStatus = getSdkStatus()
        if(!readerStatus){
            initReaderSdk(application)
        }
        if(vehicleId == null){
            LoggerHelper.writeToLog("Can't re-auth square. No vehicle Id attached to pim", logTag)
            return
        }
        if(ReaderSdk.authorizationManager().authorizationState.canDeauthorize()){
            ReaderSdk.authorizationManager().deauthorize()
            LoggerHelper.writeToLog("Reader was de-authorized", logTag)
        }
        if(vehicleId.isNotEmpty()){
            LoggerHelper.writeToLog("$vehicleId: Trying to reauthorize", logTag)
            getAuthorizationCode(vehicleId, mainActivity)
        }
    }

    private fun getAuthorizationCode(vehicleId: String, mainActivity: Activity) {
        val url = "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleId"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.code == 200) {
                        val gson = Gson()
                        val convertedObject =
                            gson.fromJson(response.body?.string(), JsonAuthCode::class.java)
                        val authCode = convertedObject.authCode
                        LoggerHelper.writeToLog("Successfully got re-auth code", logTag)
                        onAuthorizationCodeRetrieved(authCode, mainActivity)
                    }
                    if(response.code == 404) {
                       LoggerHelper.writeToLog("Vehicle not found in fleet. Error Code: ${response.code}", logTag)
                        }
                    if (response.code == 401) {
                        LoggerHelper.writeToLog("Need to authorize fleet with log In. Error Code: ${response.code}", logTag)
                    }
                    if(response.code == 500){
                        LoggerHelper.writeToLog("Error code 500: error message: ${response.message}", logTag)
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    LoggerHelper.writeToLog("Failure getting auth code", logTag)
                }
            })
        } catch (e: Error) {
            LoggerHelper.writeToLog("error requesting auth code from aws. Error: $e", logTag)
        }
    }

    private fun onAuthorizationCodeRetrieved(authorizationCode: String, mainActivity: Activity) {
        LoggerHelper.writeToLog("Authorizing square on the MainThread", logTag)
       mainActivity.runOnUiThread {
           ReaderSdk.authorizationManager().authorize(authorizationCode)
       }
        isSquareAuthorized = true
        isSquareAuthorizedMLD?.postValue(isSquareAuthorized)
    }

    fun initReaderSdk(application: Application){
        //This might not get logged due to the logging function not being setup at the time of call
        isReaderSdkInit = try{
            ReaderSdk.initialize(application)
            LoggerHelper.writeToLog("Square's Reader SDK initialized", logTag)
            true
        } catch (e: Exception){
            LoggerHelper.writeToLog("Issue initializing square. Error: $e", logTag)
            false
        }
            LoggerHelper.writeToLog("Reader was init: $isReaderSdkInit", logTag)
    }
}