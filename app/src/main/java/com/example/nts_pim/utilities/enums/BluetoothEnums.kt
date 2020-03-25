package com.example.nts_pim.utilities.enums

enum class BluetoothEnums(private val command: String) {
    /**
     * Command to return after receiving a valid packet.
     */
    ACK("OK"),
    /**
     * Command to return if a packet wasn't valid.
     */
    NACK("NO"),
    /**
     * Command containing data for the current status of the Driver App (such as vehicle status, trip info, and meter
     * status).
     */
    MDT_STATUS("MS"),
    /**
     * Command to start payment process on PIM. Should contain all the data needed to start payment.
     */
    START_PAYMENT("SP"),
    /**
     * Command Driver App can send if a payment should be canceled (may not end up using this command).
     */
    CANCEL_PAYMENT("CP"),
    /**
     * Command PIM App should send when a payment is complete (after receipt sent). Should contain payment method
     * (cash or card) and rest of payment details for card payment.
     */
    PIM_PAYMENT("PP"),
    /**
     * Command PIM app should send if a card is declined.  Should contain any available decline message.
     */
    PAYMENT_DECLINED("PD"),
    /**
     * Command PIM app should send when its status (which screen is being displayed) changes or after receiving a
     * STATUS_REQ packet.
     */
    PIM_STATUS("PS"),
    /**
     * Command Driver App can send to request the PIM's status.
     */
    STATUS_REQ("SR"),
    /**
     * Command Driver App can send to PIM to print something (may use in the future if we attach a printer to the PIM).
     */
    PRINT_RECEIPT("PR");
}