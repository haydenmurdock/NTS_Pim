package com.example.nts_pim.fragments_viewmodel.vehicle_settings_detail

import android.app.AlertDialog
import android.content.*
import android.media.AudioManager
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
import com.example.nts_pim.utilities.power_cycle.PowerAccessibilityService
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
import com.example.nts_pim.BuildConfig


class VehicleSettingsDetailFragment: ScopedFragment(), KodeinAware {

    //kodein and viewModel/Factory
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSettingsDetailViewModelFactory by instance()
    private lateinit var callBackViewModel: CallBackViewModel
    private lateinit var viewModel: VehicleSettingsDetailViewModel
    private var readerSettingsCallbackRef: CallbackReference? = null
    private val currentFragmentId = R.id.vehicle_settings_detail_fragment

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

        val batteryStatus = callBackViewModel.batteryPowerStatePermission()
        updateUI(batteryStatus)
        activity_indicator_vehicle_detail.visibility = View.INVISIBLE

        check_bluetooth_btn.setOnClickListener {
            SoundHelper.turnOffSound(context!!)
            screenDisabled()
            activity_indicator_vehicle_detail.animate()
            activity_indicator_vehicle_detail.visibility = View.VISIBLE
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


    private fun onReaderSettingsResult(result: Result<Void, ResultError<ReaderSettingsErrorCode>>) {
        if (result.isSuccess){
            println("success")
            activity_indicator_vehicle_detail.visibility = View.INVISIBLE
            SoundHelper.turnOnSound(context!!)
            screenEnabled()
        }
        if (result.isError) {
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
        val vehicleID = viewModel.getVehicleID()
        val tripID = callBackViewModel.getTripId()
        val buildName = BuildConfig.VERSION_NAME

        settings_detail_textView.text = "Vehicle ID: $vehicleID"
        build_version_textView.text = "Build Version: $buildName"
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
                    val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.mode = (AudioManager.MODE_NORMAL)
                    val intent = Intent(activity, PowerAccessibilityService::class.java)
                    intent.action = "com.claren.tablet_control.shutdown"
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    activity!!.startService(intent)

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
        exit_app_btn.alpha = normalAlpha
        check_bluetooth_btn.alpha = normalAlpha
        power_Off_PIM_btn.alpha = normalAlpha
        setting_detail_back_btn.alpha = normalAlpha
        recent_trip_button.alpha = normalAlpha

        exit_app_btn.isEnabled = enabled
        check_bluetooth_btn.isEnabled = enabled
        power_Off_PIM_btn.isEnabled = enabled
        setting_detail_back_btn.isEnabled = enabled
        battery_btn.isEnabled = enabled
        recent_trip_button.isEnabled = enabled
    }

    private fun toRecentTrip(){
        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_vehicle_settings_detail_fragment_to_recentTripAWSFragment)
        }
    }

}