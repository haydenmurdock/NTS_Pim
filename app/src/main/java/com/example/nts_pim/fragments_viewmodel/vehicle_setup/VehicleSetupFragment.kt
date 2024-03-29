package com.example.nts_pim.fragments_viewmodel.vehicle_setup


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Adapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetPimSettingsQuery
import com.amazonaws.amplify.generated.graphql.UpdatePimSettingsMutation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.model_objects.*
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.utilities.Square_Service.SquareHelper
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.dialog_composer.PIMDialogComposer
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.keyboards.QwertyKeyboard
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.sound_helper.SoundHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.github.ybq.android.spinkit.style.DoubleBounce
import com.github.ybq.android.spinkit.style.ThreeBounce
import com.google.gson.Gson
import com.squareup.sdk.reader.ReaderSdk
import com.squareup.sdk.reader.authorization.AuthorizationManager
import com.squareup.sdk.reader.authorization.AuthorizeErrorCode
import com.squareup.sdk.reader.authorization.Location
import com.squareup.sdk.reader.core.CallbackReference
import com.squareup.sdk.reader.core.Result
import com.squareup.sdk.reader.core.ResultError
import com.squareup.sdk.reader.hardware.ReaderSettingsErrorCode
import kotlinx.android.synthetic.main.vehicle_setup.*
import kotlinx.android.synthetic.main.vehicle_setup.qwertyKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import type.UpdatePIMSettingsInput
import java.io.IOException
import java.lang.Error
import java.net.NetworkInterface
import java.util.*

class VehicleSetupFragment:ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var viewModel: VehicleSetupViewModel
    private lateinit var keyboardViewModel: SettingsKeyboardViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private var authorizeCallbackRef: CallbackReference? = null
    private var readerSettingsCallbackRef: CallbackReference? = null
    private val authManager = ReaderSdk.authorizationManager()
    private val setNamesArray =
        arrayOf("UUID", "PIN", "Vehicle ID", "Authorization","Knox Startup", "Square Connection")
    private val setBooleanArray = arrayOf(false, false, false, false, false,false)
    private var deviceId = ""
    private var androidId = ""
    private var pin = ""
    private var vehicleID = ""
    private var authCode = ""
    private var authorized = false
    private var isSquareAuthorized = false
    private val currentFragment = R.id.vehicleSetupFragment
    private val alreadyAuthString = "authorize_already_authorized"
    private var checkedDeviceId = false
    private var checkedAndroidId = false
    var pinEntered = false
    var incorrectPin = ""
    var adapter: Adapter? = null
    var doesVehicleIdExist = false
    var appVersion: String? = null
    var blueToothAddress: String? = null
    var stopSquare = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.vehicle_setup, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MyCustomAdapter(requireContext(), setNamesArray, setBooleanArray)
        listView.adapter = adapter as MyCustomAdapter
        authorizeCallbackRef =
            authManager.addAuthorizeCallback(this::onAuthorizeResult)
        val readerManager = ReaderSdk.readerManager()
        readerSettingsCallbackRef =
            readerManager.addReaderSettingsActivityCallback(this::onReaderSettingsResult)
        mAWSAppSyncClient = ClientFactory.getInstance(requireContext())
        val keyboardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)
        keyboardViewModel = ViewModelProvider(this, keyboardFactory)
            .get(SettingsKeyboardViewModel::class.java)
        setUpKeyboard()
        allOtherPermissions()
        val doubleBounce = DoubleBounce()
        auth_progressBar.setIndeterminateDrawable(doubleBounce)
        val threeBounce = ThreeBounce()
        vehicle_id_progressBar.setIndeterminateDrawable(threeBounce)
        keyboardViewModel.isQwertyKeyboardUp().observe(this.viewLifecycleOwner, Observer {
            //Once a pin has been created we will try to pull the vehicle_Id from AWS
            if (it)
                pinEntered = true
            if (!it && pinEntered) {
                showUIForEnterPIN()
                keyboardViewModel.qwertyKeyboardIsUp()
            }
        })
        doesVehicleIdExist = viewModel.doesVehicleIDExist()
            if (!doesVehicleIdExist) {
                checkDeviceID(view, adapter as MyCustomAdapter, this.requireActivity())
                checkForPin(adapter as MyCustomAdapter)
            } else {
                checkDeviceID(view, adapter as MyCustomAdapter, this.requireActivity())
                showUIForSavedVehicleID()
                checkAuthorization(vehicleID, requireActivity().parent as MainActivity)
            }

        viewModel.isThereAuthCode().observe(this.viewLifecycleOwner, Observer {
            if (it) {
                auth_progressBar.isVisible = true
                auth_progressBar.animate()
                retrieveAuthorizationCode(authCode)
            }
        })

        viewModel.isSquareAuthorized().observe(this.viewLifecycleOwner, Observer {
            isSquareAuthorized = it
            if (it) {
                updateChecklist(4, true, adapter as MyCustomAdapter)
                startReaderSettings(adapter as MyCustomAdapter)
                showUIForSquareAuthorizationSuccess()
            }
        })

        viewModel.isPinEnteredWrong().observe(this.viewLifecycleOwner, Observer {
            val isPinWrong = it
            if (isPinWrong) {
                showUIForWrongPin()
                showErrorToastPinEnteredIncorrect()
                viewModel.pinWasEnteredWrong()
            }
        })

        setup_complete_btn.setOnClickListener {
            //This is for if there needs to be a check auth because its the first one in fleet.
            if(authorized) {
                setUpComplete()
                SoundHelper.turnOnSound(requireContext())
                readerSettingsCallbackRef?.clear()
                toCheckVehicleInfo()
            } else {
                setup_detail_text_view.isVisible = false
                setup_complete_btn.isVisible = false
                auth_progressBar.animate()
                checkAuthorization(vehicleID, requireActivity().parent as MainActivity)
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun checkDeviceID(view: View, adapter: MyCustomAdapter, activity: FragmentActivity) = launch {
        deviceId = viewModel.getDeviceID.await()
        // This in case there is no device ID, we make one
        if (deviceId == "") {
            if(context?.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val deviceId = Settings.Secure.getString(requireActivity().contentResolver,
                    Settings.Secure.ANDROID_ID)
                if (deviceId != null) {
                    val deviceIdObject =
                        DeviceID(deviceId)
                    ModelPreferences(view.context)
                        .putObject(SharedPrefEnum.DEVICE_ID.key, deviceIdObject)
                    this@VehicleSetupFragment.deviceId = deviceIdObject.number
                    setup_detail_text_view.text =
                        "imei was created and updated: ${this@VehicleSetupFragment.deviceId}"
                    updateChecklist(0, true, adapter)
                    launch(Dispatchers.IO) {
                        checkForPairedVehicleID(deviceIdObject.number)
                    }
                    Log.i(
                        LogEnums.PIM_SETTING.tag,
                        "Device Id: $deviceIdObject saved to Shared Preferences"
                    )
                } else {
                    PIMDialogComposer.androidVersionNotSupported(activity)
                }
            }
        } else {
            setup_detail_text_view.text = "Device id was found $deviceId"
            updateChecklist(0, true, adapter)
            launch(Dispatchers.IO) {
                checkForPairedVehicleID(deviceId)
            }
            Log.i(
                LogEnums.PIM_SETTING.tag,
                "Device Id: Already saved into preferences and is $deviceId"
            )
        }
    }

    private fun checkForPin(adapter: MyCustomAdapter) = launch {
        pin = viewModel.getPin.await()
        if (pin == "") {
            //There is no pin so we will need to show the pin screen
            keyboardViewModel.qwertyKeyboardIsUp()
            setup_detail_text_view.text = "No pin in settings"
            setup_detail_scrollView.isVisible = false
            pin_detail_scrollView.isVisible = true
        } else {
            // There is a pin found so we will get the Vehicle ID
            setup_detail_text_view.text = "Pin: $pin found in settings"
            updateChecklist(1, true, adapter)
            if (deviceId != "") {
                queryVehicleIdWithPIN(deviceId, pin, adapter)
            }
        }
    }

    private fun createPin(enteredPin: String, adapter: MyCustomAdapter) {
        val pin = PIN(enteredPin)
        // Puts pin into preferences and updates listView
        ModelPreferences(requireContext())
            .putObject(SharedPrefEnum
                .PIN_PASSWORD.key,
                pin)
        launch(Dispatchers.Main.immediate) {
            updateChecklist(1, true, adapter)
            showVehicleIDToast(vehicleID)
        }
    }
    private fun pinIsWrong(pin: String) {
        incorrectPin = pin
        launch(Dispatchers.Main.immediate) {
            viewModel.pinWasEnteredWrong()
        }
    }

    private fun queryVehicleIdWithPIN(deviceID: String, pin: String, adapter: MyCustomAdapter)
            = launch(Dispatchers.IO) {
        val pimSettingsQueryCallBack = object : GraphQLCall.Callback<GetPimSettingsQuery.Data>() {
            @SuppressLint("MissingPermission", "HardwareIds")
            override fun onResponse(response: Response<GetPimSettingsQuery.Data>) {
                val callBackVehicleID = response.data()?.pimSettings?.vehicleId()
                Log.i("VehicleIdQuery", "response: ${response.data()}")
                when (response.data()?.pimSettings?.errorCode()) {
                    "1004" -> {
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "PIM device Id has been assigned to another vehicle",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    "1016" -> {
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${response.data()!!.pimSettings.error()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                if(callBackVehicleID != null) {
                    vehicleID = callBackVehicleID
                    val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val phoneNumber = telephonyManager.line1Number
                    updatePimSettings(blueToothAddress,appVersion,phoneNumber,mAWSAppSyncClient!!, deviceId)
                    createPin(pin, adapter)
                }
                if (callBackVehicleID == null) {
                    pinIsWrong(pin)
                }
            }

            override fun onFailure(e: ApolloException) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
            mAWSAppSyncClient!!.query(GetPimSettingsQuery.builder().deviceId(deviceID).pin(pin).build())
            ?.responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
            ?.enqueue(pimSettingsQueryCallBack)
    }

    @SuppressLint("HardwareIds")
    private fun checkForPairedVehicleID(deviceID: String){
        if (deviceID == deviceId){
            //This means that the vehicle has not been unpaired. We will need to get the vehicle Id and skip the pin.
            checkedDeviceId = true
            appVersion = BuildConfig.VERSION_NAME
            blueToothAddress = getBluetoothAddress()
            mAWSAppSyncClient!!.query(GetPimSettingsQuery.builder().deviceId(deviceID).build())
                ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                ?.enqueue(vehicleIdQueryCallBack)
        } else {
            if (!checkedAndroidId){
                checkedAndroidId = true
                mAWSAppSyncClient!!.query(GetPimSettingsQuery.builder().deviceId(deviceID).build())
                    ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    ?.enqueue(vehicleIdQueryCallBack)
            }
        }
    }
    private val vehicleIdQueryCallBack = object : GraphQLCall.Callback<GetPimSettingsQuery.Data>() {
        @SuppressLint("MissingPermission", "HardwareIds")
        override fun onResponse(response: Response<GetPimSettingsQuery.Data>) {
            Log.i("vehicleIdQuery", "reponse: ${response.data()}")
            val callBackVehicleID = response.data()?.pimSettings?.vehicleId()
            when(response.data()?.pimSettings?.errorCode()){
                    "1014" -> {
                           //this is where we would check for an android Id as well.
                        }
                    }
            if(!response.hasErrors()){
                if (callBackVehicleID != null) {
                    vehicleID = callBackVehicleID
                    val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val phoneNumber = telephonyManager.line1Number
                    updatePimSettings(blueToothAddress,appVersion,phoneNumber,mAWSAppSyncClient!!, deviceId)
                    checkAuthorization(vehicleID, requireActivity())
                    if(checkedAndroidId){
                        updateDeviceId(deviceId,vehicleID)
                    }
                }
            }
            if(response.hasErrors()){
                Log.i("VehicleIdError", "Error when getting vehicleId. ${response.errors()}")
            }
        }
        override fun onFailure(e: ApolloException) {
            Log.i("VehicleSetup", "Response for getting device ID has Apollo Exception. Error: ${e.message}")
        }
    }
    @SuppressLint("HardwareIds")
    private fun updateDeviceId(deviceId: String, vehicleId: String){
        PIMMutationHelper.updateDeviceId(deviceId,mAWSAppSyncClient!!, vehicleId)
    }
    private fun getBluetoothAddress(): String?{
        // We will use this for BlueTooth setup with the driver tablet
        try {
            val all: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.name.equals("wlan0", true)) continue
                val macBytes: ByteArray = nif.hardwareAddress ?: return ""
                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }
                if (res1.isNotEmpty()) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: Exception) {
           ViewHelper.makeSnackbar(this.requireView(), "Error getting bluetooth address: ex: $ex")
        }
        return "02:00:00:00:00:00"
    }
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun saveVehicleID(vehicleId: String) {
        val vehicleID = VehicleID(vehicleId)
        ModelPreferences(requireContext()).putObject(SharedPrefEnum.VEHICLE_ID.key, vehicleID)
        launch(Dispatchers.Main.immediate) {
            viewModel.vehicleIDExists()
            showUIForSavedVehicleID()
            checkAuthorization(vehicleId, requireActivity())
        }

        Log.i(
            LogEnums.PIM_SETTING.tag,
            "Vehicle ID: ${vehicleID.vehicleID} saved to System Preferences"
        )
    }
    private fun checkAuthorization(vehicleId: String, activity: Activity) = launch(Dispatchers.IO) {
        if(ReaderSdk.authorizationManager().authorizationState.canDeauthorize()){
            ReaderSdk.authorizationManager().deauthorize()
            LoggerHelper.writeToLog("Reader was de-authorized", SquareHelper.logTag)
        }
        val isLastMACExpired = SquareHelper.isMACExpired(requireActivity().applicationContext)
        if(isLastMACExpired == null || isLastMACExpired){
            LoggerHelper.writeToLog("MAC was expired or didn't exist. Getting NEW MAC for authorization.", LogEnums.SQUARE.tag)
           getMobileAuthCode(vehicleId)
        } else {
            val lastMACid = SquareHelper.getLastMAC(requireActivity().applicationContext)?.getId() ?: ""
            LoggerHelper.writeToLog("Last mac id was less than 1 hour old. Using OLD MAC for authorization. Id: $lastMACid", LogEnums.SQUARE.tag)
            if(lastMACid != ""){
               activity.runOnUiThread {
                    ReaderSdk.authorizationManager().authorize(lastMACid)
                    viewModel.squareIsAuthorized()
                }
            } else {
                LoggerHelper.writeToLog("Last mac id was less than 1 hour old, but MAC wasn't formatted correctly. Getting new MAC", LogEnums.SQUARE.tag)
               getMobileAuthCode(vehicleId)
            }
        }
    }
    private fun retrieveAuthorizationCode(authCode: String) = launch {
        onAuthorizationCodeRetrieved(authCode)
    }
    private fun onAuthorizationCodeRetrieved(authorizationCode: String)
            = launch {
        ReaderSdk.authorizationManager().authorize(authorizationCode)
        SquareHelper.saveMAC(authorizationCode, requireActivity().applicationContext)
    }
    //onResults
    private fun onAuthorizeResult(result: Result<Location, ResultError<AuthorizeErrorCode>>) {
        if (result.isSuccess) {
            showAuthorizationToast()
            launch(Dispatchers.Main.immediate) {
                viewModel.squareIsAuthorized()
            }
        } else {
            val error = result.error
            if (error.code == AuthorizeErrorCode.NO_NETWORK) {
                showErrorToastNoNetworkToast()
            }
            if (error.code == AuthorizeErrorCode.USAGE_ERROR) {
                if (error.debugCode == alreadyAuthString) {
                    showErrorToastAlreadyAuthorized()
                    launch(Dispatchers.Main.immediate) {
                        viewModel.squareIsAuthorized()
                    }
                }
            }
        }
    }
    private fun onReaderSettingsResult(result: Result<Void, ResultError<ReaderSettingsErrorCode>>) {
        if (result.isError) {
            val error = result.error
            when (error.code) {
                ReaderSettingsErrorCode.SDK_NOT_AUTHORIZED ->
                    showErrorToastNotAuthorized()
               ReaderSettingsErrorCode.USAGE_ERROR ->
                    showErrorToastUsageReader(error)
            }
        }

        if(result.isSuccess){
            if(setup_complete_btn != null){
                checkIfBlueToothReaderIsConnected()
            }
        }
    }
    private fun getMobileAuthCode(vehicleId: String) {
        val dateTime = ViewHelper.formatDateUtcIso(Date())
        val url = "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleId&source=PIM&eventTimeStamp=2021-08-26T$dateTime&extraInfo=PIM_VEHICLE_PAIRING"
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
                        authCode = convertedObject.authCode
                        launch(Dispatchers.Main.immediate) {
                            viewModel.successfulAuthCode()
                        }
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
                        launch {
                            goToAuthWebView(vehicleId)
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
    //UI updates
    private fun showUIForEnterPIN(){
        val enteredPin = enter_pin_editText.text.toString()
        pin = enteredPin
        vehicle_id_progressBar.isVisible = true
        vehicle_id_progressBar.animate()
        queryVehicleIdWithPIN(deviceId, pin, adapter as MyCustomAdapter)
        pinEntered = false
    }
    private fun showUIForAuthWebView(){
        pin_detail_scrollView.isVisible = false
        setup_detail_scrollView.isVisible = true
        setup_complete_btn.isVisible = true
        setup_complete_btn.isClickable = true
        setup_detail_text_view.text =
            "First Vehicle in Fleet for Authorization. Get Authorization"
        setup_complete_btn.text = "Authorization"
    }
    private fun showUIForSavedVehicleID() = launch(Dispatchers.Main.immediate){
        updateChecklist(0, true, adapter as MyCustomAdapter)
        updateChecklist(1, true, adapter as MyCustomAdapter)
        updateChecklist(2, true, adapter as MyCustomAdapter)
        vehicle_id_progressBar.isVisible = false
        pin_detail_scrollView.isVisible = false
        setup_detail_scrollView.isVisible = true
        setup_detail_text_view.text = ""
    }
    private fun showUIForWrongPin(){
        enter_pin_editText.setText("")
        vehicle_id_progressBar.isVisible = false
        vehicle_id_progressBar.clearAnimation()
    }
    private fun showUIForSquareAuthorizationSuccess(){
        updateChecklist(3, true, adapter as MyCustomAdapter)
        pin_detail_scrollView.isVisible = false
        setup_detail_scrollView.isVisible = true
        setup_complete_btn.isVisible = false
        setup_complete_btn.isEnabled = false
        setup_complete_btn.isVisible = true
        setup_complete_btn.text = "DONE"
        authorized = true
        auth_progressBar.isVisible = false
        auth_progressBar.clearAnimation()
        setup_detail_text_view.text = "Bluetooth setup complete"
        setup_detail_text_view.isVisible = true
    }
    private fun setUpWebView(vehicleID: String) {
        val url =
            "https://connect.squareup.com/oauth2/authorize?client_id=sq0idp-HJ_x8bWoeez4H7q5Cfnjug&scope=MERCHANT_PROFILE_READ+PAYMENTS_WRITE_IN_PERSON+PAYMENTS_READ&state=$vehicleID&session=false"
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browser)
        Log.i("URL", "$url")
    }
    //Toasts
    private fun showVehicleIDToast(vehicleID: String){
        if(!vehicleID.isBlank()){
            val toast =
                Toast.makeText(context,
                    "Connected to $vehicleID"
                    , Toast.LENGTH_LONG)
            toast.setGravity(Gravity.TOP, 125, 0)
            toast.show()
        }
    }
    private fun showAuthorizationToast(){
        if(!stopSquare){
            val toast =
                Toast.makeText(context,
                    "Successful authorization of Square"
                    , Toast.LENGTH_LONG)
            toast.setGravity(Gravity.TOP, 125, 0)
            toast.show()
        }
    }
    private fun showErrorToastNotAuthorized(){
        Toast.makeText(
        requireContext(),
        "SDK not authorized", Toast.LENGTH_LONG)
            .show()
    }
    private fun showErrorToastAlreadyAuthorized(){
        if(!stopSquare){
            Toast.makeText(context,
                "Square SDK already authorized for $vehicleID",
                Toast.LENGTH_LONG)
                .show()
        }
    }
    private fun showErrorToastUsageReader(error: ResultError<ReaderSettingsErrorCode>){
        Toast.makeText(
            requireContext(),
            "Usage error: ${error.message}",
            Toast.LENGTH_LONG
        ).show()
    }
    private fun showErrorToastNoNetworkToast(){
        Toast.makeText(
            requireContext(),
            "No Network",
            Toast.LENGTH_LONG

        ).show()
    }
    private fun showErrorToastPinEnteredIncorrect(){
        val toast = Toast.makeText(context,
            "$incorrectPin is incorrect",
            Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 145, 0)
        toast.show()
    }
    private fun startReaderSettings(adapter: MyCustomAdapter) {
        if(!stopSquare){
            if(setup_complete_btn != null){
                setup_complete_btn.isVisible = true
            }
            val readerManager = ReaderSdk.readerManager()
            readerManager.startReaderSettingsActivity(requireContext())
            updateChecklist(4, true, adapter)
        }

    }
    private fun setUpKeyboard() {
        enter_pin_editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        enter_pin_editText.setTextIsSelectable(false)

        val qwertyKeyboard = qwertyKeyboard as QwertyKeyboard
        val ic = enter_pin_editText.onCreateInputConnection(EditorInfo())
        qwertyKeyboard.setInputConnection(ic)
        ViewHelper.hideSystemUI(requireActivity())
    }
    private fun updateChecklist(position: Int, boolean: Boolean, adapter: MyCustomAdapter) {
        setBooleanArray[position] = boolean
        adapter.notifyDataSetChanged()
    }
    private fun requestMic() {
        val REQUEST_RECORD_AUDIO_PERMISSION = 200
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }
    private fun requestStorage(){
        val REQUEST_STORAGE_PERMSSION_CODE = 1
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMSSION_CODE)
        }
    }
    private fun requestLocation() {
        val REQUEST_FINE_LOCATION_PERMSSION_CODE = 1
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FINE_LOCATION_PERMSSION_CODE)
        }
    }
    @SuppressLint("HardwareIds")
    private fun requestPhoneState(activity: FragmentActivity){
        val REQUEST_PHONE_STATE_PERMSSION_CODE = 1
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_PHONE_STATE_PERMSSION_CODE)
        } else {
            // We need phoneStatePermission to get Android Id.
            androidId = Settings.Secure.getString(requireActivity().contentResolver,
                Settings.Secure.ANDROID_ID)
            if(!isSquareAuthorized){
                checkDeviceID(this.requireView(), adapter as MyCustomAdapter, activity)
            }
        }
    }
    private fun allOtherPermissions(){
        val REQUEST_CODE_ASK_PERMISSIONS = 1
        val REQUIRED_SDK_PERMISSIONS = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            //Manifest.permission.REQUEST_INSTALL_PACKAGES,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.EXPAND_STATUS_BAR,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_PHONE_STATE
        )

        val missingPermissions = ArrayList<String>()
        // check all required dynamic permissions
        for (permission in REQUIRED_SDK_PERMISSIONS) {
            val result = ContextCompat.checkSelfPermission(requireContext(), permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            // request all missing permissions
            val permissions = missingPermissions
                .toTypedArray()
            ActivityCompat.requestPermissions(this.requireActivity(), permissions, REQUEST_CODE_ASK_PERMISSIONS)
        } else {
            val grantResults = IntArray(REQUIRED_SDK_PERMISSIONS.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(
                REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                grantResults)
        }
    }
    @SuppressLint("MissingPermission")
    private fun checkIfBlueToothReaderIsConnected(){
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val connectedDevices = mBluetoothAdapter.bondedDevices
        val squareReaderString = "Square"
        var blueToothDevice: BluetoothDevice? = null
        if (!mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable()
        }
        for (i in connectedDevices){
            Log.i("BlueTooth", "Device connected: ${i.name}")
            if (i.name.contains(squareReaderString)){
                blueToothDevice = i
            }
        }
        if (blueToothDevice != null){
            if (blueToothDevice.name.contains(squareReaderString)){
                Log.i("BlueTooth", "Square Reader is Connected")
                if (setup_complete_btn != null && !stopSquare){
                    launch(Dispatchers.Main.immediate) {
                        setup_complete_btn.isEnabled = true
                        updateChecklist(5,true, adapter as MyCustomAdapter)
                    }
                }
            } else {
                launch(Dispatchers.Main) {
                    startReaderSettings(adapter as MyCustomAdapter)
                }
             }
        } else{ launch(Dispatchers.Main) {
                startReaderSettings(adapter as MyCustomAdapter)
            }
        }
    }
    private fun updatePimSettings(
        blueToothAddress: String?,
        appVersion: String?,
        phoneNumber: String?,
        appSyncClient: AWSAppSyncClient,
        deviceId: String
    ) {
        val newDeviceId = DeviceIdCheck.getDeviceId() ?: ""
        if (newDeviceId != "") {
            val updatePimSettings = UpdatePIMSettingsInput
                .builder()
                .deviceId(newDeviceId)
                .phoneNbr(phoneNumber)
                .appVersion(appVersion)
                .btAddress(blueToothAddress)
                .build()
            appSyncClient.mutate(
                UpdatePimSettingsMutation.builder().parameters(updatePimSettings).build()
            )?.enqueue(pimSettingsCallback)
            LoggerHelper.writeToLog("Pim Mutation Helper: update pim settings: blueToothAddress: $blueToothAddress: AppVersion: $appVersion phoneNumber: $phoneNumber: deviceId:$deviceId to aws", LogEnums.TRIP_STATUS.tag)
        }
    }

    private val pimSettingsCallback =
        object : GraphQLCall.Callback<UpdatePimSettingsMutation.Data>(){
            override fun onResponse(response: Response<UpdatePimSettingsMutation.Data>) {
                if (response.data()?.updatePIMSettings()?.error() != null) {
                    when(response.data()?.updatePIMSettings()?.errorCode()){
                        "1016" -> {
                            val errorMessage = response.data()?.updatePIMSettings()!!.error() ?: "Error for PIM Phone number"
                            launch(Dispatchers.Main) {
                                stopSquare = true
                                if(setup_complete_btn != null){
                                    setup_complete_btn.isVisible = false
                                    setup_complete_btn.isEnabled = false
                                }
                                PIMDialogComposer.wrongPhoneNumberForPIM(activity!!, errorMessage, viewModel, keyboardViewModel, vehicleID, mAWSAppSyncClient!!)
                            }

                            LoggerHelper.writeToLog("Failed to update pim settings with phone number. Error: $errorMessage", LogEnums.ERROR.tag)
                        }
                    }
                }
                saveVehicleID(vehicleID)
                LoggerHelper.writeToLog("PIM Settings Response: ${response.data()?.updatePIMSettings().toString()}", LogEnums.TRIP_STATUS.tag)
            }

            override fun onFailure(e: ApolloException) {
                Log.i("Response", "response: $e")
            }
        }
    private fun setUpComplete(){
        val setUpStatus = SetupComplete(true)
        ModelPreferences(requireContext()).
            putObject(
                SharedPrefEnum.SETUP_COMPLETE.key,
                setUpStatus)
    }
    private fun goToAuthWebView(vehicleID: String) {
        showUIForAuthWebView()
        setUpWebView(vehicleID)
    }
    private fun toCheckVehicleInfo(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragment){
            navController.navigate(R.id.toCheckVehicleInfoFragment)
        }
    }
    override fun onResume() {
        super.onResume()
        requestMic()
        requestStorage()
        requestLocation()
        requestPhoneState(requireActivity())
        ViewHelper.hideSystemUI(requireActivity())
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    override fun onPause() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        super.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        if(this::keyboardViewModel.isInitialized){
            keyboardViewModel.isQwertyKeyboardUp().removeObservers(this)
        }
        if(this::viewModel.isInitialized){
            viewModel.isSquareAuthorized().removeObservers(this)
            viewModel.isThereAuthCode().removeObservers(this)
        }
        if (auth_progressBar != null){
            auth_progressBar.clearAnimation()
        }
        if (vehicle_id_progressBar != null){
            vehicle_id_progressBar.clearAnimation()
        }
    }
}

