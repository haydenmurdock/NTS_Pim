package com.example.nts_pim.fragments_viewmodel.square_setup

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.SetupHolder
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.data.repository.model_objects.JsonAuthCode
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.startup.adapter.StartupAdapter
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.Square_Service.SquareHelper
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.keyboards.PhoneKeyboard
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.google.gson.Gson
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.authorization.AuthorizeCallback
import com.squareup.sdk.reader.core.CallbackReference
import com.squareup.sdk.reader.core.Result
import com.squareup.sdk.reader.core.ResultError
import com.squareup.sdk.reader.hardware.ReaderSettingsErrorCode
import kotlinx.android.synthetic.main.startup.*
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
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class SquareSetupFragment: ScopedFragment(), KodeinAware {

    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var viewModel: VehicleSetupViewModel
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
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
    private var adapterOne: StartupAdapter? = null
    private var adapterTwo: StartupAdapter? = null
    private var adapterThree: StartupAdapter? = null
    private var attemptingToAuth = false
    private var buttonCount = 0
    private val password = "1234"
    private var isPasswordEntered = false
    private val fullBrightness = 255
    private var mobileAuthTimeOut: Long = 10
    private var maxTimeout: Long = 60


    private val authCallback = AuthorizeCallback{ authorized ->
        if(authorized.isSuccess){
            startSquareCardReaderCheck()
        }
        if(!authorized.isSuccess){
            LoggerHelper.writeToLog("auth callback, not successful. Error = ${authorized.error}", LogEnums.SQUARE.tag)
            if(view != null){
                launch(Dispatchers.Main.immediate) {
                    Toast.makeText(context,
                        "auth error: ${authorized.error}",
                        Toast.LENGTH_LONG)
                        .show()
                }
                if(authorized.error.debugCode == "authorize_code_already_used"){
                    SquareHelper.deleteMac(authorized.error.debugCode, requireContext())
                    attemptingToAuth = false
                }
                if(authorized.error.debugCode == "authorize_code_already_in_progress"){
                    SquareHelper.deleteMac(authorized.error.debugCode, requireContext())
                    attemptingToAuth = false
                }
            }
            if (numberOfReaderFailedAttempts <= 3){
                Log.i(logtag, "Reader was authorized $numberOfReaderFailedAttempts number of times. Starting square card reader check")
                startSquareCardReaderCheck()
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
        return inflater.inflate(R.layout.startup, container, false)
    }
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callBackFactory = InjectorUtiles.provideCallBackModelFactory()
        val keyboardViewModelFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)
        callBackViewModel = ViewModelProvider(this, callBackFactory)
            .get(CallBackViewModel::class.java)
        keyboardViewModel = ViewModelProvider(this, keyboardViewModelFactory)
            .get(SettingsKeyboardViewModel::class.java)
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        setUpSquareAuthCallbacks()
        setupReaderSettingsCallback()
        vehicleId = viewModel.getVehicleID()
        devices = ArrayList()
        mArrayAdapter = ArrayAdapter(this.requireContext(), R.layout.dialog_select_bluetooth_device)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        getArgs()
        turnOnBluetooth()
        initRecyclerViews()
        setUpKeyboard()
        stepOneImageButton.setOnClickListener {
            openCloseStepOneListView()
        }

        stepTwoImageButton.setOnClickListener {
            openCloseStepTwoListView()
        }

        stepThreeImageButton.setOnClickListener {
            openCloseStepThreeListView()
        }
        callBackViewModel.doWeNeedToReAuthorizeSquare().observe(this.viewLifecycleOwner, Observer {needsAuthorization->
            if(needsAuthorization){
               reauthorizeSquare()
            }
        })
        callBackViewModel.isReaderConnected().observe(this.viewLifecycleOwner, Observer { connected ->
            if(connected){
                if(VehicleTripArrayHolder.cardReaderStatus != "default") {
                    SetupHolder.foundReaderStatus(VehicleTripArrayHolder.cardReaderStatus)
                }
                LoggerHelper.writeToLog("Reader connected == true on Square setup fragment. Attempting to update AWS and going to bluetooth pairing", LogEnums.SQUARE.tag)
                if(VehicleTripArrayHolder.cardReaderStatus != "default" || lastCheckStatus != VehicleTripArrayHolder.cardReaderStatus){
                    PIMMutationHelper.updateReaderStatus(
                        vehicleId!!,
                        VehicleTripArrayHolder.cardReaderStatus,
                        mAWSAppSyncClient!!)
                }
            }
        })

        SetupHolder.isBluetoothON().observe(this.viewLifecycleOwner, Observer {
            if(it){
                updateAdapter(adapterTwo!!)
                checkBluetoothAdapter()
            }
            if(!it){
                updateAdapter(adapterTwo!!)
            }
        })

        SetupHolder.isBluetoothAdapterReady().observe(this.viewLifecycleOwner, Observer { available ->
            if(available){
                updateAdapter(adapterTwo!!)
                startSquareCardReaderCheck()
            }
            if(!available){
                updateAdapter(adapterTwo!!)
            }
        })

        SetupHolder.isSquareInit().observe(this.viewLifecycleOwner, Observer { init ->
            if(init){
                updateAdapter(adapterTwo!!)
            }
        })

        SetupHolder.isAuthorizedWithSquare().observe(this.viewLifecycleOwner, Observer { authorized ->
            if(authorized){
                updateAdapter(adapterTwo!!)
            } else {
                updateAdapter(adapterTwo!!)
            }
        })
        SetupHolder.hasContactedReader().observe(this.viewLifecycleOwner, Observer { foundReader ->
            if(foundReader){
                updateAdapter(adapterTwo!!)
            }
        })
        SetupHolder.hasFoundReaderStatus().observe(this.viewLifecycleOwner, Observer { foundStatus ->
            if(foundStatus){
                updateAdapter(adapterTwo!!)
            }
        })
        SetupHolder.hasUpdatedAWSWithReaderStatus().observe(this.viewLifecycleOwner, Observer { updatedAWS ->
            if(updatedAWS){
                updateAdapter(adapterTwo!!)
                 toBluetoothPairing()
            }
        })
        keyboardViewModel.isPhoneKeyboardUp().observe(this.viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it) {
                isPasswordEntered = true
            }
            if (!it && isPasswordEntered) {
                checkPassword()
                isPasswordEntered = false
            }
        })
        admin_screen_btn.setOnClickListener {
            changeScreenBrightness(fullBrightness)
            buttonCount += 1
            if (buttonCount in 2..5) {
                showToastMessage("$buttonCount", 1000)
            }
            if (buttonCount == 5) {
                admin_screen_btn.animate().alpha(1.00f).duration = 500
                keyboardViewModel.phoneKeyboardIsUp()
                ViewHelper.viewSlideUp(password_scroll_view, 500)
            }
            if (buttonCount >= 6) {
                if (buttonCount % 2 == 0) {
                    admin_screen_btn.animate().alpha(0.0f).duration = 500
                    ViewHelper.viewSlideDown(password_scroll_view, 500)
                    password_editText.setText("")
                    keyboardViewModel.bothKeyboardsDown()
                } else {
                    admin_screen_btn.animate().alpha(1.00f).duration = 500
                    keyboardViewModel.phoneKeyboardIsUp()
                    ViewHelper.viewSlideUp(password_scroll_view, 500)
                }
            }
        }
    }

    private fun setupReaderSettingsCallback(){
        if(readerSettingsCallbackRef == null){
            readerSettingsCallbackRef =
                readerManager.addReaderSettingsActivityCallback(this::onReaderSettingsResultBTSetup)
        } else {
            LoggerHelper.writeToLog("ReaderSettings ref already existed at time of Square chip reader check", LogEnums.SQUARE.tag)
        }

    }
    private fun setUpKeyboard() {
        password_editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        password_editText.setTextIsSelectable(false)
        val phoneKeyboard = startupPasswordPhoneKeyboard as PhoneKeyboard
        val ic = password_editText.onCreateInputConnection(EditorInfo())
        phoneKeyboard.setInputConnection(ic)
    }
    private fun checkPassword() {
        val passwordEntered = password_editText.text.toString().replace("\\s".toRegex(), "")
        if (passwordEntered == password) {
            changeScreenBrightness(255)
            toVehicleSettingsDetail()
        }
    }

    private fun toVehicleSettingsDetail(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_bluetoothSetupFragment_to_vehicle_settings_detail_fragment)
        }
    }
    private fun showToastMessage(text: String, duration: Int) {
        val toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT)
        toast.show()
        val handler = Handler()
        handler.postDelayed( { toast.cancel() }, duration.toLong())
    }

    private fun turnOnBluetooth(){
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null){
            SetupHolder.pimBluetoothIsOff()
            return
        }
        if (!mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable()
            SetupHolder.pimBluetoothIsOn()
        } else {
            LoggerHelper.writeToLog("bluetooth was on during start up", LogEnums.BLUETOOTH.tag)
            SetupHolder.pimBluetoothIsOn()
        }
    }
    private fun openCloseStepOneListView(){
        if(stepOneListView.visibility == View.VISIBLE) {
            stepOneListView.visibility = View.GONE
            stepOneImageButton.setImageResource(R.drawable.ic_plus_btn_white)
            return
        }

        if(stepOneListView.visibility == View.GONE){
            stepOneListView.visibility = View.VISIBLE
            stepOneImageButton.setImageResource(R.drawable.ic_close_btn_white)
        }
    }

    private fun openCloseStepTwoListView(){
        if(stepTwoListView.visibility == View.VISIBLE) {
            stepTwoListView.visibility = View.GONE
            stepTwoImageButton.setImageResource(R.drawable.ic_plus_btn_white)
            return
        }

        if(stepTwoListView.visibility == View.GONE){
            stepTwoListView.visibility = View.VISIBLE
            stepTwoImageButton.setImageResource(R.drawable.ic_close_btn_white)
        }
    }

    private fun openCloseStepThreeListView(){
        if(stepThreeListView.visibility == View.VISIBLE) {
            stepThreeListView.visibility = View.GONE
            stepThreeImageButton.setImageResource(R.drawable.ic_plus_btn_white)
            return
        }

        if(stepThreeListView.visibility == View.GONE){
            stepThreeListView.visibility = View.VISIBLE
            stepThreeImageButton.setImageResource(R.drawable.ic_close_btn_white)
        }
    }

    private fun updateAdapter(adapter: StartupAdapter){
        adapter.notifyDataSetChanged()
    }
    private fun initRecyclerViews(){
        makeStartupList()
        if(stepOneListView != null) {
            stepOneListView.adapter = adapterOne as StartupAdapter
        }
        if(stepTwoListView != null) {
            stepTwoListView.adapter = adapterTwo as StartupAdapter
        }
        if(stepThreeListView != null){
            stepThreeListView.adapter = adapterThree as StartupAdapter
        }
    }

    private fun makeStartupList() {
        stepOne_title_textView.text = "AWS: Connected"
        step_one_status_imageView.visibility = View.VISIBLE

        val stepOneList = SetupHolder.getStepOneList()
        val stepTwoList = SetupHolder.getStepTwoList()
        val stepThreeList = SetupHolder.getStepThreeList()
        adapterOne = StartupAdapter(requireContext(), stepOneList)
        adapterTwo = StartupAdapter(requireContext(), stepTwoList)
        adapterThree = StartupAdapter(requireContext(), stepThreeList)
    }
    private fun getArgs(){
        lastCheckStatus = arguments?.getString("lastCheckedStatus")
    }
    private fun setUpSquareAuthCallbacks(){

        readerSdk.addAuthorizeCallback(authCallback)
    }
    private fun startSquareCardReaderCheck(){
        ReaderSdk.readerManager().startReaderSettingsActivity(requireContext())
    }

    private fun onReaderSettingsResultBTSetup(result: Result<Void, ResultError<ReaderSettingsErrorCode>>) {
        if (result.isSuccess){
            LoggerHelper.writeToLog("onReaderSettings for reader check was successful", LogEnums.SQUARE.tag)
            SetupHolder.isAuthorized()
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
                    SetupHolder.notAuthorized()
                    reauthorizeSquare()
                }
                ReaderSettingsErrorCode.USAGE_ERROR -> {
                    LoggerHelper.writeToLog("usage error: ${error.debugCode}", LogEnums.SQUARE.tag)
                }
            }
        }
    }
    private fun reauthorizeSquare(){
        if(!attemptingToAuth){
            attemptingToAuth = true
            if(ReaderSdk.authorizationManager().authorizationState.canDeauthorize()){
                ReaderSdk.authorizationManager().deauthorize()
                LoggerHelper.writeToLog("Reader was de-authorized", SquareHelper.logTag)
            }
            val isLastMACExpired = SquareHelper.isMACExpired(requireContext())
            if(isLastMACExpired == null || isLastMACExpired){
                LoggerHelper.writeToLog("MAC was expired or didn't exist. Getting NEW MAC for authorization. isLastMacExpired $isLastMACExpired", LogEnums.SQUARE.tag)
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        getMobileAuthorizationCode()
                    },
                    3000 // value in milliseconds
                )
            } else {
                val lastMACid = SquareHelper.getLastMAC(requireContext())?.getId() ?: ""
                LoggerHelper.writeToLog("Last mac id was less than 1 hour old. Using OLD MAC for authorization. Id: $lastMACid", LogEnums.SQUARE.tag)
                if(lastMACid != ""){
                    launch(Dispatchers.Main.immediate){
                        ReaderSdk.authorizationManager().authorize(lastMACid)
                    }
                } else {
                    LoggerHelper.writeToLog("Last mac id was less than 1 hour old, but MAC wasn't formatted correctly. Getting new MAC", LogEnums.SQUARE.tag)
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                           getMobileAuthorizationCode()
                        },
                        3000 // value in milliseconds
                    )

                }
            }

        }
    }
    private fun changeScreenBrightness(screenBrightness: Int) {
        Settings.System.putInt(
            requireContext().contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )  //this will set the manual mode (set the automatic mode off)
        Settings.System.putInt(
            requireContext().contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            screenBrightness
        )  //this will set the brightness to maximum (255)

        //refreshes the screen
        val br =
            Settings.System.getInt(
                requireContext().contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        val lp = requireActivity().window.attributes
        lp.screenBrightness = br.toFloat() / 255
        requireActivity().window.attributes = lp
    }


    private fun getMobileAuthorizationCode() {
        val dateTime = ViewHelper.formatDateUtcIso(Date())
        val url = "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleId&source=PIM&eventTimeStamp=$dateTime&extraInfo=CHIP_READER_STATUS_CHECK"
        LoggerHelper.writeToLog("Sending MAC request to: $url. Connection Timeout: $mobileAuthTimeOut", LogEnums.SQUARE.tag)
        val client = OkHttpClient().newBuilder()
            .connectTimeout(mobileAuthTimeOut, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
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
                        LoggerHelper.writeToLog("Successfully got MAC from Square_OAuth Lambda. MAC: $authCode",
                            SquareHelper.logTag
                        )
                        onAuthorizationCodeRetrieved(authCode)
                    }
                    if(response.code == 404) {
                        LoggerHelper.writeToLog("Vehicle not found in fleet. Error Code: ${response.code}",
                            SquareHelper.logTag
                        )
                    }
                    if (response.code == 401) {
                        LoggerHelper.writeToLog("Need to authorize fleet with log In. Error Code: ${response.code}",
                            SquareHelper.logTag
                        )
                    }
                    if(response.code == 500){
                        LoggerHelper.writeToLog("Error code 500: error message: ${response.message}",
                            SquareHelper.logTag
                        )
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    LoggerHelper.writeToLog("Failure getting auth code", SquareHelper.logTag)
                    val additionalTime: Long = 10
                    if(mobileAuthTimeOut != maxTimeout){
                        mobileAuthTimeOut += additionalTime
                    }
                }
            })
        } catch (e: Error) {
            LoggerHelper.writeToLog("Error requesting auth code from aws. Error: $e", LogEnums.SQUARE.tag)
        }
    }

    private fun onAuthorizationCodeRetrieved(authorizationCode: String)  {
        LoggerHelper.writeToLog("Authorizing SQUARE on the MainThread", LogEnums.SQUARE.tag)
        activity?.runOnUiThread{
            ReaderSdk.authorizationManager().authorize(authorizationCode)
        }
        SquareHelper.saveMAC(authorizationCode, this.requireContext())
        SetupHolder.isAuthorized()

        attemptingToAuth = false
    }

    private fun checkBluetoothAdapter(){
        if(BluetoothAdapter.getDefaultAdapter().state == BluetoothAdapter.STATE_ON){
            SetupHolder.blueToothAdapterIsReady()
        } else {
            SetupHolder.blueToothAdapterNotReady()
        }
    }
    //Navigation
    private fun toBluetoothPairing(){
        if (navController?.currentDestination?.id == currentFragmentId) {
            readerSettingsCallbackRef?.clear()
            LoggerHelper.writeToLog("Square_Setup_Fragment_to_Bluetooth_Pairing_Fragment", LogEnums.LIFE_CYCLE.tag)
            navController?.navigate(R.id.action_bluetoothSetupFragment_to_blueToothPairingFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        if(!callBackViewModel.isReaderConnected().hasActiveObservers()){
            callBackViewModel.isReaderConnected().observe(this.viewLifecycleOwner, Observer {connected ->
                if(connected){
                    readerSettingsCallbackRef?.clear()
                    toBluetoothPairing()
                }
            })
        }

        updateAdapter(adapterTwo!!)
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