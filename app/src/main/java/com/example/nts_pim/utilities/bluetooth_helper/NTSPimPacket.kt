package com.example.nts_pim.utilities.bluetooth_helper

import android.util.Log
import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.Trip
import com.example.nts_pim.receivers.BatteryPowerReceiver
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import org.json.JSONObject

// Right now, all of the commands are 2 characters long.  If this changes, change this constant.
/**
* Helper class for sending and receiving packets between Driver app and NTS PIM app. Format:
* <pre>
*   SDDD...E
*   S = start byte (STX := 0x02)
*   D = variable number of data bytes.  This portion should be JSON formatted.
*   E = end byte (ETX := 0x03)
* </pre>
*/

class NTSPimPacket {
    /**
     * Indicates the type of information that should be found in this packet's data (if there is any beyond command).
     */
    enum class Command(val command: String) {
        /**
         * Command to return after receiving a valid packet.
         */
        ACK("ACK"),

        /**
         * Command to return if a packet wasn't valid.  When received, the packet should be resent at least 1 more time
         * (except for ACK or NACK packets).
         */
        NACK("NACK"),

        /**
         * Command containing data for the current status of the Driver App (such as vehicle status, trip info, and meter
         * status).
         */
        MDT_STATUS("MDT_STATUS"),  //    /**
        //     * Command to start payment process on PIM. Should contain all the data needed to start payment.
        //     */
        //    START_PAYMENT("START_PAYMENT"),
        //    /**
        //     * Command Driver App can send if a payment should be canceled (may not end up using this command).
        //     */
        //    CANCEL_PAYMENT("CANCEL_PAYMENT"),
        /**
         * Command PIM App should send when a payment is complete (after receipt sent). Should contain payment method
         * (cash or card) and rest of payment details for card payment.
         */
        PIM_PAYMENT("PIM_PAYMENT"),

        /**
         * Command PIM app should send if a card is declined.  Should contain any available decline message.
         */
        PAYMENT_DECLINED("PAYMENT_DECLINED"),

        /**
         * Command PIM app should send when its status (which screen is being displayed) changes or after receiving a
         * STATUS_REQ packet.
         */
        PIM_STATUS("PIM_STATUS"),

        /**
         * Command Driver App or PIM App can send to request the other's status. Will not include any JSON data.
         */
        STATUS_REQ("STATUS_REQ");

    }

    /**
     * This variable is set if there are more bytes to parse after finding the end of a packet.
     */
    var LeftoverIndex = 0

    private enum class ReadState {
        STX, DATA
    }

    private var _readState: ReadState? = null

    /**
     * Gets the command for this packet. For packets being parsed, this won't be set until after [.isValidPacket]
     * is called.
     *
     * @return The packet's command.
     */
    var command: Command?
        private set

    /**
     * Gets the data from the packet converted to a JSON object.  If null, packet doesn't have any data. For packets
     * being parsed, this won't be set until after [.isValidPacket] is called.
     *
     * @return PimDataObj containing packet data or null if there is no data.
     */
    var packetData: PimDataObj?
        private set
    private var _index = 0
    private var _data: ByteArray? = null
    private var _readBuffer: ByteArray? = null

    /**
     * Constructor to use when getting ready to parse data.
     */
    constructor() {
        init()
        command = null
        packetData = null
    }

    /**
     * Constructor to use when getting ready to send a packet.
     *
     * @param cmd  Command indicating what sort of data the packet will contain.
     * @param data PimDataObj containing packet data or null if no data should be sent.
     */
    constructor(
        cmd: Command?,
        data: PimDataObj?
    ) {
        init()
        command = cmd
        packetData = data
    }

    val isAckPacket: Boolean
        get() = command == Command.ACK// Command not recognized, exit.

    // If this point is reached, then data was converted to JSON and going to assume packet is valid.
// First, try to convert the data to a string.

    // If an error wasn't thrown, convert the string to a JSON object.

    // Check if the command is valid. If command expects data with it and it is null, then packet not valid.

    /**
     * Call this function after parseData() returns true to check if the data just parsed is valid.
     *
     * @return True if packet contains valid data.
     */

    /**
     * Parses the bytes passed in. The byte array may only contain a portion of the packet so keep calling parseData()
     * (on the same class instance) until true is returned.  If true is returned, check variable "LeftoverIndex" to see
     * if it is greater than 0 indicating that the data contains part (or all) of another packet and that parsing should
     * start again at this index with a new instance of this class.
     *
     * @param data Bytes read from socket connection.
     * @return True if the end of the packet was found.
     */

    /**
     * Call to instantiate the buffer or increase its size.
     */


    /**
     * Set variables to initial state ready to start looking for the beginning of a packet.
     */
    private fun init() {
        _readState = ReadState.STX
        _index = 0
        _data = null
        _readBuffer = null
    }

    // *************************
    // ****  Inner Classes  ****
    // *************************
    interface PimDataObj {
        fun fromJson(obj: JSONObject)
        fun toJson(): JSONObject
    }

    /**
     * Class to help send or parse JSON data for MDT Status packet.  If not on a trip or the trip hasn't been
     * picked up yet, some of the variables will be left null which, when parsed, will be set to default values
     * of an empty string or 0.  Sent by Driver app when status changes or in response to a Status Request.
     */
    class MdtStatusObj(mId: Int?, mTripNbr: Int?, mMeterState: String?, mPimNoReceipt: String?, mTripId: String?, mTripStatus: String?, mOwedPrice: Double?, mPimPayAmt: Double?) : PimDataObj {
        var driverId: Int? = null
        var tripNbr: Int? = null
        var meterState: String? = null
        var pimNoReceipt: String? = null
        var tripId: String? = null
        var tripStatus: String? = null
        var owedPrice: Double? = null
        var pimPayAmt: Double? = null
        init {
            driverId = mId
            tripNbr = mTripNbr
            meterState = mMeterState
            pimNoReceipt = mPimNoReceipt
            tripId = mTripId
            tripStatus = mTripStatus
            owedPrice = mOwedPrice
            pimPayAmt = mPimPayAmt
        }

        override fun fromJson(obj: JSONObject) {
            tripId = obj.optString(JSON_TRIP_ID)
            pimPayAmt = obj.optDouble(JSON_PIM_PAY_AMT)
            tripNbr = obj.optInt(JSON_TRIP_NBR)
            pimNoReceipt = obj.optString(JSON_PIM_NO_RECEIPT)
            tripStatus = obj.optString(JSON_TRIP_STATUS)
            owedPrice = obj.optDouble(JSON_OWED_PRICE)
            driverId = obj.optInt(JSON_DRIVER_ID)
            meterState = obj.optString(JSON_METER_STATE)
            LoggerHelper.writeToLog("Parsed packet from Json: tripId: $tripId, pimPayAmt: $pimPayAmt,tripNum: $tripNbr, pimNoReceipt: $pimNoReceipt, tripStatus: $tripStatus, owedPrice: $owedPrice, driverId: $driverId, meterState: $meterState", LogEnums.BLUETOOTH.tag)
            if(!tripId.isNullOrBlank() || !tripId.isNullOrEmpty()){
                val thisForTheCurrentTrip = TripDetails.isThisForAnOldTrip(tripId!!)
                if(!thisForTheCurrentTrip){
                    VehicleTripArrayHolder.addTripId(tripId!!, PimApplication.pimContext)
                } else {
                    LoggerHelper.writeToLog("Packet received for $tripId, but was found to be a completed trip. This packet is being ignored", LogEnums.TRIP_STATUS.tag)
                    return
                }
            }
            if(pimPayAmt != null && !pimPayAmt!!.isNaN()){
                VehicleTripArrayHolder.setPimPayment(pimPayAmt!!)
            } else {
                LoggerHelper.writeToLog("pim payamount: $pimPayAmt, it was ignored", LogEnums.BLUETOOTH.tag)
            }

            if(tripNbr != null){
                VehicleTripArrayHolder.addTripNumber(tripNbr!!)
            }

            if(pimNoReceipt != null && pimNoReceipt == "Y"){
                VehicleTripArrayHolder.pimDoesNotNeedToDoReceipt(true)
            }

            if(tripStatus != null){
                VehicleTripArrayHolder.addStatus(tripStatus!!)
            }
            if(owedPrice != null){}
            if(driverId != null){
                VehicleTripArrayHolder.setDriverId(driverId!!)
            }
            if(meterState != null){
                VehicleTripArrayHolder.addMeterState(meterState!!)
            }
        }

        override fun toJson(): JSONObject {
            val obj = JSONObject()
            try {
                if (driverId != null) {obj.put(JSON_DRIVER_ID, driverId!!)}
                if (meterState != null) {obj.put(JSON_METER_STATE, meterState!!)}
                if (pimNoReceipt != null) obj.put(
                    JSON_PIM_NO_RECEIPT,
                    pimNoReceipt!!
                )
                if (tripNbr != null) {obj.put(JSON_TRIP_NBR, tripNbr!!)}
                if (tripId != null) {obj.put(JSON_TRIP_ID, tripId!!)}
                if (tripStatus != null) {obj.put(JSON_TRIP_STATUS, tripStatus!!)}
                if (owedPrice != null) {obj.put(JSON_OWED_PRICE, owedPrice!!)}
                if (pimPayAmt != null) {obj.put(JSON_PIM_PAY_AMT, pimPayAmt!!)}
            } catch (ignore: Exception) {
            }
            return obj
        }

        companion object {
            private const val JSON_DRIVER_ID = "driverId"
            private const val JSON_TRIP_NBR = "tripNbr"
            private const val JSON_METER_STATE = "meterState"
            private const val JSON_PIM_NO_RECEIPT = "pimNoReceipt"
            private const val JSON_TRIP_ID = "tripId"
            private const val JSON_TRIP_STATUS = "tripStatus"
            private const val JSON_OWED_PRICE = "owedPrice"
            private const val JSON_PIM_PAY_AMT = "pimPayAmt"
        }
    }

    /**
     * Class to help send or parse JSON data for PIM Payment packet.  Sent by PIM after a card or cash payment.
     * Trip ID should be included to make sure the payment is recorded for the correct trip (like in the case
     * where a trip is closed during payment process).
     */
    class PimPaymentObj : PimDataObj {
        var pimPaidAmt: Double? = null
        var tipAmt: Double? = null
        var cardInfo: String? = null
       private var payType: String? = null
        var transDate: String? = null
        var transId: String? = null
        var tripId: String? = null
        override fun fromJson(obj: JSONObject) {
            pimPaidAmt = obj.optDouble(JSON_PAID_AMT, 0.0)
            tipAmt = obj.optDouble(JSON_TIP_AMT, 0.0)
            cardInfo = obj.optString(JSON_CARD_INFO)
            payType = obj.optString(JSON_PAY_TYPE)
            transDate = obj.optString(JSON_TRANS_DATE)
            transId = obj.optString(JSON_TRANS_ID)
            tripId = obj.optString(JSON_TRIP_ID)
        }

        override fun toJson(): JSONObject {
            val obj = JSONObject()
            pimPaidAmt = VehicleTripArrayHolder.getPimPaidAmount()
            tipAmt = VehicleTripArrayHolder.getTipAmount()
            cardInfo = VehicleTripArrayHolder.getCardInfo()
            transDate = VehicleTripArrayHolder.getTransDate()
            payType = VehicleTripArrayHolder.paymentTypeSelected
            transId = VehicleTripArrayHolder.getTransactionID()
            tripId = VehicleTripArrayHolder.getTripId()
            try {
                if (pimPaidAmt != null) {obj.put(JSON_PAID_AMT, pimPaidAmt!!)}
                if (tipAmt != null) {obj.put(JSON_TIP_AMT, tipAmt!!)}
                if (cardInfo != null) {obj.put(JSON_CARD_INFO, cardInfo!!)}
                if (payType != null) {obj.put(JSON_PAY_TYPE, payType!!)}
                if (transDate != null) {obj.put(JSON_TRANS_DATE, transDate!!)}
                if (transId != null) {obj.put(JSON_TRANS_ID, transId!!)}
                if (tripId != null) {obj.put(JSON_TRIP_ID, tripId!!)}
            } catch (ignore: Exception) {
            }
            return obj
        }

        companion object {
            private const val JSON_PAID_AMT = "pimPaidAmt"
            private const val JSON_TIP_AMT = "tipAmt"
            private const val JSON_CARD_INFO = "cardInfo"
            private const val JSON_PAY_TYPE = "payType"
            private const val JSON_TRANS_DATE = "transDate"
            private const val JSON_TRANS_ID = "transId"
            private const val JSON_TRIP_ID = "tripId"
        }

    } // end of class PimPaymentObj

    /**
     * Class to help send or parse JSON data for Payment Declined packet.  Sent by PIM so driver can see the same
     * error message as the passenger if a payment is declined.
     */
    class PaymentDeclinedObj() : PimDataObj {
        private var declineMsg: String? = null
        override fun fromJson(obj: JSONObject) {
            declineMsg = obj.optString(JSON_DECLINE_MSG)
        }

        override fun toJson(): JSONObject {
            val obj = JSONObject()
            declineMsg = VehicleTripArrayHolder.getDeclinedCardMessage()
            try {
                if (declineMsg != null) obj.put(
                    JSON_DECLINE_MSG,
                    declineMsg
                )
            } catch (ignore: Exception) {
            }
            return obj
        }

        companion object {
            private const val JSON_DECLINE_MSG = "declineMsg"
        }

    } // end of class PaymentDeclinedObj

    /**
     * Class to help send or parse JSON data for PIM Status packet.  Sent by PIM when its status changes or in
     * response to a Status Request.
     */

    class TripObj: PimDataObj{
        val trip: Trip? = null

        override fun fromJson(obj: JSONObject) {
            trip?.pickupDest = obj.optString(JSON_PICKUP_DESTINATION)

        }

        override fun toJson(): JSONObject {
            val obj = JSONObject()
            return obj
        }
        companion object {
            private const val JSON_PICKUP_DESTINATION = "pickupDest"
            private const val JSON_DROP_OFF_DESTINATION = "dropOffDest"
            private const val JSON_FIRST_NAME = "firstName"
            private const val JSON_LAST_NAME = "lastName"
            private const val JSON_TRIP_PRICE = "tripPrice"
            private const val JSON_DRIVER_ACCEPTED = "driverAccepted"
        }

    }
    class PimStatusObj : PimDataObj {
        var pimStatus: String? = null
        var tripId: String? = null
        var pimOverHeat: Boolean? = null

        override fun fromJson(obj: JSONObject) {
            tripId = obj.optString(JSON_TRIP_ID)
            pimStatus = obj.optString(JSON_PIM_STATUS)
            pimOverHeat = obj.optBoolean(JSON_OVER_HEAT)
        }

        override fun toJson(): JSONObject {
            val obj = JSONObject()
            pimStatus = VehicleTripArrayHolder.getInternalPIMStatus()
            tripId = VehicleTripArrayHolder.getTripId()
            pimOverHeat = BatteryPowerReceiver.overHeating
            try {
                if (pimStatus != null){
                    obj.put(JSON_PIM_STATUS, pimStatus)
                }
                if(tripId != null && tripId != ""){
                    obj.put(JSON_TRIP_ID, tripId)
                }
                if(pimOverHeat != null){
                    obj.put(JSON_OVER_HEAT, pimOverHeat!!)
                }
            } catch (ignore: Exception) {
            }
            return obj
        }

        companion object {
            private const val JSON_PIM_STATUS = "pimStatus"
            private const val JSON_TRIP_ID = "tripId"
            private const val JSON_OVER_HEAT = "pimOverheat"
        }

    } // end of class PimStatusObj

    companion object {
        private const val JSON_COMMAND = "Command"
        private const val JSON_DATA = "Data"
        private const val STX = 0x02
        private const val ETX = 0x03

        /**
         * Checks to see if any of the bytes passed in is the packet start byte.
         *
         * @param data Array of bytes.
         * @return True if start byte was found.
         */
        fun containsPacketStart(data: ByteArray?): Boolean {
            if (data == null || data.isEmpty()){
                LoggerHelper.writeToLog("Received a packet, but data was null or empty", LogEnums.BLUETOOTH.tag)
                return false
            }
            // Look for STX.
            for (b in data) {
                if (b.toInt() == STX) {
                    return true
                }
            }
            LoggerHelper.writeToLog("Received a packet, but didn't contain start byte", LogEnums.BLUETOOTH.tag)
            return false
        }
    }
}

