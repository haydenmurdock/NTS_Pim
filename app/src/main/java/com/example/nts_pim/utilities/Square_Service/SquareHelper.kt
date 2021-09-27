package com.example.nts_pim.utilities.Square_Service

import android.app.Activity
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.model_objects.JsonAuthCode
import com.example.nts_pim.data.repository.model_objects.square.MAC
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.google.gson.Gson
import com.squareup.sdk.reader.ReaderSdk
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit

object SquareHelper {
    //variable to check if we have successfully init square reader sdk.
    val logTag = LogEnums.SQUARE.tag
    val application = PimApplication.instance
    var isSquareAuthorized = false
    var isSquareAuthorizedMLD: MutableLiveData<Boolean>? = null

    fun saveMAC(id: String, context: Context){
        val lastMAC = getLastMAC(context)
        val isExpired = lastMAC?.isMACExpired(LocalTime.now())

        if(isExpired == null){
            LoggerHelper.writeToLog("Saving MAC. Prior MAC didn't exist", LogEnums.SQUARE.tag)
            val mac = MAC(id, LocalTime.now())
            ModelPreferences(context).putObject(SharedPrefEnum.MAC.key, mac)
        }
        if(isExpired != null
            && isExpired){
            LoggerHelper.writeToLog("Saving MAC. Last MAC expired: $isExpired", LogEnums.SQUARE.tag)
            val mac = MAC(id, LocalTime.now())
            ModelPreferences(context).putObject(SharedPrefEnum.MAC.key, mac)
        }
    }

    fun getLastMAC(context: Context):MAC? {
        return ModelPreferences(context).getObject(SharedPrefEnum.MAC.key, MAC::class.java)
    }

     fun isMACExpired(context: Context):Boolean? {
     return getLastMAC(context)?.isMACExpired(LocalTime.now())
    }

    fun deleteMac(reason: String, context: Context){
        val settings =
            context.getSharedPreferences("MODEL_PREFERENCES", Context.MODE_PRIVATE)
        settings.edit().remove(SharedPrefEnum.MAC.key).apply()
        LoggerHelper.writeToLog("Deleted MAC due to $reason", LogEnums.SQUARE.tag)
    }

    fun deAuthorizeSquare(activity: Activity){
        //This is for testing purposes
        activity.runOnUiThread {
            ReaderSdk.authorizationManager().deauthorize()
        }
    }


    fun authorizeSquare(vehicleId: String?, mainActivity: Activity, screen: String){
            if(ReaderSdk.authorizationManager().authorizationState.canDeauthorize()){
                ReaderSdk.authorizationManager().deauthorize()
                LoggerHelper.writeToLog("Reader was de-authorized", logTag)
            }
            val isLastMACExpired = isMACExpired(mainActivity.applicationContext)
            if(isLastMACExpired == null || isLastMACExpired){
                LoggerHelper.writeToLog(
                    "MAC was expired or didn't exist. Getting NEW MAC for authorization.",
                    LogEnums.SQUARE.tag
                )
                getMobileAuthCode(vehicleId!!, mainActivity, screen)
            } else {
                val lastMACid = getLastMAC(mainActivity.applicationContext)?.getId() ?: ""
                LoggerHelper.writeToLog(
                    "Last mac id was less than 1 hour old. Using OLD MAC for authorization. Id: $lastMACid",
                    LogEnums.SQUARE.tag
                )
                if(lastMACid != ""){
                    mainActivity.runOnUiThread {
                        ReaderSdk.authorizationManager().authorize(lastMACid)
                    }
                } else {
                    LoggerHelper.writeToLog(
                        "Last mac id was less than 1 hour old, but MAC wasn't formatted correctly. Getting new MAC",
                        LogEnums.SQUARE.tag
                    )
                    getMobileAuthCode(vehicleId!!, mainActivity, screen)
                }
            }
    }

    private fun getMobileAuthCode(vehicleId: String, mainActivity: Activity, screen: String) {
        val dateTime = ViewHelper.formatDateUtcIso(Date())
        val url = "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleId&source=PIM&eventTimeStamp=$dateTime&extraInfo=$screen"
        val client = OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
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
                    if (response.code == 404) {
                        LoggerHelper.writeToLog(
                            "Vehicle not found in fleet. Error Code: ${response.code}",
                            logTag
                        )
                    }
                    if (response.code == 401) {
                        LoggerHelper.writeToLog(
                            "Need to authorize fleet with log In. Error Code: ${response.code}",
                            logTag
                        )
                    }
                    if (response.code == 500) {
                        LoggerHelper.writeToLog(
                            "Error code 500: error message: ${response.message}",
                            logTag
                        )
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
        saveMAC(authorizationCode, mainActivity.applicationContext)
        isSquareAuthorized = true
        isSquareAuthorizedMLD?.postValue(isSquareAuthorized)
    }
}