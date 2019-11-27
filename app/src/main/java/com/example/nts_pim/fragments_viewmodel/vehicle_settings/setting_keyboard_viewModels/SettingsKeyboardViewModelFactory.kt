package com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.data.repository.model_objects.KeyboardWatcher

class SettingsKeyboardViewModelFactory(
    private val keyBoardWatcher: KeyboardWatcher
): ViewModelProvider.NewInstanceFactory() {

    // We have to suppress the function because we are casting the ViewModel as a Generic T

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SettingsKeyboardViewModel(
            keyBoardWatcher
        ) as T
    }
}