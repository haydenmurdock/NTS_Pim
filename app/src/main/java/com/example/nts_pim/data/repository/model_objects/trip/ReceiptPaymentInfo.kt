package com.example.nts_pim.data.repository.model_objects.trip



data class ReceiptPaymentInfo(var tripId: String,
                              var pimPayAmount: Double,
                              var owedPrice: Double,
                              var tipAmt: Double,
                              var tipPercent: Double,
                              var airPortFee: Double,
                              var discountAmt: Double,
                              var toll: Double,
                              var discountPercent: Double,
                              var destLat: Double,
                              var destLon: Double)