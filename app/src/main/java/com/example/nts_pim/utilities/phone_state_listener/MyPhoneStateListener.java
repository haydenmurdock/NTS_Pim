
package com.example.nts_pim.utilities.phone_state_listener;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

/**
 * Class to keep track of data network info (connection status, signal strength, etc.).
 *
 * @author Chad DeWitt
 * @since 6/14/2016
 */
public class MyPhoneStateListener extends PhoneStateListener
{
//    public static final int LISTEN_EVENTS = LISTEN_DATA_CONNECTION_STATE | LISTEN_SIGNAL_STRENGTHS;
//    private static MyPhoneStateListener _instance;​
//    private TelephonyManager _manager;
//    private int _dataConnectionState, _gsmErrorRate, _gsmSignalStrength;
//    private boolean _bIsGSM;
//    private boolean _bRelayNotification;
//​
//    /**
//     * Returns a static instance of this class. If passing in null for TelephonyManager and a static
//     * instance hasn't been created yet, null will be returned.
//     *
//     * @param tm Instance of TelephonyManager or null (WatchdogService will pass in the TelephonyManager).
//     * @return Static instance of this class or null if not able to create an instance.
//     */
//    public static MyPhoneStateListener getInstance(TelephonyManager tm)
//    {
//        if (_instance == null && tm != null) {
//            _instance = new MyPhoneStateListener();
//            _instance._manager = tm;
//        }
//​
//        // If TelephonyManger is passed in, set flag for whether notifications should be relayed or not.
//        if (tm != null) {
////            if (AppModel.getInstance().getSettings().getLogging() == Logging.All_Logging_Signal_Str)
////                _instance._bRelayNotification = true;
////            else
////                _instance._bRelayNotification = false;
//        }
//​
//        return _instance;
//    }
//​
//    /**
//     * If an instance of this class has been created, the data connection status will be returned. Otherwise, true is
//     * returned.
//     *
//     * @return True if data is connected or an instance if this class hasn't been created so connection status is not
//     * known.
//     */
//    public static boolean checkDataConnected()
//    {
//        return _instance == null || _instance.isDataConnected();
//    }
//​
//    @Override
//    public void onDataConnectionStateChanged(int state)
//    {
//        _dataConnectionState = state;
//        StringBuilder sb = new StringBuilder();
//        sb.append("+++ Data Connection State = ");
//        sb.append(dataConnectionStateToString());
//        sb.append(", Network Type = ");
//        sb.append(getNetworkType());
//        if(_bRelayNotification){
//
//        }
//        //relay status update
//    }
//​
//    @Override
//    public void onSignalStrengthsChanged(SignalStrength ss)
//    {
//        _gsmErrorRate = ss.getGsmBitErrorRate();
//        _gsmSignalStrength = ss.getGsmSignalStrength();
//        _bIsGSM = ss.isGsm();
//        // logSignalStrength();
//        if(_bRelayNotification){
//
//        }
//          //relay status update
//    }
//​
//    public String dataConnectionStateToString()
//    {
//        String state;
//​
//        switch (_dataConnectionState) {
//            case TelephonyManager.DATA_DISCONNECTED:
//                state = "DISCONNECTED";
//                break;
//            case TelephonyManager.DATA_CONNECTING:
//                state = "CONNECTING";
//                break;
//            case TelephonyManager.DATA_CONNECTED:
//                state = "CONNECTED";
//                break;
//            case TelephonyManager.DATA_SUSPENDED:
//                state = "SUSPENDED";
//                break;
//            default:
//                state = "UNKNOWN";
//                break;
//        }
//​
//        return state;
//    }
//​
//    public String getNetworkType()
//    {
//        String type = "";
//        int i;
//​
//        try {
//            if (_manager == null)
//                i = 0;
//            else
//                i = _manager.getNetworkType();
//            switch (i) {
//                case TelephonyManager.NETWORK_TYPE_1xRTT:
//                    type = "1xRTT";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_CDMA:
//                    type = "CDMA";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_EDGE:
//                    type = "EDGE";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_EHRPD:
//                    type = "EHRPD";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_EVDO_0:
//                    type = "EVDO_0";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_EVDO_A:
//                    type = "EVDO_A";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_EVDO_B:
//                    type = "EVDO_B";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_GPRS:
//                    type = "GPRS";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_HSDPA:
//                    type = "HSDPA";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_HSPA:
//                    type = "HSPA";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_HSUPA:
//                    type = "HSUPA";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_IDEN:
//                    type = "IDEN";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_LTE:
//                    type = "LTE";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_UMTS:
//                    type = "UMTS";
//                    break;
//                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
//                    type = "UNKNOWN";
//                    break;
//            }
//        }
//        catch (Exception e) {
//
//        }
//​
//        return type;
//    }
//​
//    public int getSignalStrength()
//    {
//        if (_gsmSignalStrength == 99)
//            return _gsmSignalStrength;
//​
//        return _gsmSignalStrength * 2 - 113;
//    }
//​
//    public boolean isDataConnected()
//    {
//        return _dataConnectionState == TelephonyManager.DATA_CONNECTED;
//    }
//​
//    public void logSignalStrength()
//    {
//        StringBuilder sb = new StringBuilder();
//        sb.append("+++ ");
//        // sb.append("CDMA Dbm=");
//        // sb.append(ss.getCdmaDbm());
//        // sb.append(", CDMA Ecio=");
//        // sb.append(ss.getCdmaEcio());
//        // sb.append(", EVDO Dbm=");
//        // sb.append(ss.getEvdoDbm());
//        // sb.append(", EVDO Ecio=");
//        // sb.append(ss.getEvdoEcio());
//        // sb.append(", EVDO SNR=");
//        // sb.append(ss.getEvdoSnr());
//        sb.append("GSM ER=");
//        sb.append(_gsmErrorRate);
//        sb.append(", GSM SS=");
//        if (_gsmSignalStrength == 99) {
//            sb.append("N/A");
//        }
//        else {
//            sb.append(getSignalStrength());
//        }
//        sb.append(", GSM=");
//        sb.append(_bIsGSM);
//​
//    }
} // end of class