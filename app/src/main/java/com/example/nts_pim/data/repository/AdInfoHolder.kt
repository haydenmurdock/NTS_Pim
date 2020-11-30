package com.example.nts_pim.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.nts_pim.PimApplication
import java.io.File


/**
 * where application stores/converts advertisement info from GetPIMAdvertisementQuery
 */

object AdInfoHolder {
    var isThereAnAdd = false
    var adDuration: Int? = null
    var adUrl: String? = null
    var adContentType: String? = null

    internal fun setAddInformation(duration: Int?, contentType: String?, url: String?){
        adDuration = duration
        adContentType = contentType
        adUrl = url
        isThereAnAdd = true
        Log.i("Ad", "Ad info set internally. Ad duration: $adDuration, contentType: $adContentType, adUrl: $adUrl, thereIsAnAdd $isThereAnAdd")
        downloadVideo(adUrl!!)
    }

    internal fun getAdInfo():Pair<Int?, String?>{
        return Pair(adDuration, adContentType)
    }

     private fun downloadVideo(url: String){
         val context = PimApplication.pimContext
         //   getVideo(context, url)
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
    fun deleteExternalStoragePublicPicture() {
        // Create a path where we will place our picture in the user's
        // public pictures directory and delete the file.  If external
        // storage is not currently mounted this will fail.
        val path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val file = File(path, "DemoPicture.jpg")
        file.delete()
    }


    internal fun showAdd() = isThereAnAdd


}