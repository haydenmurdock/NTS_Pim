package com.example.nts_pim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

import com.example.nts_pim.utilities.power_cycle.PowerAccessibilityService

class IntentReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        try {
            when (action) {
                "com.claren.tablet_control.reboot" -> {
                    intent.setClass(context, PowerAccessibilityService::class.java)
                    context.startService(intent)
                }
                "com.claren.tablet_control.shutdown" -> {
                    intent.setClass(context, PowerAccessibilityService::class.java)
                    context.startService(intent)
                }
                "com.claren.tablet_control.shutdown_knox" -> {
                    intent.setClass(context, PowerAccessibilityService::class.java)
                    context.startService(intent)
                }
            }

        } catch (e: Exception) {
        }

    }
}