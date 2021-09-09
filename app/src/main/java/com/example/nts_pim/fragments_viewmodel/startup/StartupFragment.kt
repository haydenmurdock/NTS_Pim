package com.example.nts_pim.fragments_viewmodel.startup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetPimSettingsQuery
import com.amazonaws.amplify.generated.graphql.ResetReAuthSquareMutation
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.AdInfoHolder
import com.example.nts_pim.data.repository.PIMSetupHolder
import com.example.nts_pim.data.repository.model_objects.JsonAuthCode
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.callback.CallBackViewModel
import com.example.nts_pim.fragments_viewmodel.startup.adapter.StartupAdapter
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.Square_Service.SquareHelper
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.view_helper.ViewHelper
import com.google.gson.Gson
import com.squareup.sdk.reader.ReaderSdk
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
import type.ResetReAuthSquareInput
import java.io.IOException
import java.net.NetworkInterface
import java.util.*

class StartupFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var viewModel: VehicleSetupViewModel
    private lateinit var callBackViewModel: CallBackViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private  val fullBrightness = 255
    private var permissionDraw = false
    private var permissionWrite = false
    private var permissionAccessibility = true
    private var setupStatus = false
    private var appVersionNumber: String? = null
    private var blueToothAddress: String? = null
    private var deviceId: String? = null
    private var vehicleId: String? = null
    private var phoneNumber: String? = null
    private var adapterOne: StartupAdapter? = null
    private var adapterTwo: StartupAdapter? = null
    private var adapterThree: StartupAdapter? = null
    private val currentFragmentId = R.id.startupFragment
    private var pimSettingsAttempted = false
    private var authoringSquare = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.startup, container, false)
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)
        val factory = InjectorUtiles.provideCallBackModelFactory()
        callBackViewModel = ViewModelProvider(this, factory)
            .get(CallBackViewModel::class.java)

        mAWSAppSyncClient = ClientFactory.getInstance(requireActivity().applicationContext)
        val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
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
            getPhoneNumberPermissions()
            return
        } else {
             phoneNumber = telephonyManager.line1Number
        }

        val isSetupComplete = viewModel.isSetUpComplete()
        initRecyclerViews()
        LoggerHelper.writeToLog("Startup: is Setup Complete: $isSetupComplete", LogEnums.SETUP.tag)
        deviceId = DeviceIdCheck.getDeviceId()
        if(isSetupComplete){
                PIMSetupHolder.isPaired()
                vehicleId = viewModel.getVehicleID()
                PIMMutationHelper.sendPIMStartTime(deviceId!!, mAWSAppSyncClient!!)
        } else {
            PIMSetupHolder.isNotPaired(vehicleId)
        }
        blueToothAddress = getBluetoothAddress()
        appVersionNumber = BuildConfig.VERSION_NAME
        Log.i("Version", "$appVersionNumber")
        PIMMutationHelper.getCurrentDateFormattedDateUtcIso()?.let {
            callBackViewModel.setPIMStartTime(
                it
            )
        }
        stepOneImageButton.setOnClickListener {
            openCloseStepOneListView()
        }

        stepTwoImageButton.setOnClickListener {
            openCloseStepTwoListView()
        }

        stepThreeImageButton.setOnClickListener {
            openCloseStepThreeListView()
        }

        PIMSetupHolder.doesPIMHavePermissions().observe(
            this.viewLifecycleOwner,
            androidx.lifecycle.Observer { permissions ->
                if (permissions) {
                    updateAdapter(adapterOne!!)
                    val internetOn = isNetworkAvailable(requireContext())
                    if(internetOn){
                        PIMSetupHolder.internetIsConnected()
                    }
                }
            })
        PIMSetupHolder.doesPIMHaveInternet().observe(
            this.viewLifecycleOwner,
            androidx.lifecycle.Observer { internet ->
                if (internet) {
                    updateAdapter(adapterOne!!)
                } else {
                    updateAdapter(adapterOne!!)
                }
            })
        PIMSetupHolder.isPIMSetupAndPaired().observe(
            this.viewLifecycleOwner,
            androidx.lifecycle.Observer { paired ->
                if (paired) {
                    updateAdapter(adapterOne!!)
                }
            })

        PIMSetupHolder.isPIMSubscribedToAWS().observe(
            this.viewLifecycleOwner,
            androidx.lifecycle.Observer { subscribedAWS ->
                if (subscribedAWS) {
                    updateAdapter(adapterOne!!)
                    if (deviceId != null) {
                        getPIMSettings(deviceId!!)
                    }
                }

            })
        PIMSetupHolder.doesPIMHaveSettings().observe(
            this.viewLifecycleOwner,
            androidx.lifecycle.Observer { hasSettings ->
                if (hasSettings) {
                    updateAdapter(adapterOne!!)
                    val navController = Navigation.findNavController(
                        requireActivity(),
                        R.id.nav_host_fragment
                    )
                    checkDestinations(navController)
                }
            })
    }

    private fun updateAdapter(adapter: StartupAdapter){
        adapter.notifyDataSetChanged()
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
        val stepOneList = PIMSetupHolder.getStepOneList()
        val stepTwoList = PIMSetupHolder.getStepTwoList()
        val stepThreeList = PIMSetupHolder.getStepThreeList()
        adapterOne = StartupAdapter(requireContext(), stepOneList)
        adapterTwo = StartupAdapter(requireContext(), stepTwoList)
        adapterThree = StartupAdapter(requireContext(), stepThreeList)
    }

    private fun getPIMSettings(deviceId: String){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(requireActivity().applicationContext)
        }
        if (!pimSettingsAttempted){
            mAWSAppSyncClient?.query(GetPimSettingsQuery.builder().deviceId(deviceId).build())
                ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                ?.enqueue(startUpPIMSettingsQueryCallBack)
            pimSettingsAttempted = true
        }
    }

    private var startUpPIMSettingsQueryCallBack = object: GraphQLCall.Callback<GetPimSettingsQuery.Data>() {
        override fun onResponse(response: Response<GetPimSettingsQuery.Data>) {
            Log.i("CheckingAWS", "${response.data()}")
            LoggerHelper.writeToLog(
                "GetPimSettingsQuery at start up: ${response.data()}",
                LogEnums.SETUP.tag
            )
            if(response.data()?.pimSettings?.errorCode() == "1016"){
                LoggerHelper.writeToLog(
                    "Error code 1016. Showing Toast with error message: {${response.data()!!.pimSettings.error()}",
                    LogEnums.SETUP.tag
                )
                Toast.makeText(
                    this@StartupFragment.context,
                    "${response.data()?.pimSettings!!.error()}",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (response.data() != null &&
                !response.hasErrors()
            ) {

                val isLoggingOn = response.data()?.pimSettings?.log()
                val awsBluetoothAddress = response.data()?.pimSettings?.btAddress()
                val appVersion = response.data()?.pimSettings?.appVersion()
                val reAuth = response.data()?.pimSettings?.reAuthSquare()
                val awsPhoneNumber = response.data()?.pimSettings?.phoneNbr()
                val pimPaired = response.data()?.pimSettings?.paired() ?: true
                val awsDeviceId = response.data()?.pimSettings?.deviceId()
                val useBluetooth = response.data()?.pimSettings?.useBluetooth()
                val hasAddChanged = response.data()?.pimSettings?.adChanged() ?: false
                val addRemoved = response.data()?.pimSettings?.adRemoved() ?: false

                if (!pimPaired){
                launch {
                    callBackViewModel.pimPairingValueChangedViaFMP(pimPaired)
                     }
                }
                if(pimPaired && awsDeviceId != deviceId){
                    LoggerHelper.writeToLog(
                        "Device id didn't match from the query. Is trying to update device for v",
                        LogEnums.SETUP.tag
                    )
                    launch(Dispatchers.IO) {
                        PIMMutationHelper.updateDeviceId(
                            deviceId!!,
                            mAWSAppSyncClient!!,
                            vehicleId!!
                        )
                    }
                }

                if(isLoggingOn != null){
                    LoggerHelper.logging = isLoggingOn
                }
                if (awsBluetoothAddress.isNullOrEmpty() || awsBluetoothAddress != blueToothAddress){
                PIMMutationHelper.updatePimSettings(
                    blueToothAddress,
                    appVersionNumber,
                    phoneNumber,
                    mAWSAppSyncClient!!,
                    deviceId!!
                )
                }

                if(appVersion.isNullOrBlank() || appVersion.isNullOrEmpty() || appVersion != appVersionNumber){
                    PIMMutationHelper.updatePimSettings(
                        blueToothAddress,
                        appVersionNumber,
                        phoneNumber,
                        mAWSAppSyncClient!!,
                        deviceId!!
                    )
                }

                if(awsPhoneNumber != phoneNumber){
                    PIMMutationHelper.updatePimSettings(
                        blueToothAddress,
                        appVersionNumber,
                        phoneNumber,
                        mAWSAppSyncClient!!,
                        deviceId!!
                    )
                }
                if(useBluetooth != null){
                    LoggerHelper.writeToLog(
                        "useBluetooth value at start up: $useBluetooth",
                        LogEnums.SETUP.tag
                    )
                    if(useBluetooth == true){
                            BluetoothDataCenter.turnOnBlueTooth()
                        } else {
                            BluetoothDataCenter.turnOffBlueTooth()
                        }
                    }
                if(addRemoved) {
                    LoggerHelper.writeToLog(
                        "Ad removed was true. Trying to delete ads",
                        LogEnums.SETUP.tag
                    )
                    AdInfoHolder.deleteAds()
                }
                if(!addRemoved){
                    LoggerHelper.writeToLog(
                        "Ad removed was false. checking for internal add info",
                        LogEnums.SETUP.tag
                    )
                   AdInfoHolder.checkForInternalAddInfo()
                }
                if(hasAddChanged){
                    LoggerHelper.writeToLog(
                        "Ad changed is true. Trying to delete ads. Grabbing ad info from AWS",
                        LogEnums.SETUP.tag
                    )
                    AdInfoHolder.deleteAds()
                    PIMMutationHelper.getAdInformation(vehicleId!!)
                }
                if(reAuth != null && reAuth){
                    reauthorizeSquareFromFMP()
                } else {
                    PIMSetupHolder.pimSettingsAreUpdated()
                }
            }


        }
        override fun onFailure(e: ApolloException) {
            LoggerHelper.writeToLog(
                "Failure for Get Pim Settings Query. Request did not reach AWS. Error: $e",
                LogEnums.SETUP.tag
            )
        }
    }
    private fun reauthorizeSquareFromFMP() {
        //We don't check for a valid MAC in this instance. If we use a MAC that is already in use, it will push the authorization to the Reader Check Screen with a de-authorized reader SDK.
        ViewHelper.makeSnackbar(requireView(), "FLEET MGMT PORTAL: RE-AUTH REQUEST")
        if(!authoringSquare){
            ReaderSdk.authorizationManager().addDeauthorizeCallback {  deauthorize  ->
                if(deauthorize.isSuccess){
                    SquareHelper.deleteMac("FMP request for new MAC", requireContext())
                    getMobileAuthCode(vehicleId!!)
                }
            }
            ReaderSdk.authorizationManager().addAuthorizeCallback {result ->
                if(result.isSuccess){
                    LoggerHelper.writeToLog("Reader SDK was successfully authorized via FMP", LogEnums.SQUARE.tag)
                    PIMSetupHolder.pimSettingsAreUpdated()
                }

            }
            if (ReaderSdk.authorizationManager().authorizationState.canDeauthorize()) {
                activity?.runOnUiThread {
                    ReaderSdk.authorizationManager().deauthorize()
                }

                LoggerHelper.writeToLog(
                    "De-authorized_Square via startup function to authorize on FMP",
                    LogEnums.SQUARE.tag
                )
            }
        }
    }

    private fun getMobileAuthCode(vehicleId: String) = launch(Dispatchers.IO) {
        val dateTime = ViewHelper.formatDateUtcIso(Date())
        val url = "https://i8xgdzdwk5.execute-api.us-east-2.amazonaws.com/prod/CheckOAuthToken?vehicleId=$vehicleId&source=PIM&eventTimeStamp=$dateTime&extraInfo=REQ_VIA_FLEET_MGMT_PORTAL"
        LoggerHelper.writeToLog("Sending MAC request to: $url", LogEnums.SQUARE.tag)
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
                        LoggerHelper.writeToLog("Successfully got MAC", SquareHelper.logTag)
                        onAuthorizationCodeRetrieved(authCode, vehicleId)
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
                        launch(Dispatchers.Main.immediate) {
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
    private fun onAuthorizationCodeRetrieved(authorizationCode: String, vehicleId: String){
        SquareHelper.saveMAC(authorizationCode, this.requireContext())
        launch(Dispatchers.Main.immediate){
            ReaderSdk.authorizationManager().authorize(authorizationCode)
        }
        authoringSquare = false
        sendBackAuthMutation(vehicleId)
    }

    private fun sendBackAuthMutation(vehicleId: String) = launch(Dispatchers.IO){
        val reAuthInput = ResetReAuthSquareInput.builder().vehicleId(vehicleId).build()
          mAWSAppSyncClient?.mutate(
              ResetReAuthSquareMutation.builder().parameters(reAuthInput).build()
          )?.enqueue(reAuthCallback)

    }
    private var reAuthCallback = object : GraphQLCall.Callback<ResetReAuthSquareMutation.Data>(){
        override fun onResponse(response: Response<ResetReAuthSquareMutation.Data>) {
            if(response.hasErrors()){
                LoggerHelper.writeToLog(
                    "There was an error trying to reAuthorize square account for this vehicle. Suggest manuel ReAuth",
                    LogEnums.SETUP.tag
                )
            }

            if(response.data()?.resetReAuthSquare()?.reAuthSquare() != null &&
                response.data()?.resetReAuthSquare()?.reAuthSquare() == false){
                LoggerHelper.writeToLog(
                    "$vehicleId was successfully re-authorized",
                    LogEnums.SETUP.tag
                )
            }
        }

        override fun onFailure(e: ApolloException) {
            LoggerHelper.writeToLog(
                "There was an error trying to reAuthorize square account for this vehicle. Suggest manual ReAuth",
                LogEnums.SETUP.tag
            )
        }
    }
    private fun checkDrawPermission(): Boolean {
        val retValue = Settings.canDrawOverlays(context)
        if(!retValue){
            openDrawPermissionsMenu()
        }
        return retValue
    }


    private fun openDrawPermissionsMenu(){
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + requireContext().packageName)
        )
        startActivity(intent)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For 29 api or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->    true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->   true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->   true
                else ->     false
            }
        }
        // For below 29 api
        else {
            if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting) {
                return true
            }
        }
        return false
    }
    private fun getBluetoothAddress(): String?{
      // We will use this for BlueTooth setup with the driver tablet
        val alphabetArray = mutableListOf<String>(
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z"
        )
        var bluetoothAddress = ""
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
                val macAddress = res1.toString()
                val lastChar = macAddress.last()
                val updatedMacAddress = macAddress.dropLast(1)
                if(!alphabetArray.contains(lastChar.toString())){
                    //This means the last part was a number so we need to - by 1
                    val lastNumber = lastChar.toInt() - 1
                    bluetoothAddress = updatedMacAddress + lastNumber.toString()
                    return bluetoothAddress
                }
                for((index, letter) in alphabetArray.withIndex()){
                    if(letter == lastChar.toString()){
                       bluetoothAddress = updatedMacAddress + alphabetArray[index - 1]
                        break
                    }
                }
                return bluetoothAddress
            }
        } catch (ex: Exception) {
            com.example.nts_pim.utilities.view_helper.ViewHelper.makeSnackbar(
                this.requireView(),
                "Error getting bluetooth address: ex: $ex"
            )
        }
        return "02:00:00:00:00:00"
    }

    private fun checkSystemWritePermission(): Boolean {
        var retVal = true
        retVal = Settings.System.canWrite(context)
        if(retVal){
            // when we have permission we manually set to brightness 255
            changeScreenBrightness(fullBrightness)

        }else{
            //We don't have permissions so we push to grant permissions
            openAndroidPermissionsMenu()
        }
        return retVal
    }

    private fun openAndroidPermissionsMenu() {
        val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        i.setData(Uri.parse("package:" + requireContext().packageName))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }
    private fun getPhoneNumberPermissions(){
        val REQUEST_CODE_ASK_PERMISSIONS = 1
        val REQUIRED_SDK_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_NUMBERS
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
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                permissions,
                REQUEST_CODE_ASK_PERMISSIONS
            )
        } else {
            val grantResults = IntArray(REQUIRED_SDK_PERMISSIONS.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(
                REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                grantResults
            )
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
    private fun checkPermissions() {
        permissionDraw = checkDrawPermission()
        permissionWrite = checkSystemWritePermission()
        setupStatus = viewModel.isSetUpComplete()
    }

    private fun checkDestinations(navController: NavController){
        if (setupStatus && permissionWrite && permissionDraw && permissionAccessibility){
            viewModel.vehicleIDExists()
            if (navController.currentDestination?.id == currentFragmentId) {
              navController.navigate(R.id.bluetoothSetupFragment)
            }
        } else if(permissionDraw && permissionWrite && permissionAccessibility) {
            if (navController.currentDestination?.id == currentFragmentId) {
                navController.navigate(R.id.toVehicleSetupFragment)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (!permissionDraw || !permissionWrite || !permissionAccessibility) {
            checkPermissions()
        }
        if(permissionDraw && permissionWrite && permissionAccessibility){
            PIMSetupHolder.permissionsChecked()
        }
        if(!setupStatus){
            checkDestinations(navController)
        }
    }
}