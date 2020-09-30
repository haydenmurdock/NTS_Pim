package com.example.nts_pim.fragments_viewmodel.vehicle_settings_detail

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.os.Build
import android.util.Log
import com.amazonaws.amplify.generated.graphql.UnpairPimMutation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.receivers.BatteryPowerReceiver
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.*
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.google.gson.Gson
import com.squareup.sdk.reader.checkout.CheckoutParameters
import com.squareup.sdk.reader.checkout.CurrencyCode
import com.squareup.sdk.reader.checkout.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import type.UnpairPIMInput
import java.io.IOException
import java.lang.Exception


class VehicleSettingsDetailFragment: ScopedFragment(), KodeinAware {

    //kodein and viewModel/Factory
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSettingsDetailViewModelFactory by instance<VehicleSettingsDetailViewModelFactory>()
    private lateinit var callBackViewModel: CallBackViewModel
    private lateinit var viewModel: VehicleSettingsDetailViewModel
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var readerSettingsCallbackRef: CallbackReference? = null
    private val currentFragmentId = R.id.vehicle_settings_detail_fragment
    private var vehicleId = ""
    private var tripID = ""
    private var deviceId = ""
    private val bluetoothDeviceAdapter = BluetoothAdapter.getDefaultAdapter()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.vehicle_settings_detail, container, false)
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAWSAppSyncClient = ClientFactory.getInstance(context)
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
        VehicleTripArrayHolder.readerStatusHasBeenChecked()
        tripID = ModelPreferences(requireContext())
            .getObject(
                SharedPrefEnum.CURRENT_TRIP.key,
                CurrentTrip::class.java)?.tripID ?: ""

        val batteryStatus = callBackViewModel.batteryPowerStatePermission()
        activity_indicator_vehicle_detail.visibility = View.INVISIBLE
        if(context?.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            deviceId =  DeviceIdCheck.getDeviceId() ?: "There was an issue in android 10 with the device Id"
        }
       updateUI(batteryStatus)
        check_bluetooth_btn.setOnClickListener {
            screenDisabled()
            activity_indicator_vehicle_detail.animate()
            activity_indicator_vehicle_detail.visibility = View.VISIBLE
           Log.i("VehicleSettingsDetailFragment", "BlueToothDeviceAdapter: State- ${bluetoothDeviceAdapter.state}, " +
                   "Enabled:  ${bluetoothDeviceAdapter.isEnabled}" +
                   "Address: ${bluetoothDeviceAdapter.address}" +
                   "Bonded Devices: ${bluetoothDeviceAdapter.bondedDevices}" +
                   "Scan Mode: ${bluetoothDeviceAdapter.scanMode}" +
                   "isDiscovering: ${bluetoothDeviceAdapter.isDiscovering}")
            readerManager.startReaderSettingsActivity(requireContext())
        }


        setting_detail_back_btn.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.back_to_welcome_fragment)
        }
        exit_app_btn.setOnClickListener {
            PIMDialogComposer.exitApplication(requireActivity())
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
        reauth_btn.setOnClickListener {
            showReauthorizeDialog(requireActivity(), vehicleId)
        }

        callBackViewModel.isPimPaired().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer { isPaired ->
            if(!isPaired) {
                readerSettingsCallbackRef?.clear()
                unPairPim(vehicleId, mAWSAppSyncClient!!)
            }
        })

        upload_logs_btn.setOnClickListener {
            if(!LoggerHelper.logging){
                launch(Dispatchers.IO) {
                    LoggerHelper.addInternalLogsToAWS(vehicleId)
                }
                Toast.makeText(this.context, "Internal Logs sent to AWS", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this.context, "Logging is already on. Logs not sent", Toast.LENGTH_LONG).show()
            }
        }

        battery_btn.setOnClickListener {
            callBackViewModel.enableOrDisableBatteryPower()
            val batteryPermission = callBackViewModel.batteryPowerStatePermission()
            Toast.makeText(
                requireContext(),
                "Allow Battery Power: $batteryPermission", Toast.LENGTH_LONG
            ).show()
            updatePowerButtonUI(batteryPermission)
        }

    }

    private fun getAuthorizationCode(vehicleId: String) {
        val url =
            "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleId"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.code == 200) {
                        val gson = Gson()
                        val convertedObject =
                            gson.fromJson(response.body?.string(), JsonAuthCode::class.java)
                        val authCode = convertedObject.authCode
                        onAuthorizationCodeRetrieved(authCode)
                        com.example.nts_pim.utilities.view_helper.ViewHelper.makeSnackbar(requireView(), "Re-authorized successful")
                    }
                    if (response.code == 404) {
                        launch(Dispatchers.Main.immediate) {
                            Toast.makeText(
                                context!!,
                                "Vehicle not found in fleet, check fleet management portal",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    if (response.code == 401) {
                        launch(Dispatchers.Main.immediate){
                            Toast.makeText(
                                context!!,
                                "Need to authorize fleet with log In",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("failure")
                }
            })
        } catch (e: Exception) {
            println(e)
        }
    }
    private fun onAuthorizationCodeRetrieved(authorizationCode: String)
            = launch {
        ReaderSdk.authorizationManager().authorize(authorizationCode)
    }
    private fun startSquareFlow(){
        if(ReaderSdk.authorizationManager().authorizationState.isAuthorized){
            callBackViewModel.setAmountForSquareDisplay(1.00)
            val p = 100.00
            val checkOutTotal = p.toLong()
            val amountMoney = Money(checkOutTotal, CurrencyCode.current())
            val parametersBuilder = CheckoutParameters.newBuilder(amountMoney)
            parametersBuilder.skipReceipt(false)
            parametersBuilder.note("[$vehicleId][square test]")
            val checkoutManager = ReaderSdk.checkoutManager()
            checkoutManager.startCheckoutActivity(requireContext(), parametersBuilder.build())
        } else {
            PIMDialogComposer.showSquareNotAuthorized(requireActivity())
        }
    }

    private fun showUnPairDialog(){
        val powerOffApplicationAlert = AlertDialog.Builder(this.activity)
        powerOffApplicationAlert.setTitle("Unpair PIM")
        powerOffApplicationAlert.setMessage("Would you like to unpair from $vehicleId?")
            .setPositiveButton("Yes"){ _, _->
                unPairPim(vehicleId, mAWSAppSyncClient!!)
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun unPair(){
        context?.deleteSharedPreferences("MODEL_PREFERENCES")
        val currentTrip = ModelPreferences(requireContext().applicationContext)
            .getObject(
                SharedPrefEnum.CURRENT_TRIP.key,
                CurrentTrip::class.java)
        val vehicleSettings = ModelPreferences(requireContext().applicationContext)
            .getObject(SharedPrefEnum.VEHICLE_SETTINGS.key,
                VehicleSettings::class.java)
        val pin = ModelPreferences(requireContext().applicationContext)
            .getObject(SharedPrefEnum.PIN_PASSWORD.key, PIN::class.java)
        val deviceID = ModelPreferences(requireContext().applicationContext)
            .getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)
        val statusObject = ModelPreferences(requireContext().applicationContext)
            .getObject(SharedPrefEnum.SETUP_COMPLETE.key, SetupComplete::class.java)

        if(currentTrip == null && vehicleSettings == null && pin == null && deviceID == null && statusObject == null){
            callBackViewModel.clearAllTripValues()
            viewModel.squareIsNotAuthorized()
            viewModel.vehicleIdDoesNotExist()
            viewModel.recheckAuth()
            viewModel.companyNameNoLongerExists()
            keyboardViewModel.qwertyKeyboardIsUp()
            BluetoothDataCenter.turnOffBlueTooth()
            BluetoothDataCenter.blueToothSocketIsDisconnected()
            BluetoothDataCenter.disconnectedToDriverTablet()
            Log.i("Vehicle Settings", "unpair successful, restarting Pim")
           // activity?.recreate()
            toStartUp()
        } else {
            Log.i("Vehicle Settings",
                "currentTrip: $currentTrip, vehicle Settings $vehicleSettings, pin $pin,deviceID $deviceID, setUpStatus: $statusObject")
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
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
            SoundHelper.turnOnSound(requireContext())
            screenEnabled()
        }
        if (result.isError) {
            //sound on if square call wasn't successful in turning sound back on
            SoundHelper.turnOnSound(requireContext())
            screenEnabled()
            activity_indicator_vehicle_detail.visibility = View.INVISIBLE
            val error = result.error
            when (error.code) {
                ReaderSettingsErrorCode.SDK_NOT_AUTHORIZED -> Toast.makeText(
                    requireContext(),
                    "SDK not authorized${error.message}", Toast.LENGTH_LONG
                ).show()
                ReaderSettingsErrorCode.USAGE_ERROR -> Toast.makeText(
                    requireContext(),
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
        imei_textView.text = "Device Identifier: $deviceId"
        logging_textView.text = "Logging: $isLoggingOn"
        val c = context?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val bucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            c.appStandbyBucket.toString()
        } else {
            val bucket = "50"
        }
        when(bucket){
            "10" -> power_status_textView.text = "Power Status: Active"
            "20" -> power_status_textView.text = "Power Status:Working Set"
            "30" -> power_status_textView.text = "Power Status:Frequent"
            "40" -> power_status_textView.text = "Power Status:Stand by"
            "50" -> power_status_textView.text = "Current OS version does not support power bucket check"
        }
        val currentBatteryTemp = BatteryPowerReceiver.temp
        battery_temp_textView.text = "Battery Temp: $currentBatteryTemp F"

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

    private fun showReauthorizeDialog(activity: Activity, vehicleId: String){
        val exitApplicationAlert = AlertDialog.Builder(activity)
        exitApplicationAlert.setTitle("Re-Authorize Square Account?")
        exitApplicationAlert.setMessage("Note: If yes is picked, you might need to re-pair reader")
            .setPositiveButton("Yes"){ _, _->
                if(ReaderSdk.authorizationManager().authorizationState.canDeauthorize()){
                    ReaderSdk.authorizationManager().deauthorize()
                }
                if(!ReaderSdk.authorizationManager().authorizationState.isAuthorized){
                    getAuthorizationCode(vehicleId)
                }
            }
            .setNegativeButton("Cancel",null)
            .show()
    }
    private fun unPairPim(vehicleId: String, appSyncClient: AWSAppSyncClient){
        val unpairPIMInput = UnpairPIMInput.builder().vehicleId(vehicleId).build()
        appSyncClient.mutate(UnpairPimMutation.builder().parameters(unpairPIMInput).build()).enqueue(
            mutationCallbackUnpairPim)
    }
    private val mutationCallbackUnpairPim = object : GraphQLCall.Callback<UnpairPimMutation.Data>() {
        override fun onResponse(response: Response<UnpairPimMutation.Data>) {
            if(!response.hasErrors()){
                LoggerHelper.writeToLog("Successfully unpaired PIM")
                launch(Dispatchers.Main.immediate) {
                    unPair()
                }
            }
            if(response.hasErrors()){
                com.example.nts_pim.utilities.view_helper.ViewHelper.makeSnackbar(requireView(), "Unpair was unsuccessful. Error: ${response.errors()}")
            }
        }

        override fun onFailure(e: ApolloException) {
            Log.e("Error", "There was an issue updating the pimStatus: $e")
            com.example.nts_pim.utilities.view_helper.ViewHelper.makeSnackbar(requireView(), "Unpair was unsuccessful. Failure due to ${e.message}}")
        }
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
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_vehicle_settings_detail_fragment_to_recentTripAWSFragment)
        }
    }

    private fun toStartUp(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_vehicle_settings_detail_fragment_to_startupFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        readerSettingsCallbackRef?.clear()
        if(this::callBackViewModel.isInitialized){
            callBackViewModel.isPimPaired().removeObservers(this)
        }
    }
}