package com.example.nts_pim.utilities.bluetooth_helper

import android.util.Log
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
//    val isValidPacket: Boolean
//        get() {
//            if (_data == null) return false
//            try {
//                // First, try to convert the data to a string.
//                val s = String(_data!!)
//
//                // If an error wasn't thrown, convert the string to a JSON object.
//                val obj = JSONObject(s)
//                val strCmd = obj.getString(JSON_COMMAND)
//                val json = obj.optJSONObject(JSON_DATA)
//
//                // Check if the command is valid. If command expects data with it and it is null, then packet not valid.
//                if (strCmd == Command.ACK.command) {
//                    command = Command.ACK
//                } else if (strCmd == Command.NACK.command) {
//                    command = Command.NACK
//                } else if (strCmd == Command.MDT_STATUS.command) {
//                    command =
//                        Command.MDT_STATUS
//                    if (json == null) return false
//                    packetData = MdtStatusObj()
//                    (packetData as MdtStatusObj).fromJson(json)
//                } else if (strCmd == Command.PIM_PAYMENT.command) {
//                    command =
//                        Command.PIM_PAYMENT
//                    if (json == null) return false
//                    packetData = PimPaymentObj()
//                    (packetData as PimPaymentObj).fromJson(json)
//                } else if (strCmd == Command.PAYMENT_DECLINED.command) {
//                    command =
//                        Command.PAYMENT_DECLINED
//                    if (json == null) return false
//                    packetData = PaymentDeclinedObj()
//                    (packetData as PaymentDeclinedObj).fromJson(json)
//                } else if (strCmd == Command.PIM_STATUS.command) {
//                    command =
//                        Command.PIM_STATUS
//                    if (json == null) return false
//                    packetData = PimStatusObj()
//                    (packetData as PimStatusObj).fromJson(json)
//                } else if (strCmd == Command.STATUS_REQ.command) {
//                    command =
//                        Command.STATUS_REQ
//                } else {
//                    // Command not recognized, exit.
//                    return false
//                }
//
//                // If this point is reached, then data was converted to JSON and going to assume packet is valid.
//                return true
//            } catch (e: Exception) {
//                Log.i("bluetooth", "Error:" + e.message)
//            }
//            return false
//        }

    /**
     * Parses the bytes passed in. The byte array may only contain a portion of the packet so keep calling parseData()
     * (on the same class instance) until true is returned.  If true is returned, check variable "LeftoverIndex" to see
     * if it is greater than 0 indicating that the data contains part (or all) of another packet and that parsing should
     * start again at this index with a new instance of this class.
     *
     * @param data Bytes read from socket connection.
     * @return True if the end of the packet was found.
     */
    fun parseData(data: ByteArray): Boolean {
        var b: Byte
        var i = 0
        val len = data.size
        while (i < len) {
            b = data[i]

            // If this byte is for the start of a packet and we are not currently looking for the start, reset parsing and
            // start over from here.
            if (b.toInt() == STX && _readState != ReadState.STX) {
                init()
            }
            // To make sure our buffer doesn't keep growing forever if the end byte is not found, going to assume we need
            // to start over if we've parsed 10k bytes.
            if (_readState == ReadState.DATA && _index > 10000) {
                init()
            }
            when (_readState) {
                ReadState.STX -> if (b.toInt() == STX) {
                    _readState = ReadState.DATA
                }
                ReadState.DATA ->                     // Check for end byte.
                    if (b.toInt() == ETX) {
                        // If there are any more bytes to parse, set leftover index.
                        if (i < len - 1) LeftoverIndex = i + 1

                        // Copy buffer to data array.
                        _data = ByteArray(_index)
                        System.arraycopy(_readBuffer!!, 0, _data, 0, _index)

                        // Done parsing, return true.
                        return true
                    } else {
                        // Make sure buffer limit hasn't been reached.
                        if (_index == _readBuffer!!.size) allocateBuffer()
                        _readBuffer!![_index] = b
                        _index++
                    }
            }
            i++
        }
        return false
    }

    fun toBytes(): ByteArray? {
        var packet: ByteArray? = null
        val json: JSONObject

        // First, create a JSON object that contains the command.
        try {
            json = JSONObject()
            json.put(JSON_COMMAND, command!!.command)

            // If this packet has additional data, add it as a JSON object.
            if (packetData != null) {
                json.put(JSON_DATA, packetData!!.toJson())
            }

            // Convert JSON to string and then bytes and add to byte araay plus 2 bytes for STX and ETX.
            _data = json.toString().toByteArray()
            packet = ByteArray(_data!!.size + 2)
            packet[0] = STX.toByte()
            System.arraycopy(_data!!, 0, packet, 1, _data!!.size)
            packet[1 + _data!!.size] = ETX.toByte()
        } catch (e: Exception) {
        }
        return packet
    }

    /**
     * Call to instantiate the buffer or increase its size.
     */
    private fun allocateBuffer() {
        val tempBuffer: ByteArray
        if (_readBuffer == null) {
            _readBuffer = ByteArray(1024)
        } else {
            // Double the buffer's size.
            tempBuffer = ByteArray(_readBuffer!!.size * 2)
            System.arraycopy(_readBuffer!!, 0, tempBuffer, 0, _readBuffer!!.size)
            _readBuffer = tempBuffer
        }
    }

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
            driverId = obj.optInt(JSON_DRIVER_ID)
            tripNbr = obj.optInt(JSON_TRIP_NBR)
            meterState = obj.optString(JSON_METER_STATE)
            pimNoReceipt = obj.optString(JSON_PIM_NO_RECEIPT)
            tripId = obj.optString(JSON_TRIP_ID)
            tripStatus = obj.optString(JSON_TRIP_STATUS)
            owedPrice = obj.optDouble(JSON_OWED_PRICE, 0.0)
            pimPayAmt = obj.optDouble(JSON_PIM_PAY_AMT, 0.0)
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

    } // end of class MdtStatusObj

    /**
     * Class to help send or parse JSON data for PIM Payment packet.  Sent by PIM after a card or cash payment.
     * Trip ID should be included to make sure the payment is recorded for the correct trip (like in the case
     * where a trip is closed during payment process).
     */
    class PimPaymentObj : PimDataObj {
        var pimPaidAmt: Double? = null
        var tipAmt: Double? = null
        var cardInfo: String? = null
        var payType: String? = null
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
    class PaymentDeclinedObj : PimDataObj {
        var declineMsg: String? = null
        override fun fromJson(obj: JSONObject) {
            declineMsg = obj.optString(JSON_DECLINE_MSG)
        }

        override fun toJson(): JSONObject {
            val obj = JSONObject()
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
        override fun fromJson(obj: JSONObject) {
            pimStatus = obj.optString(JSON_PIM_STATUS)
        }

        override fun toJson(): JSONObject {
            val obj = JSONObject()
            try {
                if (pimStatus != null) obj.put(JSON_PIM_STATUS, pimStatus)
            } catch (ignore: Exception) {
            }
            return obj
        }

        companion object {
            private const val JSON_PIM_STATUS = "pimStatus"
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
            if (data == null || data.size == 0) return false

            // Look for STX.
            for (b in data) {
                if (b.toInt() == STX) return true
            }
            return false
        }
    }
}