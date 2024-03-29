package com.example.nts_pim.utilities.logging_service

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.airbnb.lottie.L
import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.model_objects.VehicleID
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.ConcurrentModificationException


object LoggerHelper {
    var logging = false
    var loggingTime:Long = 3000
    @SuppressLint("SimpleDateFormat")
    private val dateTimeStamp = SimpleDateFormat("yyyy_MM_dd").format(Date())
    @SuppressLint("SimpleDateFormat")
    private val timeTimeStamp = SimpleDateFormat("HH:mm:ss")
    val charset = Charsets.UTF_8
    private var logToSendAWS: String? = null
    private var vehicleId: String? = null
    private const val logFragmentStartStamp = "-LOG FRAGMENT START-"
    private const val logFragmentEndStamp = "-LOG FRAGMENT END-"
    private const val permissionGranted = 0
    private val pimContext = PimApplication.pimContext
    private var logArray: MutableList<String?>? = null
    private const val logLimit = 201
    private var canAddToLog = false

    internal fun writeToLog(log: String, tag: String?){
        if(!canAddToLog){
            Log.i(tag, "Can't add: $log to official logs since it hasn't been cleared of previous logs")
            return
        }
        if(tag != null){
            Log.i(tag, log)
        }

        val readPermission = ContextCompat.checkSelfPermission(pimContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(pimContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val logTimeStamp = timeTimeStamp.format(Date())
        addLogToInternalLogs(logTimeStamp + "_" + log + "\n")
        if(logging && writePermission == permissionGranted && readPermission == permissionGranted){
            if (vehicleId == null){
                vehicleId = ModelPreferences(pimContext).getObject(SharedPrefEnum.VEHICLE_ID.key, VehicleID::class.java)?.vehicleID
            }
            if(logToSendAWS == null){
                logToSendAWS = logFragmentStartStamp + logTimeStamp + "_" + log + "\n"
            } else {
                logToSendAWS += logTimeStamp + "_" + log + "\n"
            }
        }
    }

   internal fun sendLogToAWS(enteredVehicleId: String){
       var vehicleIdForLog = enteredVehicleId
       if (enteredVehicleId == ""){
           vehicleIdForLog = vehicleId ?: ""
       }
       if(logToSendAWS == null){
           return
       }
       logToSendAWS += logFragmentEndStamp

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val JSON = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
        try{
            json.put("fileName", "PIM/$vehicleIdForLog"+"_"+"$dateTimeStamp.txt")
            json.put("text", "$logToSendAWS"+"\n")
        } catch (e: JSONException){
            Log.i("ERROR", "JSON error $e")
        }
        val body = json.toString().toRequestBody(JSON)
        Log.i("LOGGER","Json body :  $json")
        val url = URL("https://y22euz5gjh.execute-api.us-east-2.amazonaws.com/prod/uploadLogs")
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", "AdxSCAHcze6iyaJZJZIEgaEgThUGW78LaSemE2US")
            .addHeader("token", "GPAVNHeKTCvG4FFz")
            .post(body)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if(response.isSuccessful) {
                    Log.i(
                        "LOGGER",
                        "response code : ${response.code} response message: ${response.message}")
                }
                when(response.code) {
                    200 -> {
                        logToSendAWS = null
                        Log.i("LOGGER", "Successful log uploaded to AWS. Clearing Out local log")
                    }
                    else -> {
                        Log.i("LOGGER", "Log response code unexpected. ${response.code}. Local Log was not cleared")
                    }
                }

            }
        } catch (e: IOException){
            Log.i(
                "LOGGER", "Error from Log Upload $e")
        }
    }

    internal fun getOrStartInternalLogs(){
        if(!canAddToLog){
            return
        }
        logArray = getArrayList()
       // val logWithSizeCheck = keepTheLogCountLow(logArray)
        saveArrayList(logArray)
        Log.i("Logger", "Internal Log Started")
    }

    fun deleteLogsOverLimit(): Boolean {
        val mLogs = getArrayList()
        val currentLogAmount = mLogs?.size
        Log.i("Logger, ","current log amount before delete check. $currentLogAmount")
        if (currentLogAmount != null) {
            if (currentLogAmount > logLimit) {
                Log.i("Logger", "currentAmount is larger. $currentLogAmount vs. $logLimit")
                val deleteUpTo = currentLogAmount - logLimit
                var numberDeleted = 0
                Log.i("Logger", "deleting up to log limit of $deleteUpTo")
                while (numberDeleted < deleteUpTo) {
                    with(mLogs.iterator()) {
                        forEach {
                            if (it == mLogs.first()) {
                                numberDeleted += 1
                                remove()
                                Log.i(
                                    "Logger",
                                    "number of logs deleted: $numberDeleted. Logs left: ${mLogs.count()}"
                                )
                            }
                        }
                    }
                }
            }
        }
        Log.i("Logger", "number of logs in mLogs after log delete check: ${mLogs?.count()}")
        saveArrayList(mLogs)
        canAddToLog = true
        getOrStartInternalLogs()
        return canAddToLog
    }

    private fun addLogToInternalLogs(log:String){
        if(!canAddToLog){
            return
        }
        try {
            logArray?.add(log)
            removeAndAddLog(logArray)
        } catch (e: ConcurrentModificationException) {

        }
    }

    private fun removeAndAddLog(array: MutableList<String?>?){
        if (canAddToLog){
            return
        }
        if(array == null){
            return
        }
        if(array.count() > logLimit){
            while (array.count() > logLimit) {
                with(array.iterator()) {
                    forEach {
                        if (it == array.first()) {
                            remove()
                            Log.i(
                                "Logger",
                                "deleted logs since array count was greater than log limit. Log count after deletion: ${array.count()}"
                            )
                        }
                    }
                }
            }
        } else {
            Log.i("Logger", "Internal Log hasn't hit $logLimit, Log count is now ${array?.count()}")
        }
        if(!array.isNullOrEmpty()){
            saveArrayList(array)
        }
    }

    private fun saveArrayList(list: MutableList<String?>?) {
        if(list == null) {
            return
        }
        try {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                PimApplication.pimContext)
            val editor: SharedPreferences.Editor = prefs.edit()
            val gson = Gson()
            val json: String = gson.toJson(list)
            editor.putString("InternalLogs", json)
            editor.apply()
        } catch (e: ConcurrentModificationException){
          Log.i("Error", "Concurrent modification exception: $e")
        }
    }

    private fun getArrayList(): ArrayList<String?>? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(PimApplication.pimContext)
        val gson = Gson()
        val json = prefs.getString("InternalLogs", null)
        val type =
            object : TypeToken<ArrayList<String?>?>() {}.type
        return gson.fromJson(json, type)
    }

    internal fun addInternalLogsToAWS(vehicleId: String) {
        if(!canAddToLog){
            return
        }
        val logArray = logArray ?: mutableListOf<String>()
       try {
            logArray.forEach {log ->
                logToSendAWS += log
           }
            logArray.clear()
       } catch (e: ConcurrentModificationException){
            Log.i("Error", "Concurrent modification exception when adding and sending logs: $e. ")
        }
        sendLogToAWS(vehicleId)
    }
}
