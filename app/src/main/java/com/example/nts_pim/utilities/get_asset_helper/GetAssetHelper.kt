package com.example.nts_pim.utilities.get_asset_helper

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.example.nts_pim.activity.MainActivity
import okhttp3.*
import java.io.*


object GetAssetHelper {
    val testVideoURL = "https://account-logos.s3.us-east-2.amazonaws.com/pim-advertisements/1604075519295.mp4"
    val testImageURL = "https://account-logos.s3.us-east-2.amazonaws.com/pim-advertisements/ccsi_A.png"
    var videoInputStream: InputStream? = null
    val videoTag = "Asset_Helper_Video"
    val imageTag = "Asset_Helper_Image"

    fun getVideo(context: Context) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(testVideoURL)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                        Log.i("$videoTag", "Name: $name, value: $value")
                    }
                    try {
                        videoInputStream = response.body?.byteStream()
                        val buff = ByteArray(1024 * 4)
                        var downloaded: Long = 0
                        val target = response.body!!.contentLength()
                        val mediaFile = File(context.cacheDir, "mySuperVideo.mp4")
                        val output: OutputStream = FileOutputStream(mediaFile)
                        while (true) {
                            var completed = videoInputStream!!.read(buff)
                            if (completed == -1) {
                                break
                            }
                            output.write(buff, 0, completed)
                            //write buff
                            downloaded += completed.toLong()
                        }
                        output.flush()
                        output.close()
                        downloaded == target
                       val doesItExist = mediaFile.exists()
                    } catch (ignore: IOException) {
                        false
                    } finally {
                        Log.i("$videoTag", "input stream closed")
                        videoInputStream?.close()
                    }
                }
                println(response.body!!.string())
            }
        })
    }

    fun getImage(){

    }
}