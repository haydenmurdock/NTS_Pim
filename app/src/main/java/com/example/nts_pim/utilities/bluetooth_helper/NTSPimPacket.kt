package com.example.nts_pim.utilities.bluetooth_helper

import com.example.nts_pim.PimApplication
import com.example.nts_pim.data.repository.TripDetails
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.trip.Trip
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
        ACK("ACK"),
        NACK("NACK"),
        MDT_STATUS("MDT_STATUS"),
        PIM_PAYMENT("PIM_PAYMENT"),
        PAYMENT_DECLINED("PAYMENT_DECLINED"),
        PIM_STATUS("PIM_STATUS"),
        STATUS_REQ("STATUS_REQ"),
        GET_UPFRONT_PRICE("GET_UPFRONT_PRICE"),
        UPFRONT_PRICE("UPFRONT_PRICE"),
        UPDATE_TRIP("UPDATE_TRIP")
    }
    private enum class ReadState {STX}
    private var _readState: ReadState? = null
    var command: Command?
        private set
    var packetData: PimDataObj?
        private set
    private var _index = 0
    private var _data: ByteArray? = null
    private var _readBuffer: ByteArray? = null

    constructor() {
        init()
        command = null
        packetData = null
    }
    constructor(
        cmd: Command?,
        data: PimDataObj?
    ) {
        init()
        command = cmd
        packetData = data
    }
    private fun init() {
        _readState = ReadState.STX
        _index = 0
        _data = null
        _readBuffer = null
    }
    interface PimDataObj {
        fun fromJson(obj: JSONObject)
        fun toJson(): JSONObject
    }

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

