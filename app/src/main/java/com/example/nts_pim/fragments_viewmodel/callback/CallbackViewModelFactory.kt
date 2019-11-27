package com.example.nts_pim.fragments_viewmodel.callback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nts_pim.data.repository.VehicleTripArrayHolder

class CallbackViewModelFactory(
    private val vehicleTripArrayHolder: VehicleTripArrayHolder
): ViewModelProvider.NewInstanceFactory(){
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CallBackViewModel(vehicleTripArrayHolder) as T
    }
}
