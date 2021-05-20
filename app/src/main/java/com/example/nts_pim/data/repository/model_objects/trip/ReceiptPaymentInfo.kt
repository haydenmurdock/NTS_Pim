package com.example.nts_pim.data.repository.model_objects.trip



data class ReceiptPaymentInfo(val tripId: String,
                              val pimPayAmount: Double,
                              val owedPrice: Double,
                              val tipAmt: Double,
                              val tipPercent: Double,
                              val airPortFee: Double,
                              val discountAmt: Double,
                              val toll: Double,
                              val discountPercent: Double,
                              val destLat: Double,
                              val destLon: Double)