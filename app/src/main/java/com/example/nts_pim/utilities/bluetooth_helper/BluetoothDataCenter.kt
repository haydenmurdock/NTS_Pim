package com.example.nts_pim.utilities.bluetooth_helper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nts_pim.data.repository.model_objects.BluetoothObject
import com.google.gson.Gson

object BluetoothDataCenter {
    private var responseMessage = ""
    private var responseMessageMutableLiveData =  MutableLiveData<String>()

    init {
        responseMessage = ""
    }

    internal fun updateResponseMessage(message: String){
        if(message != responseMessage) {
            responseMessage = message
            responseMessageMutableLiveData.postValue(responseMessage)
        }
    }

    internal fun getResponseMessage() = responseMessageMutableLiveData as LiveData<String>
}