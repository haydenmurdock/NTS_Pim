package com.example.nts_pim.fragments_viewmodel

import com.example.nts_pim.data.repository.model_objects.KeyboardWatcher
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.fragments_viewmodel.callback.CallbackViewModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModelFactory


object InjectorUtiles {

    fun provideCallBackModelFactory(): CallbackViewModelFactory {
        val vehicleTripArrayHolder = VehicleTripArrayHolder
        return CallbackViewModelFactory(vehicleTripArrayHolder)
    }

    fun provideSettingKeyboardModelFactory(): SettingsKeyboardViewModelFactory {
        val keyboardWatcher = KeyboardWatcher
        return SettingsKeyboardViewModelFactory(
            keyboardWatcher
        )
    }
}