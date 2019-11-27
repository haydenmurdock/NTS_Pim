package com.example.nts_pim.utilities.view_helper

import android.app.Activity
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.isVisible
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*


object ViewHelper {
    //The qwerty keyboard watches these for view layout
    var isCapsLockOn = false
    var isSpecialCharOn = false
    //makes view visible and slides it up
    fun viewSlideUp(view: View, duration: Int) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(0f, 0f, view.height.toFloat(), 0f)
        animate.duration = duration.toLong()
        animate.fillAfter = true
        animate.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationRepeat(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationEnd(p0: Animation?) {
                view.clearAnimation()
            }
        })
        view.startAnimation(animate)
    }
    //slides view down and makes invisible
    fun viewSlideDown(view: View, duration: Int){
        val animate = TranslateAnimation(0f, 0f, 0f, view.height.toFloat())
        animate.duration = duration.toLong()
        animate.fillAfter = true
        animate.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationRepeat(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationEnd(p0: Animation?) {
                view.clearAnimation()
                view.visibility = View.GONE
            }
        })
        view.startAnimation(animate)
    }

    fun viewSlideDownTaxiView(view: View, duration: Int){
        val downAmount = (view.height + 150).toFloat()
        val animate = TranslateAnimation(0f, 0f, 0f, downAmount)
        animate.duration = duration.toLong()
        animate.fillAfter = true
        animate.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationRepeat(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationEnd(p0: Animation?) {
                view.clearAnimation()
                view.visibility = View.GONE
            }
        })
        view.startAnimation(animate)

    }

    fun disableButton(imageView: ImageView){
        if (imageView.isVisible){
           imageView.isEnabled = false
        }
    }

    fun formatDateUtcIso(date: Date?): String {
        if (date == null) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
        val formattedDateString = sdf.format(date)
        val fS = formattedDateString.removeSuffix("+0000")
        return fS.plus("Z")
    }


    fun hideSystemUI(activity: Activity) {
        //  Enables regular immersive mode.
        //   For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        //  Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

    }

}