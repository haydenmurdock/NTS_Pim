package com.example.nts_pim.fragments_viewmodel.vehicle_settings_detail

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.CurrentTrip
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.receivers.BatteryPowerReceiver
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import kotlinx.android.synthetic.main.tablet_info.*
import kotlinx.android.synthetic.main.vehicle_settings_detail.battery_temp_textView
import kotlinx.android.synthetic.main.vehicle_settings_detail.build_version_textView
import kotlinx.android.synthetic.main.vehicle_settings_detail.imei_textView
import kotlinx.android.synthetic.main.vehicle_settings_detail.last_trip_id_textView
import kotlinx.android.synthetic.main.vehicle_settings_detail.logging_textView
import kotlinx.android.synthetic.main.vehicle_settings_detail.power_status_textView
import kotlinx.android.synthetic.main.vehicle_settings_detail.settings_detail_textView
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance


class TabletInfoFragment: ScopedFragment(), KodeinAware{
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSettingsDetailViewModelFactory by instance<VehicleSettingsDetailViewModelFactory>()
    private lateinit var viewModel: VehicleSettingsDetailViewModel
    private val currentFragmentId = R.id.tabletInfoFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tablet_info, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(VehicleSettingsDetailViewModel::class.java)

        updateUI(requireContext())

        back_to_admin_screen_imageView.setOnClickListener {
            backToAdminScreen()
        }

    }

    private fun updateUI(context: Context) {
        val buildName = BuildConfig.VERSION_NAME
        val isLoggingOn = LoggerHelper.logging
        val deviceId = getDeviceId(context) ?: "issue with device id"
        val tripId = getTripId()
        settings_detail_textView.text = "Vehicle ID: ${viewModel.getVehicleID()}"
        build_version_textView.text = "Build Version: $buildName"
        imei_textView.text = "Device Identifier: $deviceId"
        logging_textView.text = "Logging: $isLoggingOn"
        val c = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val bucket: Any? = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            c.appStandbyBucket.toString()
        } else {
            "50"
        })
        when (bucket) {
            "10" -> power_status_textView.text = "Power Status: Active"
            "20" -> power_status_textView.text = "Power Status:Working Set"
            "30" -> power_status_textView.text = "Power Status:Frequent"
            "40" -> power_status_textView.text = "Power Status:Stand by"
            "50" -> power_status_textView.text =
                "Current OS version does not support power bucket check"
        }
        val currentBatteryTemp = BatteryPowerReceiver.temp
        battery_temp_textView.text = "Battery Temp: $currentBatteryTemp F"

        if (tripId.isNotEmpty()) {
            last_trip_id_textView.text = "Trip Id: $tripId"
        } else {
            last_trip_id_textView.text = "Trip Id: none"
        }
    }

   private fun getDeviceId(context: Context):String? {
        if(context.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            return DeviceIdCheck.getDeviceId()
        }
        return  null
    }

    private fun getTripId(): String {
      return ModelPreferences(requireContext())
            .getObject(
                SharedPrefEnum.CURRENT_TRIP.key,
                CurrentTrip::class.java)?.tripID ?: ""
    }

    //Navigation

    private fun backToAdminScreen(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_tabletInfoFragment_to_vehicle_settings_detail_fragment)
        }
    }

}