package com.example.nts_pim.fragments_viewmodel.startup

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetPimSettingsQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.BuildConfig
import com.example.nts_pim.R
import com.example.nts_pim.data.repository.model_objects.DeviceID
import com.example.nts_pim.data.repository.providers.ModelPreferences
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.enums.SharedPrefEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import com.example.nts_pim.utilities.power_cycle.PowerAccessibilityService
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.net.NetworkInterface
import java.util.*


class StartupFragment: ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: VehicleSetupModelFactory by instance()
    private lateinit var viewModel: VehicleSetupViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private val fullBrightness = 255
    private var permissionDraw = false
    private var permissionWrite = false
    private var permissionAccessibility = false
    private var setupStatus = false
    private var navController: NavController? = null
    private var appVersionNumber: String? = null
    private var blueToothAddress: String? = null
    private var deviceId: String? = null
    private val currentFragmentId = R.id.startupFragment


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.startup, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(VehicleSetupViewModel::class.java)
        navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        mAWSAppSyncClient = ClientFactory.getInstance(activity!!.applicationContext)
        val isSetupComplete = viewModel.isSetUpComplete()
        if(isSetupComplete){
            val deviceId = ModelPreferences(context!!).getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)
            if(deviceId != null && deviceId.number.isNotBlank()){
                Log.i("LOGGER", "Vehicle Setup complete and checking AWS For Logging. device Id: ${deviceId.number}")
                checkAWSForLogging(deviceId.number)
            }
        }
        blueToothAddress = getBluetoothAddress()
        appVersionNumber = BuildConfig.VERSION_NAME
        deviceId = ModelPreferences(this.requireContext()).getObject(SharedPrefEnum.DEVICE_ID.key, DeviceID::class.java)?.number
    }

    private fun checkAWSForLogging(deviceId: String){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(activity!!.applicationContext)
        }
        mAWSAppSyncClient?.query(GetPimSettingsQuery.builder().deviceId(deviceId).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_FIRST)
            ?.enqueue(awsLoggingQueryCallBack)
    }

    private var awsLoggingQueryCallBack = object: GraphQLCall.Callback<GetPimSettingsQuery.Data>() {
        override fun onResponse(response: Response<GetPimSettingsQuery.Data>) {

            if (response.data() != null &&
                !response.hasErrors()
            ) {
                val isLoggingOn = response.data()?.pimSettings?.log()
                val awsBluetoothAddress = response.data()?.pimSettings?.btAddress()
                val appVersion = response.data()?.pimSettings?.appVersion()

                if(isLoggingOn != null){
                    Log.i("LOGGER", "AWS Query callback: isLoggingOn = $isLoggingOn")
                    LoggerHelper.logging = isLoggingOn
                }
                if (awsBluetoothAddress.isNullOrBlank() || awsBluetoothAddress != blueToothAddress){
                PIMMutationHelper.updatePimSettings(blueToothAddress, null, mAWSAppSyncClient!!,deviceId!!)
                }

                if(appVersion.isNullOrBlank() || appVersion.isNullOrEmpty()){
                    PIMMutationHelper.updatePimSettings(null, appVersionNumber, mAWSAppSyncClient!!, deviceId!!)
                }
            }
        }
        override fun onFailure(e: ApolloException) {
        }
    }
    private fun checkAccessibilityPermission():Boolean {
        val retVal = PowerAccessibilityService().isAccessibilitySettingsOn(context!!)
        if(!retVal){
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        return retVal
    }
    private fun checkDrawPermission(): Boolean {
        val retValue = Settings.canDrawOverlays(context)
        if(!retValue){
            openDrawPermissionsMenu()
        }
        return retValue
    }

    private fun openDrawPermissionsMenu(){
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context!!.getPackageName()))
        startActivity(intent)
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
            com.example.nts_pim.utilities.view_helper.ViewHelper.makeSnackbar(this.view!!, "Error getting bluetooth address: ex: $ex")
        }
        return "02:00:00:00:00:00"
    }

    private fun checkSystemWritePermission(): Boolean {
        var retVal = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(context)
            if(retVal){
                // when we have permission we manually set to brightness 255
                changeScreenBrightness(fullBrightness)

            }else{
                //We don't have permissions so we push to grant permissions
                openAndroidPermissionsMenu()
            }
        }
        return retVal
    }

    private fun openAndroidPermissionsMenu() {
        val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        i.setData(Uri.parse("package:" + context!!.getPackageName()))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private fun changeScreenBrightness(screenBrightness: Int) {
        Settings.System.putInt(
            context!!.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )  //this will set the manual mode (set the automatic mode off)
        Settings.System.putInt(
            context!!.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            screenBrightness
        )  //this will set the brightness to maximum (255)

        //refreshes the screen
        val br =
            Settings.System.getInt(context!!.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        val lp = activity!!.window.attributes
        lp.screenBrightness = br.toFloat() / 255
        activity!!.window.attributes = lp
    }
    private fun checkPermissions() {
        permissionDraw = checkDrawPermission()
        permissionWrite = checkSystemWritePermission()
        permissionAccessibility = checkAccessibilityPermission()
        setupStatus = viewModel.isSetUpComplete()
    }

    private fun checkDestinations(navController: NavController){
        if (setupStatus && permissionWrite && permissionDraw && permissionAccessibility){
            viewModel.vehicleIDExists()
            if (navController.currentDestination?.id == currentFragmentId) {
                navController.navigate(R.id.toWelcomeScreenFromStartUP)
            }
        } else if(permissionDraw && permissionWrite && permissionAccessibility) {
            if (navController.currentDestination?.id == currentFragmentId) {
                navController.navigate(R.id.toVehicleSetupFragment)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!permissionDraw || !permissionWrite || !permissionAccessibility) {
            checkPermissions()
        } else if (permissionWrite && permissionDraw && permissionAccessibility){
            checkDestinations(navController!!)
        }
        checkDestinations(navController!!)
    }
}