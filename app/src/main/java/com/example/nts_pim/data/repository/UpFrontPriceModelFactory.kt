package com.example.nts_pim.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UpFrontPriceModelFactory (private val upfrontPriceRepository: UpfrontPriceRepository):
    ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return UpfrontPriceViewModel(upfrontPriceRepository) as T
    }
}

