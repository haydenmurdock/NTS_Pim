package com.example.nts_pim.utilities.logging_service

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.nts_pim.data.repository.model_objects.VehicleID
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


object LoggerHelper {
    var logging = false
    private val dateTimeStamp = SimpleDateFormat("yyyy_MM_dd").format(Date())
    private val FILENAME = "Pim_log_$dateTimeStamp.txt"
    private val timeTimeStamp = SimpleDateFormat("HH:mm:ss")
    val charset = Charsets.UTF_8
    private var path: File? = null
    private var logToSend: String? = null
    private var vehicleId: String? = null
    private const val logFragmentStartStamp = "-LOG FRAGMENT START-"
    private const val logFragmentEndStamp = "-LOG FRAGMENT END-"
    private const val permissionGranted = 0


    internal fun writeToLog (context: Context, log: String){
        val readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if(logging && writePermission == permissionGranted && readPermission == permissionGranted){
            if (vehicleId == null){
                vehicleId = ModelPreferences(context).getObject(SharedPrefEnum.VEHICLE_ID.key, VehicleID::class.java)?.vehicleID
            }
            val logTimeStamp = timeTimeStamp.format(Date())
            if(logToSend == null){
                logToSend = logFragmentStartStamp + logTimeStamp + "_" + log + "\n"
            } else {
                logToSend += logTimeStamp + "_" + log + "\n"
            }
//            path = File(Environment.getExternalStorageDirectory().path)
//            if(!path!!.exists()){
//                path!!.mkdir()
//            }
//            path = File(path, "PIM_LOGGING")
//            if(!path!!.exists()){
//                path!!.mkdir()
//            }
//            path  = File(path, FILENAME)
//            var fos =  FileOutputStream(path,true)
//            try {
//                fos = context.openFileOutput(FILENAME, 0)
//                fos.write(log.toByteArray(charset))
//                Log.i("LOGGER", "wrote to file to $path")
//            } catch (ex: FileNotFoundException){
//                Log.i("LOGGER", "FAIL for file exeption ${ex.message}")
//                print(ex.message)
//            } catch (e: IOException){
//                Log.i("LOGGER", "FAIL for IOEXCEPTION: ${e.message}")
//            } finally {
//                if(fos != null){
//                    try {
//                        fos.close()
//                    }catch (e: IOException){
//                        Log.i("LOGGER", "FAIL for fos Close IOEXCEPTION: ${e.message}")
//                    }
//                }
//            }
        } else {
            Log.i("LOGGER", "Not logging information," +
                    " logging == $logging, " +
                    "Read permission == $readPermission," +
                    " write permission == $writePermission ")
        }
    }
    private fun readInternalLog(context: Context) {
        var fis: FileInputStream? = null
        try {
            fis = context.openFileInput(FILENAME)
            val irs = InputStreamReader(fis)
            val reader = BufferedReader(irs)
            val sb = StringBuilder()
            var line: String? = null
            while ({ line = reader.readLine(); line }() != null) {
             sb.append(line).append("\n")
            }
            Log.i("LOGGER", "Log: ${sb}")
        } catch (e: FileNotFoundException) {
            Log.i("LOGGER", "FAIL for file not found opening log: ${e.message}")
        } finally {
            try {
                if (fis != null) {
                    fis.close()
                }
            } catch (e: IOException) {
                Log.i("LOGGER", "FAIL for closing FIS: ${e.message}")
            }
        }
    }

   internal fun sendLogToAWS(EnteredVehicleId: String, context: Context){
       var vehicleIdForLog = EnteredVehicleId
       if (EnteredVehicleId == ""){
           vehicleIdForLog = vehicleId ?: ""
       }
       if(logToSend == null){
           Log.i("LOGGER", "Nothing changed during Logging period. Log not sent")
           return
       }
       logToSend += logFragmentEndStamp

       Log.i("LOGGER", "Sending log to AWS: LOG: $logToSend")
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
            json.put("text", "$logToSend"+"\n")
        } catch (e: JSONException){
            Log.i("ERROR", "JSON error $e")
        }
        val body = RequestBody.create(JSON, json.toString())
        Log.i("LOGGER","Json body :  ${json}")
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
                        logToSend = null
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
}
