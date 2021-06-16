package com.example.nts_pim.fragments_viewmodel.square_setup

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.JsonAuthCode
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.Square_Service.SquareHelper
import com.example.nts_pim.utilities.enums.LogEnums
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
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.lang.Error
import kotlin.collections.ArrayList

class SquareSetupFragment: ScopedFragment(), KodeinAware {

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
    private var devices = ArrayList<String>()
    private var navController: NavController? = null
    private lateinit var callBackViewModel: CallBackViewModel
    private val currentFragmentId = R.id.bluetoothSetupFragment
    private var vehicleId: String? = null
    private var numberOfReaderFailedAttempts = 0


    private val deauthorizeCallback = DeauthorizeCallback {
        LoggerHelper.writeToLog("de-authcallback, {${it.error}}}", LogEnums.SQUARE.tag)
    }

    private val authCallback = AuthorizeCallback{authorized ->
        if(!authorized.isSuccess){
            LoggerHelper.writeToLog("auth callback, not successful. Error = ${authorized.error}", LogEnums.SQUARE.tag)
            if(view != null){
                launch(Dispatchers.Main.immediate) {
                    Toast.makeText(context,
                        "auth error: ${authorized.error}",
                        Toast.LENGTH_LONG)
                        .show()
                }
            }
            if (numberOfReaderFailedAttempts <= 2){
                Log.i(logtag, "Reader was authorized $numberOfReaderFailedAttempts number of times. Starting square card reader check")
                startSquareCardReaderCheck(this.requireActivity())
                numberOfReaderFailedAttempts += 1
            } else {
                PIMMutationHelper.updateReaderStatus(
                    vehicleId!!,
                    VehicleTripArrayHolder.cardReaderStatus,
                    mAWSAppSyncClient!!)
                toBluetoothPairing()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.square_setup_screen, container, false)
    }
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)
        callBackViewModel = ViewModelProvider(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        readerSettingsCallbackRef =
            readerManager.addReaderSettingsActivityCallback(this::onReaderSettingsResultBTSetup)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        setUpSquareAuthCallbacks()
        vehicleId = viewModel.getVehicleID()
        devices = ArrayList()
        mArrayAdapter = ArrayAdapter(this.requireContext(), R.layout.dialog_select_bluetooth_device)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        getArgs()
        startSquareCardReaderCheck(this.requireActivity())
        val authStatus = SquareHelper.getAuthStatus()
        LoggerHelper.writeToLog("auth complete at beginning of square setup: $authStatus", LogEnums.SQUARE.tag)
        callBackViewModel.doWeNeedToReAuthorizeSquare().observe(this.viewLifecycleOwner, Observer {needsAuthorization->
            if(needsAuthorization){
               startSquareCardReaderCheck(this.requireActivity())
            }
        })
        callBackViewModel.isReaderConnected().observe(this.viewLifecycleOwner, Observer { connected ->
            if(connected){
                LoggerHelper.writeToLog("Reader connected == true on Square setup fragment. Attempting to update AWS and going to bluetooth pairing", LogEnums.SQUARE.tag)
                if(VehicleTripArrayHolder.cardReaderStatus != "default" || lastCheckStatus != VehicleTripArrayHolder.cardReaderStatus){
                    PIMMutationHelper.updateReaderStatus(
                        vehicleId!!,
                        VehicleTripArrayHolder.cardReaderStatus,
                        mAWSAppSyncClient!!)
                } else {
                   LoggerHelper.writeToLog("last reader check == $lastCheckStatus. Internal status of reader is ${VehicleTripArrayHolder.cardReaderStatus}. Did Not update AWS with reader status.", LogEnums.SQUARE.tag)
                }
                toBluetoothPairing()
            }
        })
        SquareHelper.isSquareAuthorizedMLD?.observe(this.viewLifecycleOwner, Observer {authStatus ->
            if(!authStatus){
              SquareHelper.reauthorizeSquare(vehicleId, this.requireActivity())
            }
            if(authStatus){
                startSquareCardReaderCheck(this.requireActivity())
            }

        })
    }
    private fun getArgs(){
        lastCheckStatus = arguments?.getString("lastCheckedStatus")
    }
    private fun setUpSquareAuthCallbacks(){
        readerSdk.addAuthorizeCallback(authCallback)
        readerSdk.addDeauthorizeCallback(deauthorizeCallback)
    }
    private fun startSquareCardReaderCheck(mainActivity: Activity){
       val isReaderSDKAuthorized = ReaderSdk.authorizationManager().authorizationState.isAuthorized
        if(!isReaderSDKAuthorized){
            LoggerHelper.writeToLog("Reader SDK wasn't authorized at time of square card reader check. Reauthorizing. ", LogEnums.SQUARE.tag)
            SquareHelper.reauthorizeSquare(vehicleId, mainActivity)
        } else {
            LoggerHelper.writeToLog("Reader SDK authorized at time of square card reader check. Starting reader check", LogEnums.SQUARE.tag)
            ReaderSdk.readerManager().startReaderSettingsActivity(requireContext())
        }

    }

    private fun onReaderSettingsResultBTSetup(result: Result<Void, ResultError<ReaderSettingsErrorCode>>) {
        if (result.isSuccess){
            LoggerHelper.writeToLog("onReaderSettings for reader check was successful", LogEnums.SQUARE.tag)
        }
        if (result.isError) {
            val error = result.error
            LoggerHelper.writeToLog("Error on reader settings result. Error Code == ${error.code}", LogEnums.SQUARE.tag)
            when (error.code) {
                ReaderSettingsErrorCode.SDK_NOT_AUTHORIZED -> {
                    Toast.makeText(
                        requireContext(),
                        "SDK not authorized, trying to reauthorized square", Toast.LENGTH_LONG
                    ).show()
                    LoggerHelper.writeToLog("SDK not authorized, trying to reauthorized square", LogEnums.SQUARE.tag)
                    SquareHelper.reauthorizeSquare(vehicleId, this.requireActivity())
                }
                ReaderSettingsErrorCode.USAGE_ERROR -> {
                    Toast.makeText(
                        requireContext(),
                        "Usage Error: ${error.message}", Toast.LENGTH_LONG
                    ).show()
                    LoggerHelper.writeToLog("usage error: ${error.debugCode}, trying to reauthorized square", LogEnums.SQUARE.tag)
                    SquareHelper.reauthorizeSquare(vehicleId, this.requireActivity())
                }
            }
        }
    }
    //Navigation
    private fun toBluetoothPairing(){
        if (navController?.currentDestination?.id == currentFragmentId) {
            LoggerHelper.writeToLog("Going to bluetoothPairing Fragment", LogEnums.LIFE_CYCLE.tag)
            navController?.navigate(R.id.action_bluetoothSetupFragment_to_blueToothPairingFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        if(!callBackViewModel.isReaderConnected().hasActiveObservers()){
            callBackViewModel.isReaderConnected().observe(this.viewLifecycleOwner, Observer {connected ->
                if(connected){
                    toBluetoothPairing()
                }
            })
        }
    }

    override fun onStop() {
        if(view != null){
          //  readerSettingsCallbackRef?.clear()
        }
        super.onStop()
    }

    override fun onDestroyView() {
        if(view != null){
            callBackViewModel.getTripStatus().removeObservers(this.viewLifecycleOwner)
            callBackViewModel.doWeNeedToReAuthorizeSquare().removeObservers(this.viewLifecycleOwner)
            callBackViewModel.isReaderConnected().removeObservers(this.viewLifecycleOwner)
            readerSettingsCallbackRef?.clear()
        }
        super.onDestroyView()
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