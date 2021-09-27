package com.example.nts_pim.fragments_viewmodel.check_vehicle_info

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetCompanyNameQuery
import com.amazonaws.amplify.generated.graphql.GetPimInfoQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.SetupHolder
import com.example.nts_pim.data.repository.model_objects.VehicleSettings
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.welcome.WelcomeViewModel
import com.example.nts_pim.fragments_viewmodel.welcome.WelcomeViewModelFactory
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.github.ybq.android.spinkit.style.ThreeBounce
import kotlinx.android.synthetic.main.check_vehicle_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class CheckVehicleInfoFragment: ScopedFragment(), KodeinAware {

    override val kodein by closestKodein()
    private val viewModelFactory: CheckVehicleInfoModelFactory by instance<CheckVehicleInfoModelFactory>()
    private val welcomeViewModelFactory: WelcomeViewModelFactory by instance<WelcomeViewModelFactory>()
    private lateinit var viewModel: CheckVehicleInfoViewModel
    private lateinit var welcomeViewModel: WelcomeViewModel
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
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(CheckVehicleInfoViewModel::class.java)
        welcomeViewModel = ViewModelProvider(this, welcomeViewModelFactory)
            .get(WelcomeViewModel::class.java)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProvider(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        mArrayAdapter = ArrayAdapter(this.requireContext(), R.layout.dialog_select_bluetooth_device)

       com.example.nts_pim.utilities.view_helper.ViewHelper.hideSystemUI(requireActivity())
        progressBar.animate()
        val threeBounce = ThreeBounce()
        progressBar.setIndeterminateDrawable(threeBounce)
        vehicleId = viewModel.getVehicleID()
        callBackViewModel.pimPairingValueChangedViaFMP(true)
        queryBlueTooth()
        getVehicleInfo()
        viewModel.doesCompanyNameExist().observe(this.viewLifecycleOwner, Observer {companyNameExists ->
            if(companyNameExists){
                saveVehicleSettings()
                welcomeViewModel.isSetupComplete()
                SetupHolder.subscribedToAWS()
                val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                navController.navigate(R.id.action_checkVehicleInfoFragment_to_startupFragment)
            }
        })
    }
    private fun getVehicleInfo() = launch(Dispatchers.IO){
        if(vehicleId != ""){
            queryCompanyName(vehicleId)
        }
        cabNumber = getCabNumber(vehicleId)
    }

    private fun queryBlueTooth() = launch(Dispatchers.IO){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(requireActivity().applicationContext)
        }
            mAWSAppSyncClient?.query(GetPimInfoQuery.builder().vehicleId(vehicleId).build())
                ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                ?.enqueue(awsBluetoothQueryCallBack)
    }
    private var awsBluetoothQueryCallBack = object: GraphQLCall.Callback<GetPimInfoQuery.Data>() {
        override fun onResponse(response: Response<GetPimInfoQuery.Data>) {
            val res = response.data()?.pimInfo
            Log.i("Bluetooth", "Response == $res")
            if(!response.hasErrors()){
                val isBlueToothOn = response.data()?.pimInfo?.useBluetooth()
                if(isBlueToothOn != null){
                    if(isBlueToothOn){
                        Log.i("Bluetooth", "Bluetooth was on in aws")
                        BluetoothDataCenter.turnOnBlueTooth()
                    } else {
                        Log.i("Bluetooth", "Bluetooth was off in aws")
                        BluetoothDataCenter.turnOffBlueTooth()
                    }
                }
            }
        }

        override fun onFailure(e: ApolloException) {

        }
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