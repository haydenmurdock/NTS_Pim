package com.example.nts_pim.fragments_viewmodel.check_vehicle_info

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetCompanyNameQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.VehicleSettings
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import com.example.nts_pim.utilities.bluetooth_helper.ConnectThread
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.github.ybq.android.spinkit.style.ThreeBounce
import kotlinx.android.synthetic.main.check_vehicle_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.lang.Exception

class CheckVehicleInfoFragment: ScopedFragment(), KodeinAware {

    override val kodein by closestKodein()
    private val viewModelFactory: CheckVehicleInfoModelFactory by instance<CheckVehicleInfoModelFactory>()
    private lateinit var viewModel: CheckVehicleInfoViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var mArrayAdapter: ArrayAdapter<String>? = null
    private var vehicleId = ""
    private var cabNumber = ""
    private var companyName = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.check_vehicle_info, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(CheckVehicleInfoViewModel::class.java)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        mArrayAdapter = ArrayAdapter(this.requireContext(), R.layout.dialog_select_bluetooth_device)
       com.example.nts_pim.utilities.view_helper.ViewHelper.hideSystemUI(requireActivity())
        progressBar.animate()
        val threeBounce = ThreeBounce()
        progressBar.setIndeterminateDrawable(threeBounce)
        callBackViewModel.pimPairingValueChangedViaFMP(true)
        getVehicleInfo()
        viewModel.doesCompanyNameExist().observe(this.viewLifecycleOwner, Observer {companyNameExists ->
            if(companyNameExists){
                saveVehicleSettings()
                activity?.recreate()
                val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                navController.navigate(R.id.WelcomeFragment)
            }
        })
    }
    private fun getVehicleInfo() = launch(Dispatchers.IO){
        vehicleId = viewModel.getVehicleID()
        if(vehicleId != ""){
            queryCompanyName(vehicleId)
        }
        cabNumber = getCabNumber(vehicleId)
    }
    private fun queryCompanyName(vehicleID: String)= launch {
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(context)
        }
        mAWSAppSyncClient?.query(GetCompanyNameQuery.builder().vehicleId(vehicleID).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            ?.enqueue(queryCompanyNameCallBack)
    }


    private var queryCompanyNameCallBack = object: GraphQLCall.Callback<GetCompanyNameQuery.Data>(){
        override fun onResponse(response: Response<GetCompanyNameQuery.Data>) {
            Log.i("Results", response.data()?.companyName?.name().toString())
            Log.i("Results", response.data().toString())
            if (response.data()!!.companyName.name() != null) {
                companyName = response.data()!!.companyName.name()!!
                launch(Dispatchers.Main.immediate){
                    viewModel.companyNameExists()
                }.start()
            }
        }
        override fun onFailure(e: ApolloException) {
            Log.e("ERROR", e.toString())
        }
    }
    private fun getCabNumber(vehicleID: String): String{
        var cabNumber = ""
        val numberArray = "1234567890".toCharArray()

        for (i in vehicleID) {
            if (numberArray.contains(i)) {
                cabNumber += i
            }
        }
        return cabNumber
    }

    private fun saveVehicleSettings(){
        val vehicleSettings = VehicleSettings(
            cabNumber,
            companyName
        )
        ModelPreferences(requireContext()).putObject(SharedPrefEnum.VEHICLE_SETTINGS.key, vehicleSettings)
        Log.i("Check Vehicle Info", "Vehicle Settings Saved: cab Number: $cabNumber company Name: $companyName")
    }


    override fun onDestroy() {
        super.onDestroy()
        if(this::viewModel.isInitialized){
            viewModel.doesCompanyNameExist().removeObservers(this)
        }
        if(progressBar != null){
            progressBar.clearAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        progressBar.clearAnimation()
    }

}