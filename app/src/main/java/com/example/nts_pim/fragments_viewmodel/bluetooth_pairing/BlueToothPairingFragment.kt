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
import com.example.nts_pim.data.repository.VehicleTripArrayHolder
import com.example.nts_pim.fragments_viewmodel.base.ClientFactory
import com.example.nts_pim.fragments_viewmodel.base.ScopedFragment
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupViewModel
import com.example.nts_pim.utilities.bluetooth_helper.BluetoothDataCenter
import com.example.nts_pim.utilities.bluetooth_helper.NTSPimPacket
import com.example.nts_pim.utilities.device_id_check.DeviceIdCheck
import com.example.nts_pim.utilities.enums.PIMStatusEnum
import com.example.nts_pim.utilities.logging_service.LoggerHelper
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
        isBluetoothOn = BluetoothDataCenter.isBluetoothOn().value
        vehicleId = viewModel.getVehicleID()
        deviceId = DeviceIdCheck.getDeviceId() ?: ""
        VehicleTripArrayHolder.updateInternalPIMStatus(PIMStatusEnum.PIM_PAIRING.status)
        if(!isBluetoothOn!!){
            Log.i("${logTag}", "Bluetooth pairing is off. Going to welcome screen")
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is off. Going to welcome screen")
            toWelcomeScreen()
        } else {
            Log.i("${logTag}", "Bluetooth pairing is on. Starting pairing process")
            LoggerHelper.writeToLog("${logTag}, Bluetooth pairing is on. Starting pairing process")
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
            if(response.hasErrors()){
                Log.i("Bluetooth", "error from driver signed in query. ${response.errors()}")
            }
            response.data()?.status?.signinStatusTimeStamp()
           val driverId = response.data()?.status?.driverId()
            if(driverId != 0){
                isDriverSignedIn = true
                Log.i("Bluetooth", "Driver is signed in")
            }
            if(driverId == 0) {
                isDriverSignedIn = false
                Log.i("Bluetooth", "Driver is not signed in. Driver id == $driverId")
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
                    Log.i("Bluetooth", "Displaying no_bluetooth_connection.xml")
                    try {
                        noBTView =  View.inflate(activity, R.layout.no_bluetooth_connection, viewGroup)
                    }catch (e: IllegalStateException){
                        Log.i("Bluetooth", "Issue with the no_bluetooth_connection inflating. Error: $e")
                    }

                }

                override fun onTick(p0: Long) {
                    Log.i("Bluetooth", "Show no connection timer has been running for ${(timerLength - p0)/60000} mins.")
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
        Log.i(logTag, "Discovery timer canceled")
        noBTConnectionTimer?.cancel()
        super.onDestroy()
    }
}
