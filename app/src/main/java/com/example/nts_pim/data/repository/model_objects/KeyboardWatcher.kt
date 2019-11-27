package com.example.nts_pim.data.repository.model_objects

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object KeyboardWatcher {

    private var isQwertyKeyboardUp = false
    private var isQwertyKeyboardUpLiveData = MutableLiveData<Boolean>()

    private var isPhoneKeyboardUp = false
    private var isPhoneKeyboardUpLiveData = MutableLiveData<Boolean>()

    private var isEnterBtnOnKeyboardPressed = "Neither"
    private var isEnterBtnOnKeyboardPressedLiveData = MutableLiveData<String>()

    private var phoneNumberKeyboardIsGoingForward = true

     fun qwertyKeyboardisUp(){
        isQwertyKeyboardUp = true
        isQwertyKeyboardUpLiveData.value =
            isQwertyKeyboardUp
        Log.i("Keyboard", "Live data has been updated to Qwerty keyboard up")
    }

     fun phoneKeyboardisUp() {
        isPhoneKeyboardUp = true
        isPhoneKeyboardUpLiveData.value =
            isPhoneKeyboardUp
        Log.i("Keyboard", "Live data has been updated to phone keyboard up")
    }

     fun bothKeyboardsareDown(){
        isQwertyKeyboardUp = false
        isPhoneKeyboardUp = false
        isQwertyKeyboardUpLiveData.value =
            isQwertyKeyboardUp
        isPhoneKeyboardUpLiveData.value =
            isPhoneKeyboardUp
        Log.i("Keyboard", "Live data has been updated to both keyboards are are down")
    }

    fun enterButtonPressed(keyboardPressed: String){
        isEnterBtnOnKeyboardPressed = keyboardPressed
        isEnterBtnOnKeyboardPressedLiveData.value =
            isEnterBtnOnKeyboardPressed
        bothKeyboardsareDown()
    }

    fun getQwertyKeyboardStatus() = isQwertyKeyboardUpLiveData as LiveData<Boolean>

    fun getPhoneKeyboardStatus() = isPhoneKeyboardUpLiveData as LiveData<Boolean>

    fun getWhatEnterButtonPressed() = isEnterBtnOnKeyboardPressedLiveData as LiveData<String>

    fun phoneNumberKeyboardIsGoingBackward(){
        phoneNumberKeyboardIsGoingForward = false
    }

    fun isphoneNumberKeyboardIsGoingForward() = phoneNumberKeyboardIsGoingForward

    fun phoneNumberIsGoingForward(){
        phoneNumberKeyboardIsGoingForward = true
    }
}
