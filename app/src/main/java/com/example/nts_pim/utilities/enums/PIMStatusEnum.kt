package com.example.nts_pim.utilities.enums

enum class PIMStatusEnum(val status: String) {
    WELCOME_SCREEN("WELCOME_SCREEN|Passenger is viewing welcome screen"),
    METER_SCREEN("METER_SCREEN|Passenger is viewing meter screen"),
    PAYMENT_SCREEN("PAYMENT_SCREEN|Passenger is viewing payment details"),
    TIP_SCREEN("TIP_SCREEN|Passenger is viewing tip screen"),
    CASH_PAYMENT("CASH_PAYMENT|Passenger has picked cash payment option"),
    STARTED_SQUARE_PAYMENT("STARTED_SQUARE_PAYMENT|Passenger has picked Square payment"),
    CANCELED_SQUARE_PAYMENT("ENDED_SQUARE_PAYMENT|Passenger has canceled Square payment"),
    SQUARE_PAYMENT_COMPLETE("PAYMENT_COMPLETE|Passenger has completed payment"),
    PAYMENT_ERROR("PAYMENT_ERROR|There was an error during Passenger payment"),
    RECEIPT_SCREEN("RECEIPT_SCREEN| Passenger is viewing a receipt screen"),
    PIM_PAIRING("PIM_PAIRING| Pim found driver tablet to pair via Bluetooth"),
    ERROR_UPDATING("ERROR| Syncing error. Try again"),
    SDK_NOT_AUTHORIZED("ERROR| SDK was not authorized to take a payment"),
    USAGE_ERROR("ERROR| USAGE ERROR"),
    OVERHEATING("OVERHEATING| PIM IS OVERHEATING")
}


// wait block for 3 seconds for a response.

