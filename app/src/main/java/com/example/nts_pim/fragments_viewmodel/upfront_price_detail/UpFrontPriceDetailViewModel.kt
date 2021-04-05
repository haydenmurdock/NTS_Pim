package com.example.nts_pim.fragments_viewmodel.upfront_price_detail

import androidx.lifecycle.ViewModel
import com.example.nts_pim.data.repository.UpfrontPriceRepository

class UpFrontPriceDetailViewModel : ViewModel() {

    fun getUpFrontPriceDetails() = UpfrontPriceRepository.getUpfrontPriceDetails()
}
