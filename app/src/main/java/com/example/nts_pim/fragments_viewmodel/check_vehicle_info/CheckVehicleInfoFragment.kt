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
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var viewModel: CheckVehicleInfoViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var mArrayAdapter: ArrayAdapter<String>? = null
    private var blueToothSetUpComplete:Boolean = false
    private var vehicleId = ""
    private var cabNumber = ""
    private var companyName = ""
    private var testBTAddress = "6C:00:6B:A8:5F:3C"

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
        val vehicleId = viewModel.getVehicleID()
        progressBar.setIndeterminateDrawable(threeBounce)
        callBackViewModel.pimPairingValueChangedViaFMP(true)
        BlueToothHelper.getPairedDevicesAndRegisterBTReceiver(requireActivity())
//        requestToPairBlueTooth(requireActivity(), vehicleId)
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
//    private val receiver = object : BroadcastReceiver() {
//
//        override fun onReceive(context: Context, intent: Intent) {
//            when (intent.action) {
//                BluetoothDevice.ACTION_FOUND -> {
//                    // Discovery has found a device. Get the BluetoothDevice
//                    // object and its info from the Intent.
//                    val device: BluetoothDevice =
//                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                    val deviceName = device.name
//                    val deviceHardwareAddress = device.address // MAC address
//                    Log.i("Bluetooth Device found", "Device Name: $deviceName, deviceBTAddress: $deviceHardwareAddress")
//                    if(deviceHardwareAddress == testBTAddress){
//                        ConnectThread(device).start()
//
//                    }
//                }
//                "android.bluetooth.device.action.PAIRING_REQUEST" -> {
//                    try {
//                        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
//                        val pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
//                        //the pin in case you need to accept for an specific pin
//                        Log.i("Bluetooth", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY",0));
//                        //maybe you look for a name or address
//                        Log.i("Bluetooth", device.name);
//                        var pinBytes: ByteArray
//                        pinBytes = (""+pin).toByteArray(Charsets.UTF_8)
//                        device.setPin(pinBytes);
//                        //setPairing confirmation if needed
//                       // device.setPairingConfirmation(true);
//                    } catch (e: Exception) {
//                        e.printStackTrace();
//                    }
//                }
//                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
//                    val state =
//                        intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
//                    val prevState = intent.getIntExtra(
//                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
//                        BluetoothDevice.ERROR
//                    )
//                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
//                        Toast.makeText(
//                            context,
//                            "Paired",
//                            Toast.LENGTH_SHORT
//                        )
//                            .show()
//                    }
//                }
//            }
//        }
//    }

    private fun requestToPairBlueTooth(activity: Activity, vehicleId: String){
        val bluetoothRequest = AlertDialog.Builder(activity)
        bluetoothRequest.setTitle("Pair bluetooth devices")
        bluetoothRequest.setMessage("Would you like to connect via bluetooth to $vehicleId")
            .setPositiveButton("Pair"){ _, _->
               //begin pairing process
                //start scanning
//                val filter = IntentFilter()
//                filter.addAction(BluetoothDevice.ACTION_FOUND)
//                filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
//                filter.addAction("android.bluetooth.device.action.PAIRING_REQUEST")
//                activity.registerReceiver(receiver, filter)
                bluetoothAdapter?.startDiscovery()


            }
            .setNegativeButton("No"){ _,_ ->
                blueToothSetUpComplete = true
                LoggerHelper.writeToLog("User picked no for pairing bluetooth. Not connect to driver tablet during setup.")
                Log.i("CheckVehicleInfo", "User selected no for bluetooth pairing")
                getVehicleInfo()
            }.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(this::viewModel.isInitialized){
            viewModel.doesCompanyNameExist().removeObservers(this)
        }
        if(progressBar != null){
            progressBar.clearAnimation()
        }
//        if(receiver != null){
//            activity?.unregisterReceiver(receiver)
//        }

    }

    override fun onPause() {
        super.onPause()
        progressBar.clearAnimation()
    }

}