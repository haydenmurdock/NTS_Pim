package com.example.nts_pim.utilities

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.nts_pim.utilities.Square_Service.SquareService

class LifeCycleCallBacks : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {

    }

    override fun onActivityDestroyed(activity: Activity?) {
       val name = activity?.localClassName
        if (name == "com.squareup.ui.main.ApiMainActivity"){

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
            SquareService().enableTransactionMode(true)
            SquareService().processActivityResume(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity?) {
    }

    override fun onActivityStopped(activity: Activity?) {

    }

}