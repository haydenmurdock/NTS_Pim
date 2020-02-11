package com.example.nts_pim.fragments_viewmodel.vehicle_settings_detail

import android.Manifest
import android.app.AlertDialog
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.dialog_composer.PIMDialogComposer
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.core.CallbackReference
import com.squareup.sdk.reader.core.Result
import com.squareup.sdk.reader.core.ResultError
import com.squareup.sdk.reader.hardware.ReaderSettingsErrorCode
import kotlinx.android.synthetic.main.vehicle_settings_detail.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.data.repository.model_objects.*
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.squareup.sdk.reader.checkout.CheckoutParameters
import com.squareup.sdk.reader.checkout.CurrencyCode
import com.squareup.sdk.reader.checkout.Money


class VehicleSettingsDetailFragment: ScopedFragment(), KodeinAware {

    //kodein and viewModel/Factory
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSettingsDetailViewModelFactory by instance()
    private lateinit var callBackViewModel: CallBackViewModel
    private lateinit var viewModel: VehicleSettingsDetailViewModel
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private var readerSettingsCallbackRef: CallbackReference? = null
    private val currentFragmentId = R.id.vehicle_settings_detail_fragment
    private var vehicleId = ""
    private var tripID = ""
    private var imei = ""
    private val bluetoothDeviceAdapter = BluetoothAdapter.getDefaultAdapter()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.vehicle_settings_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = InjectorUtiles.provideCallBackModelFactory()
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(VehicleSettingsDetailViewModel::class.java)
        callBackViewModel = ViewModelProviders.of(this, factory)
            .get(CallBackViewModel::class.java)

        val readerManager = ReaderSdk.readerManager()
        readerSettingsCallbackRef =
            readerManager.addReaderSettingsActivityCallback(this::onReaderSettingsResult)

        val keyboardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()

        keyboardViewModel = ViewModelProviders.of(this, keyboardFactory)
            .get(SettingsKeyboardViewModel::class.java)
        vehicleId = viewModel.getVehicleID()

        tripID = ModelPreferences(context!!)
            .getObject(
                SharedPrefEnum.CURRENT_TRIP.key,
                CurrentTrip::class.java)?.tripID ?: ""

        val batteryStatus = callBackViewModel.batteryPowerStatePermission()
        activity_indicator_vehicle_detail.visibility = View.INVISIBLE
        val telephonyManager = activity!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if(context?.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            imei = telephonyManager.imei
        }
        updateUI(batteryStatus)
        check_bluetooth_btn.setOnClickListener {
            //** Scott **
            screenDisabled()
            activity_indicator_vehicle_detail.animate()
            activity_indicator_vehicle_detail.visibility = View.VISIBLE
           Log.d("VehicleSettingsDetailFragment", "BlueToothDeviceAdapter: State- ${bluetoothDeviceAdapter.state}, " +
                   "Enabled:  ${bluetoothDeviceAdapter.isEnabled}" +
                   "Address: ${bluetoothDeviceAdapter.address}" +
                   "Bonded Devices: ${bluetoothDeviceAdapter.bondedDevices}" +
                   "Scan Mode: ${bluetoothDeviceAdapter.scanMode}" +
                   "isDiscovering: ${bluetoothDeviceAdapter.isDiscovering}")
            readerManager.startReaderSettingsActivity(context!!)
        }

        setting_detail_back_btn.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.back_to_welcome_fragment)
        }
        exit_app_btn.setOnClickListener {
            PIMDialogComposer.exitApplication(activity!!)
        }
        power_Off_PIM_btn.setOnClickListener {
            showPowerOffDialog()
        }
        recent_trip_button.setOnClickListener {
            toRecentTrip()
        }
        vehicle_settings_unpair_button.setOnClickListener {
            showUnPairDialog()
        }

        square_test_image_view.setOnClickListener {
            startSquareFlow()
        }

        battery_btn.setOnClickListener {
            callBackViewModel.enableOrDisableBatteryPower()
            val batteryPermission = callBackViewModel.batteryPowerStatePermission()
            Toast.makeText(
                context!!,
                "Allow Battery Power: $batteryPermission", Toast.LENGTH_LONG
            ).show()
            updatePowerButtonUI(batteryPermission)
        }
    }

    private fun startSquareFlow(){
        callBackViewModel.setAmountForSquareDisplay(1.00)
        val p = 100.00
        val checkOutTotal = p.toLong()
        val amountMoney = Money(checkOutTotal, CurrencyCode.current())
        val parametersBuilder = CheckoutParameters.newBuilder(amountMoney)
        parametersBuilder.skipReceipt(false)
        parametersBuilder.note("[$vehicleId][square test]")
        val checkoutManager = ReaderSdk.checkoutManager()
        checkoutManager.startCheckoutActivity(context!!, parametersBuilder.build())
    }

    private fun showUnPairDialog(){
        val powerOffApplicationAlert = AlertDialog.Builder(this.activity)
        powerOffApplicationAlert.setTitle("Unpair PIM")
        powerOffApplicationAlert.setMessage("Would you like to unpair from $vehicleId?")
            .setPositiveButton("Yes"){ _, _->
                unPair()
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun unPair(){
        context?.deleteSharedPreferences("MODEL_PREFERENCES")
        val currentTrip = ModelPreferences(context!!.applicationContext)
            .getObject(
                SharedPrefEnum.CURRENT_TRIP.key,
                CurrentTrip::class.java)
        val vehicleSettings = ModelPreferences(context!!.applicationContext)
            .getObject(SharedPrefEnum.VEHICLE_SETTINGS.key,
                VehicleSettings::class.java)
        val pin = ModelPreferences(context!!.applicationContext)
            .getObject(SharedPrefEnum.PIN_PASSWORD.key, PIN::class.java)
        val deviceID = ModelPreferences(context!!.applicationContext)
            .getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)
        val statusObject = ModelPreferences(context!!.applicationContext)
            .getObject(SharedPrefEnum.SETUP_COMPLETE.key, SetupComplete::class.java)

        if(currentTrip == null && vehicleSettings == null && pin == null && deviceID == null && statusObject == null){
            callBackViewModel.clearAllTripValues()
            viewModel.squareIsNotAuthorized()
            viewModel.vehicleIdDoesNotExist()
            viewModel.recheckAuth()
            viewModel.companyNameNoLongerExists()
            keyboardViewModel.qwertyKeyboardIsUp()
            Log.i("Vehicle Settings", "unpair successful, restarting Pim")
            activity?.recreate()
            toStartUp()
        } else {
            Log.i("Vehicle Settings",
                "currentTrip: $currentTrip, vehicle Settings $vehicleSettings, pin $pin,deviceID $deviceID, setUpStatus: $statusObject")
        }
    }
    //** Scott **
    private fun onReaderSettingsResult(result: Result<Void, ResultError<ReaderSettingsErrorCode>>) {
        Log.d("VehicleSettingsDetailFragment", "BlueToothDeviceAdapter: State- ${bluetoothDeviceAdapter.state}, " +
                "Enabled:  ${bluetoothDeviceAdapter.isEnabled}" +
                "Address: ${bluetoothDeviceAdapter.address}" +
                "Bonded Devices: ${bluetoothDeviceAdapter.bondedDevices}" +
                "Scan Mode: ${bluetoothDeviceAdapter.scanMode}" +
                "isDiscovering: ${bluetoothDeviceAdapter.isDiscovering}")
        if (result.isSuccess){
            println("success")
            if (activity_indicator_vehicle_detail != null){
                activity_indicator_vehicle_detail.visibility = View.INVISIBLE
            }
            // sound on if square call wasn't successful in turning sound back on
            SoundHelper.turnOnSound(context!!)
            screenEnabled()
        }
        if (result.isError) {
            //sound on if square call wasn't successful in turning sound back on
            SoundHelper.turnOnSound(context!!)
            screenEnabled()
            activity_indicator_vehicle_detail.visibility = View.INVISIBLE
            val error = result.error
            when (error.code) {
                ReaderSettingsErrorCode.SDK_NOT_AUTHORIZED -> Toast.makeText(
                    context!!,
                    "SDK not authorized${error.message}", Toast.LENGTH_LONG
                ).show()
                ReaderSettingsErrorCode.USAGE_ERROR -> Toast.makeText(
                    context!!,
                    "Usage error ${error.message}", Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun updateUI(batteryStatus: Boolean) {
        val alpha = 1.00f
        val duration = 500.toLong()
        val buildName = BuildConfig.VERSION_NAME
        val isLoggingOn = LoggerHelper.logging
        settings_detail_textView.text = "Vehicle ID: $vehicleId"
        build_version_textView.text = "Build Version: $buildName"
        imei_textView.text = "IMEI: $imei"
        logging_textView.text = "Logging: $isLoggingOn"
        val c = context?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val bucket = c.appStandbyBucket.toString()
        when(bucket){
            "10" ->  power_status_textView.text = "Power Status: Active"
            "20" -> power_status_textView.text = "Power Status:Working Set"
            "30" -> power_status_textView.text = "Power Status:Frequent"
            "40" -> power_status_textView.text = "Power Status:Stand by"
        }

        if(tripID.isNotEmpty()){
            last_trip_id_textView.text = "Trip Id: $tripID"
        } else {
            last_trip_id_textView.text = "Trip Id: none"
        }

        check_bluetooth_btn.animate().alpha(alpha).setDuration(duration)
        setting_detail_back_btn.animate().alpha(alpha).setDuration(duration)
        exit_app_btn.animate().alpha(alpha).setDuration(duration)
        power_Off_PIM_btn.animate().alpha(alpha).setDuration(duration)
        recent_trip_button.animate().alpha(alpha).setDuration(duration)
        updatePowerButtonUI(batteryStatus)
        updateDevButtonUI()
    }
    private fun updatePowerButtonUI(batteryStatus: Boolean){
        if(batteryStatus){
            battery_btn.animate().alpha(0.25f).setDuration(500)
        }else{
            battery_btn.animate().alpha(1.0f).setDuration(500)
        }
    }
    private fun updateDevButtonUI(){
        if(resources.getBoolean(R.bool.isDevModeOn)){
            dev_mode_btn.animate().alpha(0.25f).duration = 500
        }else {
            dev_mode_btn.animate().alpha(0.25f).duration = 500
        }
    }
    private fun showPowerOffDialog(){
        //we have to send the broadcast in a fragment instead of the DialogComposer
            val powerOffApplicationAlert = AlertDialog.Builder(this.activity)
            powerOffApplicationAlert.setTitle("Power Off Application")
            powerOffApplicationAlert.setMessage("Would you like to power off the application?")
                .setPositiveButton("Yes"){ _, _->
                    //This is for inside our own app.
//                    val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//                    audioManager.mode = (AudioManager.MODE_NORMAL)
//                    val intent = Intent(activity, PowerAccessibilityService::class.java)
//                    intent.action = "com.claren.tablet_control.shutdown"
//                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
//                    activity!!.startService(intent)

                    val action =  "com.claren.tablet_control.shutdown"
                    val p = "com.claren.tablet_control"
                    val intent = Intent()
                    intent.action = action
                    intent.`package` = p
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    activity?.sendBroadcast(intent)
                }
                .setNegativeButton("Cancel",null)
                .show()
    }
    private fun screenDisabled(){
        val lowerAlpha = 0.5f
        val notEnabled = false
        exit_app_btn.alpha = lowerAlpha
        check_bluetooth_btn.alpha = lowerAlpha
        power_Off_PIM_btn.alpha = lowerAlpha
        setting_detail_back_btn.alpha = lowerAlpha
        recent_trip_button.alpha = lowerAlpha

        exit_app_btn.isEnabled = notEnabled
        check_bluetooth_btn.isEnabled = notEnabled
        power_Off_PIM_btn.isEnabled = notEnabled
        setting_detail_back_btn.isEnabled = notEnabled
        battery_btn.isEnabled = notEnabled
        recent_trip_button.isEnabled = notEnabled
    }
    private fun screenEnabled(){
        val normalAlpha = 1.0f
        val enabled = true
        if(exit_app_btn != null){
            exit_app_btn.alpha = normalAlpha
            exit_app_btn.isEnabled = enabled
        }
        if(check_bluetooth_btn != null){
            check_bluetooth_btn.alpha = normalAlpha
            check_bluetooth_btn.isEnabled = enabled
        }
        if(power_Off_PIM_btn != null){
            power_Off_PIM_btn.alpha = normalAlpha
            power_Off_PIM_btn.isEnabled = enabled
        }
        if(setting_detail_back_btn != null){
            setting_detail_back_btn.alpha = normalAlpha
            setting_detail_back_btn.isEnabled = enabled

        }
        if(recent_trip_button != null){
            recent_trip_button.alpha = normalAlpha
            recent_trip_button.isEnabled = enabled
        }

        if(battery_btn != null){
            recent_trip_button.isEnabled = enabled
        }
    }
//Navigation
    private fun toRecentTrip(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_vehicle_settings_detail_fragment_to_recentTripAWSFragment)
        }
    }

    private fun toStartUp(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_vehicle_settings_detail_fragment_to_startupFragment)
        }
    }
}