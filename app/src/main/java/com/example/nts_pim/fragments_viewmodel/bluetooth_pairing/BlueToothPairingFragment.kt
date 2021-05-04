package com.example.nts_pim.fragments_viewmodel.bluetooth_pairing

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.amazonaws.amplify.generated.graphql.GetPimSettingsQuery
import com.amazonaws.amplify.generated.graphql.GetStatusQuery
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.nts_pim.R
import com.example.nts_pim.activity.MainActivity
import com.example.nts_pim.data.repository.UpfrontPriceViewModel
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.fragments_viewmodel.InjectorUtiles
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_settings.setting_keyboard_viewModels.SettingsKeyboardViewModel
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.dialog_composer.PIMDialogComposer
import com.example.nts_pim.utilities.enums.LogEnums
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
import com.example.nts_pim.utilities.mutation_helper.PIMMutationHelper
import kotlinx.android.synthetic.main.fragment_blue_tooth_pairing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.lang.IllegalStateException
import kotlin.time.milliseconds

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class BlueToothPairingFragment : ScopedFragment(), KodeinAware {
    override val kodein by closestKodein()
    private val logTag = "Bluetooth_Pairing_Fragment"
    private var navController: NavController? = null
    private val viewModelFactory: VehicleSetupModelFactory by instance<VehicleSetupModelFactory>()
    private lateinit var upfrontPriceViewModel: UpfrontPriceViewModel
    private var mAWSAppSyncClient: AWSAppSyncClient? = null
    private lateinit var viewModel: VehicleSetupViewModel
    private val currentFragmentId = R.id.blueToothPairingFragment
    private var vehicleId: String? = null
    private var isBluetoothOn:Boolean? = null
    private var deviceId: String? = null
    private var noBTConnectionTimer: CountDownTimer? = null
    private var noBTView: View? = null
    private var viewGroup: ViewGroup? = null
    private var isDriverSignedIn = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blue_tooth_pairing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this, viewModelFactory).get(VehicleSetupViewModel::class.java)
        val factory = InjectorUtiles.provideUpFrontPriceFactory()
        upfrontPriceViewModel = ViewModelProvider(this, factory)
            .get(UpfrontPriceViewModel::class.java)
        isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value
        vehicleId = viewModel.getVehicleID()
        deviceId = DeviceIdCheck.getDeviceId() ?: ""
        mAWSAppSyncClient = ClientFactory.getInstance(context)
        VehicleTripArrayHolder.updateInternalPIMStatus(PIMStatusEnum.PIM_PAIRING.status)
        if(PIMMutationHelper.stopPimSetup){
            val error = PIMMutationHelper.pimError
            error?.message?.let {
                val keyboardFactory = InjectorUtiles.provideSettingKeyboardModelFactory()
               val keyboardViewModel = ViewModelProvider(this, keyboardFactory)
                    .get(SettingsKeyboardViewModel::class.java)
                PIMDialogComposer.wrongPhoneNumberForPIM(this.requireActivity(),
                    it, viewModel, keyboardViewModel, vehicleId!!,mAWSAppSyncClient!! )
            }
        }
        if(!isBluetoothOn!!){
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is off. Going to welcome screen", LogEnums.BLUETOOTH.tag)
            toWelcomeScreen()
        } else {
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is on. Starting pairing process", LogEnums.BLUETOOTH.tag)
            startBluetoothConnectionTimer()
            checkIfDriverIsSignedIn(vehicleId!!)
            getDriverTabletBluetoothAddress(deviceId!!)
        }

        BluetoothDataCenter.isConnectedToDriverTablet().observe(this.viewLifecycleOwner, Observer { tabletConnected ->
            if (tabletConnected){
                val dataObject = NTSPimPacket.PimStatusObj()
                val statusObj =  NTSPimPacket(NTSPimPacket.Command.PIM_STATUS, dataObject)
                Log.i("Bluetooth", "status request packet to be sent == $statusObj")
                (activity as MainActivity).sendBluetoothPacket(statusObj)
                BluetoothDataCenter.startUpBTPairSuccessful()
                toWelcomeScreen()
            }
        })

        // Leaving in commented code for driver signed in. We might want to use this value for bluetooth connection logic.
//        upfrontPriceViewModel.isDriverSignedIn().observe(this.viewLifecycleOwner, Observer { signedIn ->
//            if(isDriverSignedIn){
//
//            }
//        })
    }

    private fun getDriverTabletBluetoothAddress(deviceId: String){
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(requireActivity().applicationContext)
        }

        mAWSAppSyncClient?.query(GetPimSettingsQuery.builder().deviceId(deviceId).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            ?.enqueue(awsBluetoothAddressCallback)
    }
    private var awsBluetoothAddressCallback = object: GraphQLCall.Callback<GetPimSettingsQuery.Data>() {
        override fun onResponse(response: Response<GetPimSettingsQuery.Data>) {
            Log.i("Bluetooth", "Bluetooth query response == ${response.data()}")

            val bluetoothAddress = response.data()?.pimSettings?.vehBtAddr()

            if(bluetoothAddress != null){
                BluetoothDataCenter.updateDriverTabletBTDevice(bluetoothAddress)
            }
        }

        override fun onFailure(e: ApolloException) {

        }
    }

    private fun checkIfDriverIsSignedIn(vehicleId:String) {
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(requireActivity().applicationContext)
        }
        mAWSAppSyncClient?.query(GetStatusQuery.builder().vehicleId(vehicleId).build())
            ?.responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            ?.enqueue(driverSignedInQuery)
    }
    private var driverSignedInQuery = object: GraphQLCall.Callback<GetStatusQuery.Data>(){
        override fun onResponse(response: Response<GetStatusQuery.Data>) {
            response.data()?.status?.signinStatusTimeStamp()
           val driverId = response.data()?.status?.driverId()
            if(driverId != 0){
                upfrontPriceViewModel.driverSignedIn()
               LoggerHelper.writeToLog("Driver signed in", LogEnums.TRIP_STATUS.tag)
            }
            if(driverId == 0) {
                upfrontPriceViewModel.driverSignedOut()
                LoggerHelper.writeToLog("Driver not signed in", LogEnums.TRIP_STATUS.tag)
            }
        }

        override fun onFailure(e: ApolloException) {

        }
    }

    private fun startBluetoothConnectionTimer(){
        val timerLength = 600000.toLong()
         viewGroup = activity?.findViewById<View>(R.id.bluetooth_pairing_layout) as ViewGroup
        if(noBTConnectionTimer == null){
            noBTConnectionTimer = object: CountDownTimer(timerLength, 60000){
                override fun onFinish() {
                    try {
                        noBTView =  View.inflate(activity, R.layout.no_bluetooth_connection, viewGroup)
                    }catch (e: IllegalStateException){
                        LoggerHelper.writeToLog("Issue with the no_bluetooth_connection inflating. Error: $e", LogEnums.BLUETOOTH.tag)
                    }

                }

                override fun onTick(p0: Long) {
                }
            }.start()
        }
    }

   //Navigation
   private fun toWelcomeScreen() = launch(Dispatchers.Main.immediate){
       if (navController?.currentDestination?.id == currentFragmentId) {
           navController?.navigate(R.id.action_blueToothPairingFragment_to_welcome_fragment)
       }
       if(noBTView != null){
           val bluetoothLayout = view?.findViewById<View>(R.id.no_bluetooth_connection_layout)
           viewGroup?.removeView(bluetoothLayout)
           viewGroup?.removeView(noBTView)
       }
       noBTConnectionTimer?.cancel()
   }

    override fun onDestroy() {
        BluetoothDataCenter.isConnectedToDriverTablet().removeObservers(this)
        noBTConnectionTimer?.cancel()
        super.onDestroy()
    }
}
