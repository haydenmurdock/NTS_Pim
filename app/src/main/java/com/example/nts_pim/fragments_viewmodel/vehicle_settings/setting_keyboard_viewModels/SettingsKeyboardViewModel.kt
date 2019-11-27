package com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels

import androidx.lifecycle.ViewModel
import com.example.nts_pim.data.repository.model_objects.KeyboardWatcher

class SettingsKeyboardViewModel(
   private val keyboardWatcher : KeyboardWatcher
): ViewModel() {
    internal fun isQwertyKeyboardUp() = keyboardWatcher.getQwertyKeyboardStatus()

    internal fun qwertyKeyboardisUp() = keyboardWatcher.qwertyKeyboardisUp()

    internal fun isPhoneKeyboardUp() = keyboardWatcher.getPhoneKeyboardStatus()

    internal fun phoneKeyboardIsUp() = keyboardWatcher.phoneKeyboardisUp()

    internal fun bothKeyboardsDown() = keyboardWatcher.bothKeyboardsareDown()

    internal fun enterBtnPressed(enterString: String){
        keyboardWatcher.enterButtonPressed(enterString)
    }

    internal fun isEnterBtnPressed() = keyboardWatcher.getWhatEnterButtonPressed()

    internal fun isKeyboardGoingForward() = keyboardWatcher.isphoneNumberKeyboardIsGoingForward()


}