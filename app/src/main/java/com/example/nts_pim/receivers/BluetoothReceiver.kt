package com.example.nts_pim.receivers

import android.bluetooth.BluetoothAdapter.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nts_pim.data.repository.PIMSetupHolder


class BluetoothReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(ACTION_STATE_CHANGED)) {
                when(intent.getIntExtra(EXTRA_STATE, ERROR)) {
                     STATE_OFF -> {
                         PIMSetupHolder.pimBluetoothIsOff()

                     }
                    STATE_TURNING_OFF -> {

                    }
                    STATE_ON -> {
                        PIMSetupHolder.pimBluetoothIsOn()
                    }
                   STATE_TURNING_ON -> {

                   }
                }
            }
        }
    private fun turnOnBluetooth(){
        val mBluetoothAdapter = getDefaultAdapter()
        if (mBluetoothAdapter == null){
            PIMSetupHolder.pimBluetoothIsOff()
            return
        }
        if (!mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable()
            PIMSetupHolder.pimBluetoothIsOn()
        }
    }
}


