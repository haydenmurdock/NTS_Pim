package com.example.nts_pim.fragments_viewmodel.device_id_update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
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
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class DeviceIdUpdate: ScopedFragment(), KodeinAware {
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
        callbackViewModel = ViewModelProviders.of(this,factory).get(CallBackViewModel::class.java)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(VehicleSetupViewModel::class.java)
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
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId) {
            navController.navigate(R.id.startupFragment)
        }
    }
}