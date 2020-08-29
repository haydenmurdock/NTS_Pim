package com.example.nts_pim.fragments_viewmodel.bluetooth_setup

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.JsonAuthCode
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothHelper
import com.example.nts_pim.utilities.bluetooth_helper.BlueToothServerController
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.google.gson.Gson
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.authorization.AuthorizeCallback
import com.squareup.sdk.reader.authorization.DeauthorizeCallback
import com.squareup.sdk.reader.core.CallbackReference
import com.squareup.sdk.reader.core.Result
import com.squareup.sdk.reader.core.ResultError
import com.squareup.sdk.reader.hardware.ReaderSettingsErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.waitMillis
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.lang.Error
import java.util.*
import kotlin.collections.ArrayList

class BluetoothSetupFragment: ScopedFragment(), KodeinAware {

    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var viewModel: VehicleSetupViewModel
    private var readerSdk = ReaderSdk.authorizationManager()
    private val readerManager = ReaderSdk.readerManager()
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var readerSettingsCallbackRef: CallbackReference? = null
    private val logtag = "Square Reader Setup"
    private var mArrayAdapter: ArrayAdapter<String>? = null
    private var lastCheckStatus: String? = null
    var message = ""
    private var devices = ArrayList<String>()
    private var navController: NavController? = null
    private lateinit var callBackViewModel: CallBackViewModel
    private val currentFragmentId = R.id.bluetoothSetupFragment
    private var vehicleId: String? = null
    private var numberOfReaderFailedAttempts = 0


    private val deauthorizeCallback = DeauthorizeCallback {
        Log.i(logtag, "deauthorize callback: $it")
    }

    private val authCallback = AuthorizeCallback{authorized ->
        Log.i(logtag, "authorizeCallBack: $authorized")
        if(authorized.isSuccess){
            if (numberOfReaderFailedAttempts <= 2){
                Log.i(logtag, "Reader was authorized $numberOfReaderFailedAttempts number of times. Starting square card reader check")
                startSquareCardReaderCheck()
                numberOfReaderFailedAttempts += 1
            } else {
                val cal = Calendar.getInstance()
                PIMMutationHelper.updateReaderStatus(
                    vehicleId!!,
                    VehicleTripArrayHolder.cardReaderStatus,
                    mAWSAppSyncClient!!,
                    cal!!)
                toWelcomeScreen()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bluetooth_setup_screen, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)
        callBackViewModel = ViewModelProviders.of(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        readerSettingsCallbackRef =
            readerManager.addReaderSettingsActivityCallback(this::onReaderSettingsResultBTSetup)
        val pairedDevices = BlueToothHelper.getPairedDevicesAndRegisterBTReceiver(activity!!)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        setUpSquareAuthCallbacks()
        vehicleId = viewModel.getVehicleID()
        devices = ArrayList()
        mArrayAdapter = ArrayAdapter(this.context!!, R.layout.dialog_select_bluetooth_device)
        navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        getArgs()
        startSquareCardReaderCheck()
        callBackViewModel.doWeNeedToReAuthorizeSquare().observe(this.viewLifecycleOwner, Observer {needsAuthorization->
            if(needsAuthorization){
              reauthorizeSquare()
            }
        })
        callBackViewModel.isReaderConnected().observe(this.viewLifecycleOwner, Observer {connected ->
            if(connected){
                Log.i("Square", "last reader check == $lastCheckStatus. Internal status of reader is ${VehicleTripArrayHolder.cardReaderStatus}")
                if(VehicleTripArrayHolder.cardReaderStatus != "default" || lastCheckStatus != VehicleTripArrayHolder.cardReaderStatus){
                    val cal = Calendar.getInstance()
                    PIMMutationHelper.updateReaderStatus(
                        vehicleId!!,
                        VehicleTripArrayHolder.cardReaderStatus,
                        mAWSAppSyncClient!!,
                        cal)
                } else { Log.i("Square", "last reader check == $lastCheckStatus. Internal status of reader is ${VehicleTripArrayHolder.cardReaderStatus}. Did Not update AWS for Second reader check.")
                 LoggerHelper.writeToLog("last reader check == $lastCheckStatus. Internal status of reader is ${VehicleTripArrayHolder.cardReaderStatus}. Did Not update AWS for Second reader check.")
                }
                toWelcomeScreen()
            }
        })
        //adding devices to adapter and device array. If there is not a SAMSUNG device bonded it starts the discovery mode.
        pairedDevices.forEach { device ->
            devices.add(device.first)
            mArrayAdapter!!.add((if (device.first != null) device.first else "Unknown") + "\n" + device.second + "\nPaired")

        }
        pairedDevices.forEach { device ->
            if(device.first.contains("SAMSUNG")){
                Log.i("Bluetooth", "Device has been bonded via bluetooth")
                callBackViewModel.deviceIsBondedViaBT()
                if(btAdapter.isDiscovering){
                    Log.i("Bluetooth", "Device has been bonded, canceling discovery")
                    btAdapter.cancelDiscovery()
                }
            }
        }
    }
    private fun getArgs(){
        lastCheckStatus = arguments?.getString("lastCheckedStatus")
    }
    private fun setUpSquareAuthCallbacks(){
        readerSdk.addAuthorizeCallback(authCallback)
        readerSdk.addDeauthorizeCallback(deauthorizeCallback)
    }
    private fun startSquareCardReaderCheck(){
        ReaderSdk.readerManager().startReaderSettingsActivity(context!!)
    }
    private fun reauthorizeSquare() = launch(Dispatchers.Main.immediate){
        if(readerSdk.authorizationState.canDeauthorize()){
            ReaderSdk.authorizationManager().deauthorize()
            Log.i("LOGGER", "$vehicleId successfully de-authorized")
        }
        if(!vehicleId.isNullOrEmpty()){
            Log.i("LOGGER", "$vehicleId: Trying to reauthorize")
            getAuthorizationCode(vehicleId!!)
        }
    }
    private fun onReaderSettingsResultBTSetup(result: Result<Void, ResultError<ReaderSettingsErrorCode>>) {
        if (result.isSuccess){
        }
        if (result.isError) {
            val error = result.error
            when (error.code) {
                ReaderSettingsErrorCode.SDK_NOT_AUTHORIZED -> {
                    Toast.makeText(
                        context!!,
                        "SDK not authorized, trying to reauthorized square", Toast.LENGTH_LONG
                    ).show()
                    reauthorizeSquare()
                }
                ReaderSettingsErrorCode.USAGE_ERROR -> {
                    Log.i(logtag, "Usage error: ${error.debugMessage}")
                    reauthorizeSquare()
                }
            }
        }
    }

    private fun getAuthorizationCode(vehicleId: String) {
        val url = "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleId"
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
                        onAuthorizationCodeRetrieved(authCode, vehicleId)
                        Log.i("LOGGER", "$vehicleId successfully got AuthCode")
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
        } catch (e: Error) {
            println(e)
        }
    }
    private fun onAuthorizationCodeRetrieved(authorizationCode: String, vehicleId: String)
            = launch(Dispatchers.Main.immediate) {
       readerSdk.authorize(authorizationCode)
    }
    //Navigation
    private fun setUpBluetoothServer(activity: Activity){
        BlueToothServerController(activity).start()
    }

    private fun toWelcomeScreen() = launch(Dispatchers.Main.immediate){
        if (navController?.currentDestination?.id == currentFragmentId) {
            navController?.navigate(R.id.action_bluetoothSetupFragment_to_welcome_fragment)
        }
    }

    override fun onStop() {
        if(view != null){
            callBackViewModel.getTripStatus().removeObservers(this.viewLifecycleOwner)
            callBackViewModel.doWeNeedToReAuthorizeSquare().removeObservers(this.viewLifecycleOwner)
            callBackViewModel.isReaderConnected().removeObservers(this.viewLifecycleOwner)
            readerSettingsCallbackRef?.clear()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if(view != null){
            callBackViewModel.getTripStatus().removeObservers(this.viewLifecycleOwner)
            callBackViewModel.doWeNeedToReAuthorizeSquare().removeObservers(this.viewLifecycleOwner)
            callBackViewModel.isReaderConnected().removeObservers(this.viewLifecycleOwner)
            readerSettingsCallbackRef?.clear()
        }
        super.onDestroy()
    }
}