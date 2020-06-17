package com.example.nts_pim.utilities

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.utilities.Square_Service.SquareService
import com.example.nts_pim.utilities.sound_helper.SoundHelper

class LifeCycleCallBacks : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {

    }

    override fun onActivityDestroyed(activity: Activity?) {
       val name = activity?.localClassName
        if (name == "com.squareup.ui.main.ApiMainActivity"){
                SoundHelper.turnOnSound(activity.applicationContext)
                val squareViewGroup = activity.findViewById<View>(android.R.id.content) as ViewGroup
                Log.i("Square", "Activity was destroyed, removing all views on Square Activity")
                squareViewGroup.removeAllViews()
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        val name = activity?.localClassName
        if (name == "com.squareup.ui.main.ApiMainActivity"){
            SquareService().enableTransactionMode(true)
            SquareService().processActivityResume(activity)
        }

    }

    override fun onActivityResumed(activity: Activity?) {

        val name = activity?.localClassName
        if (name == "com.squareup.ui.main.ApiMainActivity"){
            if(MainActivity.navigationController.currentDestination?.id != R.id.welcome_fragment){
                SquareService().enableTransactionMode(true)
                SquareService().processActivityResume(activity)
            } else {
                Log.i("Logger", "Closed square activity via on ActivityResumed since the welcome fragment was shown")
                activity.finish()
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity?) {
    }

    override fun onActivityStopped(activity: Activity?) {

    }

}