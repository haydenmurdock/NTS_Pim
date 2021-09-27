package com.example.nts_pim.fragments_viewmodel.device_id_update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

/**
 * This is the starting place for the navigation of the app.
 * It will go to the startup fragment no matter what, but also it will update AWS deviceId with an androidId if it has an IMEI.
 * We use the android id(a.k.a. device id) to get the Pim_Settings from AWS so we want it to be current and  correct before we try to grab them.
 *
 * This exists since there was a time when we used the imei as the device id, but android 10 removed access so we had to switch to the android id.
 *
 * Hopefully this can be removed someday or be better utilized.
 *
 */

class DeviceIdUpdateFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var viewModel: VehicleSetupViewModel
    private lateinit var callbackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private val currentFragmentId = R.id.deviceIdUpdate

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.device_id_update, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callbackViewModel = ViewModelProvider(this,factory).get(CallBackViewModel::class.java)
        viewModel = ViewModelProvider(this, viewModelFactory).get(VehicleSetupViewModel::class.java)
        val isSetUpComplete = viewModel.isSetUpComplete()
        if(isSetUpComplete){
            checkForIMEIAndUpdateDeviceId()
        } else {
            goToVehicleStartUp()
        }
    }
    private fun checkForIMEIAndUpdateDeviceId(){
      val needToUpdateAWS = DeviceIdCheck.needToUpdateBackendForDeviceId()
        val deviceId = DeviceIdCheck.getDeviceId() ?: ""
        val vehicleId = viewModel.getVehicleID()
        if(needToUpdateAWS && deviceId != ""){
            PIMMutationHelper.updateDeviceId(deviceId, mAWSAppSyncClient!!, vehicleId)
        }
        goToVehicleStartUp()
    }

    //Navigation
    private fun goToVehicleStartUp(){
            val loggingCheckComplete = LoggerHelper.deleteLogsOverLimit()
            if(loggingCheckComplete){
                val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                if (navController.currentDestination?.id == currentFragmentId) {
                    navController.navigate(R.id.startupFragment)
                }
            }
    }
}