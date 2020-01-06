package com.example.nts_pim

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager.LayoutParams
import java.lang.Exception

class UnlockScreenLock: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val w = window
            w.addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD)
            w.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON)
            val h = Handler()
            h.postDelayed(finishTask, 300)
        } catch (e: Exception) {
            println(e)
        }
    }
    private val finishTask =  Runnable {
          run {
            try {
                finish()
            }
            catch (e:Exception) {
                println(e)
            }
        }
    }
} // end of class WakeUp.
