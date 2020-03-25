package com.example.nts_pim.utilities.bluetooth_helper;

import android.util.Log;

import org.json.JSONObject;

import javax.annotation.Signed;

import kotlin.Pair;
import kotlin.reflect.jvm.internal.impl.util.Checks;


// Right now, all of the commands are 2 characters long.  If this changes, change this constant.
public class ntsPimPacket
{
    // Right now, all of the commands are 2 characters long.  If this changes, change this constant.
    private static final int CMD_BYTE_LENGTH = 2;

    /**
     * Indicates the type of information that should be found in this packet. The strings that represent the command
     * should all have the same number of characters.
     */
    public enum Command
    {
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

        private final String command;

        Command(String cmd)
        {
            command = cmd;
        }
    }

    /**
     * This variable is set if there are more bytes to parse after finding the end of a packet.
     */
    public int LeftoverIndex;

    private enum ReadState
    {
        STX, CMD, LENGTH, DATA, ETX
    }

    private static final int STX = 0x02;
    private static final int ETX = 0x03;

    private ReadState _readState;
    private Command _cmd;
    private String _strCmd;
    private int _bytesRead, _dataLen;
    private byte[] _data;
    private JSONObject _jsonData;

    /**
     * Constructor to use when getting ready to parse data.
     */
    public ntsPimPacket()
    {
        _readState = ReadState.STX;
        _cmd = null;
        _strCmd = "";
        _bytesRead = 0;
        _dataLen = 0;
        _data = null;
        _jsonData = null;
    }

    /**
     * Constructor to use when getting ready to send a packet.
     *
     * @param cmd  Command indicating what sort of data the packet will contain.
     * @param data JSONObject containing packet data or null if no data should be sent.
     */
    public ntsPimPacket(Command cmd, JSONObject data)
    {
        _cmd = cmd;

        if (data == null) {
            _dataLen = 0;
            _data = null;
            _jsonData = null;
        }
        else {
            _jsonData = data;
            String s = _jsonData.toString();
            _data = s.getBytes();
            _dataLen = _data.length;
        }
    }

    /**
     * Checks to see if any of the bytes passed in is the packet start byte.
     *
     * @param data Array of bytes.
     * @return True if start byte was found.
     */
    public static boolean containsPacketStart(byte[] data)
    {
        if (data == null || data.length == 0) return false;

        // Look for STX.
        for (byte b : data) {
            if (b == STX)
                return true;
        }

        return false;
    }

    /**
     * Gets the data from the packet converted to a JSON object.  If null, packet doesn't have any data.
     *
     * @return JSONObject containing packet data or null if there is no data.
     */
    public JSONObject getPacketData()
    {
        return _jsonData;
    }

    /**
     * Call this function after parseData() returns true to check if the data just parsed is valid.
     *
     * @return True if packet contains valid data.
     */
    public boolean isValidPacket()
    {
        // Check if the command is valid.
        if (_strCmd.equals(Command.ACK.command)) {
            _cmd = Command.ACK;
        }
        else if (_strCmd.equals(Command.NACK.command)) {
            _cmd = Command.NACK;
        }
        else if (_strCmd.equals(Command.MDT_STATUS.command)) {
            _cmd = Command.MDT_STATUS;
        }
        else if (_strCmd.equals(Command.START_PAYMENT.command)) {
            _cmd = Command.START_PAYMENT;
        }
        else if (_strCmd.equals(Command.CANCEL_PAYMENT.command)) {
            _cmd = Command.CANCEL_PAYMENT;
        }
        else if (_strCmd.equals(Command.PIM_PAYMENT.command)) {
            _cmd = Command.PIM_PAYMENT;
        }
        else if (_strCmd.equals(Command.PAYMENT_DECLINED.command)) {
            _cmd = Command.PAYMENT_DECLINED;
        }
        else if (_strCmd.equals(Command.PIM_STATUS.command)) {
            _cmd = Command.PIM_STATUS;
        }
        else if (_strCmd.equals(Command.PRINT_RECEIPT.command)) {
            _cmd = Command.PRINT_RECEIPT;
        }
        else if (_strCmd.equals(Command.STATUS_REQ.command)) {
            _cmd = Command.NACK;
        }
        else {
            // Command not recognized, exit.
           // return false;
        }

      if (_data == null) return true;

        try {
            // First, try to convert the data to a string.
            String s = new String(_data);

            // If an error wasn't thrown, convert the string to a JSON object.
            _jsonData = new JSONObject(s);

            // If this point is reached, then data was converted to JSON and going to assume packet is valid.
            return true;
        }
        catch (Exception e) {
            Log.e(e.toString(), "NtsPimPacket.isValidPacket");
        }

        return false;
    }

    /**
     * Parses the bytes passed in. The byte array may only contain a portion of the packet so keep calling parseData()
     * (on the same class instance) until true is returned.  If true is returned, check variable "LeftoverIndex" to see
     * if it is greater than 0 indicating that the data contains part (or all) of another packet and that parsing should
     * start again at this index.
     *
     * @param data Bytes read from socket connection.
     * @return True if the end of the packet was found.
     */
    public boolean parseData(byte[] data)
    {
        byte b;
        char c;

        //We can just use the buffer.

        for (int i = 0, len = data.length; i < len; i++) {
            b = data[i];

            switch (_readState) {
                case STX:
                    if (b == STX) _readState = ReadState.CMD;
                    break;
                case CMD:
                    c = (char) b;
                    _strCmd += c;
                    _bytesRead++;
                    if (_bytesRead == CMD_BYTE_LENGTH) {
                        _readState = ReadState.LENGTH;
                        _bytesRead = 0;
                        // Use data array to parse length.
                        _data = new byte[2];
                    }
                    break;
                case LENGTH:
                    _data[_bytesRead] = b;
                    _bytesRead++;
                    if (_bytesRead == 2) {
                        // Reset bytes read to start keeping track of data bytes.
                        _bytesRead = 0;
                        // Convert length bytes to integer.  The first byte is the most significant byte (MSB) and the second is
                        // the least significant byte (LSB).  Shift the MSB by 8 bits and do a bitwise OR with LSB.  Bytes, when
                        // cast to an integer, can be negative so do a bitwise AND with 0xFF to make them positive.
                        _dataLen = ((_data[0] & 0xFF) << 8) | (_data[1] & 0xFF);
                        if (_dataLen > 0) {
                            _readState = ReadState.DATA;
                            _data = new byte[_dataLen];
                        }
                        else {
                            _readState = ReadState.ETX;
                            _data = null;
                        }
                    }
                    break;
                case DATA:
                    _data[_bytesRead] = b;
                    _bytesRead++;
                    if (_bytesRead == _dataLen) {
                        _readState = ReadState.ETX;
                    }
                    break;
                case ETX:
                    if (b == ETX) {
                        // If there are any more bytes to parse, set leftover index.
                        if (i < len - 1)
                            LeftoverIndex = i + 1;

                        // Done parsing, return true.
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    public byte[] toBytes()
    {
        byte[] packet, cmd;
        int i;

        // First, figure out how many bytes are in the command.  Even though right now all of the commands are 2 characters
        // long, this may change in the future so going to leave it variable.
        cmd = _cmd.command.getBytes();

        // Create a byte array for the packet to be sent:
        // - 1 byte for STX
        // - 2 bytes for data length
        // - 1 byte for ETX
        // - plus however many data bytes there are.
        packet = new byte[4 + cmd.length + _dataLen];
        packet[0] = STX;

        // Insert command bytes in to packet array.
        System.arraycopy(cmd, 0, packet, 1, cmd.length);
        i = 1 + cmd.length;

        // Split integer up in to two bytes with the most significant byte first (also know as "big endian" order).
        packet[i] = intToByte(_dataLen >> 8);
        packet[i + 1] = intToByte(_dataLen);
        i += 2;

        // Write data bytes. These should already have been set with the constructor.
        if (_dataLen > 0) {
            System.arraycopy(_data, 0, packet, i, _dataLen);
            i += _dataLen;
        }

        packet[i] = ETX;

        return packet;
    }

    /**
     * Converts the integer's lowest order byte in to a signed byte value.
     *
     * @param i Integer to convert to byte.
     * @return Signed byte.
     */
    private byte intToByte(int i)
    {
        int iTemp = i & 0xFF;

        if (iTemp > 127)
            return (byte) (iTemp - 256);
        return (byte) iTemp;
    }
}
