package com.example.nts_pim.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.model_objects.AdDuration
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import java.io.File


/**
 * where application stores/converts advertisement info from GetPIMAdvertisementQuery
 */

object AdInfoHolder {
    private val mediaTypeVideo = "video/mp4"
    private val mediaTypeImage = "image/jpeg"
    private val imagePath = "/storage/emulated/0/Android/data/com.example.nts_pim/files/Download/PIM_Image"
    private val videoPath = "/storage/emulated/0/Android/data/com.example.nts_pim/files/Download/PIM_Video"
    var isThereAnAdd = false
    var adDuration: Int? = null
    var adUrl: String? = null
    var adContentType: String? = null
    val context = PimApplication.pimContext

    internal fun setAddInformation(duration: Int?, contentType: String?, url: String?){
        adDuration = duration
        adContentType = contentType
        adUrl = url
        isThereAnAdd = true
        Log.i("Ad", "Ad info set internally. Ad duration: $adDuration, contentType: $adContentType, adUrl: $adUrl, thereIsAnAdd $isThereAnAdd")
        LoggerHelper.writeToLog("Ad info set internally. Ad duration: $adDuration, contentType: $adContentType, adUrl: $adUrl, thereIsAnAdd $isThereAnAdd")
        saveDurationAmount(adDuration)
        if(adContentType == mediaTypeVideo){
            if(adUrl != null){
                downloadVideo(adUrl!!)
                Log.i("Ad", "Video ad downloading")
                LoggerHelper.writeToLog("Video ad downloading")
            } else {
                Log.i("Ad", "Video Ad url was null. This means there is one already saved to storage")
                LoggerHelper.writeToLog("Video Ad url was null. This means there is one already saved to storage")
            }
        }
        if(adContentType == mediaTypeImage){
            if(adUrl != null){
                downloadImage(context, adUrl!!)
                Log.i("Ad", "Image ad downloading")
                LoggerHelper.writeToLog("Image ad downloading")
            } else {
                Log.i("Ad", "Image ad url was null. This means there is one already saved to storage")
                LoggerHelper.writeToLog("Image ad url was null. This means there is one already saved to storage")
            }
        }
    }

    private fun saveDurationAmount(duration: Int?){
                if(duration != null) {
                    val newDurationAmount = AdDuration(duration)
                    ModelPreferences(context).putObject(
                        SharedPrefEnum.AD_DURATION.key,
                        newDurationAmount
                    )
                    Log.i("Ad", "Saved duration amount :$duration")
                    LoggerHelper.writeToLog("Saved duration amount :$duration")
                }
    }

    private fun getDurationAmount(): Int? {
        return ModelPreferences(context).getObject(SharedPrefEnum.AD_DURATION.key, AdDuration::class.java)?.time
    }

    internal fun getAdInfo():Pair<Int?, String?>{
        return Pair(adDuration, adContentType)
    }

     private fun downloadVideo(url: String){
         getVideo(context, url)
     }

    private fun downloadImage(context: Context, url: String){
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setTitle("Download")
            request.setDescription("Downloading Your File")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "PIM_Image")
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {

        }
        "/storage/emulated/0/Android/data/com.example.nts_pim/files/Download/PIM_Image"
    }
    private fun getVideo(context: Context, assetUrl: String) {
            try {
                val request = DownloadManager.Request(Uri.parse(assetUrl))
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                request.setTitle("Download")
                request.setDescription("Downloading Your File")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "PIM_Video")
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
            } catch (e: Exception) {

            }
        "/storage/emulated/0/Android/data/com.example.nts_pim/files/Download/PIM_Video"
    }
    internal fun deleteAds(){
        val videoFile = File(videoPath)
        val imageFile = File(imagePath)
        if(videoFile.exists()){
            try{
                videoFile.delete()
                LoggerHelper.writeToLog("videoFile deleted")
                Log.i("Ad", "Video file was deleted")

            } catch (e: Exception){

            }
        }
        if(imageFile.exists()){
            try{
                imageFile.delete()
                LoggerHelper.writeToLog("imageFile deleted")
                Log.i("Ad", "Image file was deleted")
            } catch (e: Exception){

            }
        }
    }


    internal fun showAdd() = isThereAnAdd


    internal fun checkForInternalAddInfo(){
        val videoFile = File(videoPath)
        val imageFile = File(imagePath)
        val length = getDurationAmount()
        if(videoFile.exists()){
            try{
                setAddInformation(length, mediaTypeVideo, null)
                Log.i("Ad", "Video file was found. Updated media type and Duration saved was $length")
                LoggerHelper.writeToLog("Video file was found. Updated media type and Duration saved was $length")
            } catch (e: Exception){
            }
        }

        if(imageFile.exists()){
            try{
               setAddInformation(length, mediaTypeImage, null)
                Log.i("Ad", "Image file was found. Updated media type and Duration saved was $length")
                LoggerHelper.writeToLog("Image file was found. Updated media type and Duration saved was $length")
            } catch (e: Exception){
            }
        }
    }
}